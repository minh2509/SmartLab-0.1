package com.smartlab.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.Role;
import com.smartlab.entity.User;
import com.smartlab.entity.UserRole;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;
import com.smartlab.repository.RolePermissionRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.UserRoleRepository;

@Service
@Profile("!nodb")
public class DatabaseUserDetailsService implements UserDetailsService {

	private static final String GENERIC_AUTHENTICATION_FAILURE = "Authentication failed.";

	private final UserRepository userRepository;
	private final UserRoleRepository userRoleRepository;
	private final RolePermissionRepository rolePermissionRepository;

	public DatabaseUserDetailsService(
			UserRepository userRepository,
			UserRoleRepository userRoleRepository,
			RolePermissionRepository rolePermissionRepository) {
		this.userRepository = userRepository;
		this.userRoleRepository = userRoleRepository;
		this.rolePermissionRepository = rolePermissionRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		String normalizedEmail = normalizeEmail(username);
		List<User> candidates = userRepository.findByEmail(normalizedEmail);
		if (candidates.size() != 1) {
			throw new UsernameNotFoundException(GENERIC_AUTHENTICATION_FAILURE);
		}

		User user = candidates.get(0);
		if (user.getAccountStatus() != UserAccountStatus.ACTIVE) {
			throw new DisabledException(GENERIC_AUTHENTICATION_FAILURE);
		}

		List<UserRole> activeUserRoles = userRoleRepository.findByUserAndStatus(user, UserRoleStatus.ACTIVE);
		Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
		activeUserRoles.forEach(userRole -> {
			if (userRole.getRole() != null && userRole.getRole().getCode() != null) {
				authorities.add(new SimpleGrantedAuthority(SecurityAuthorities.role(userRole.getRole().getCode())));
			}
		});
		List<Role> activeRoles = activeUserRoles.stream()
				.map(UserRole::getRole)
				.filter(role -> role != null)
				.toList();
		if (!activeRoles.isEmpty()) {
			rolePermissionRepository.findByRoleIn(activeRoles).forEach(rolePermission -> {
				if (rolePermission.getPermission() != null && rolePermission.getPermission().getCode() != null) {
					authorities.add(new SimpleGrantedAuthority(
							SecurityAuthorities.permission(rolePermission.getPermission().getCode())));
				}
			});
		}

		return new SmartLabUserPrincipal(
				user.getId(),
				user.getLab().getId(),
				user.getEmail(),
				user.getFullName(),
				user.getPasswordHash(),
				user.getAccountStatus(),
				authorities);
	}

	private static String normalizeEmail(String email) {
		if (email == null || email.trim().isBlank()) {
			throw new UsernameNotFoundException(GENERIC_AUTHENTICATION_FAILURE);
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}
}

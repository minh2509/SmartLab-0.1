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

import com.smartlab.entity.User;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.UserRoleRepository;

@Service
@Profile("!nodb")
public class DatabaseUserDetailsService implements UserDetailsService {

	private static final String GENERIC_AUTHENTICATION_FAILURE = "Authentication failed.";

	private final UserRepository userRepository;
	private final UserRoleRepository userRoleRepository;

	public DatabaseUserDetailsService(UserRepository userRepository, UserRoleRepository userRoleRepository) {
		this.userRepository = userRepository;
		this.userRoleRepository = userRoleRepository;
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

		Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
		userRoleRepository.findByUserAndStatus(user, UserRoleStatus.ACTIVE).forEach(userRole -> {
			if (userRole.getRole() != null && userRole.getRole().getCode() != null) {
				authorities.add(new SimpleGrantedAuthority("ROLE_" + userRole.getRole().getCode()));
			}
		});

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

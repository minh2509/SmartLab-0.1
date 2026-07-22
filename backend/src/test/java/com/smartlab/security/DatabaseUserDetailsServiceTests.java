package com.smartlab.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.smartlab.entity.Lab;
import com.smartlab.entity.Role;
import com.smartlab.entity.User;
import com.smartlab.entity.UserRole;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.UserRoleRepository;

class DatabaseUserDetailsServiceTests {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
	private final DatabaseUserDetailsService service = new DatabaseUserDetailsService(userRepository, userRoleRepository);

	@Test
	void loadUserNormalizesEmailAndMapsOnlyActiveRolesToPrefixedAuthorities() {
		User user = user(UserAccountStatus.ACTIVE, "minh@example.edu");
		Role admin = role("ADMIN");
		Role duplicateAdmin = role("ADMIN");
		Role inactiveMember = role("MEMBER");
		when(userRepository.findByEmail("minh@example.edu")).thenReturn(List.of(user));
		when(userRoleRepository.findByUserAndStatus(user, UserRoleStatus.ACTIVE))
				.thenReturn(List.of(userRole(user, admin), userRole(user, duplicateAdmin)));
		when(userRoleRepository.findByUserAndStatus(user, UserRoleStatus.INACTIVE))
				.thenReturn(List.of(userRole(user, inactiveMember)));

		SmartLabUserPrincipal principal =
				(SmartLabUserPrincipal) service.loadUserByUsername("  Minh@Example.EDU ");

		assertEquals(user.getId(), principal.userId());
		assertEquals(user.getLab().getId(), principal.labId());
		assertEquals("minh@example.edu", principal.email());
		assertEquals("Full Name", principal.fullName());
		assertEquals(Set.of("ROLE_ADMIN"), authorityNames(principal));
		assertFalse(authorityNames(principal).contains("ROLE_MEMBER"));
		assertFalse(principal.toString().contains("encoded-password-hash"));
		assertThrows(UnsupportedOperationException.class, () -> principal.getAuthorities().clear());
		verify(userRepository, never()).save(any(User.class));
		verify(userRoleRepository, never()).save(any(UserRole.class));
	}

	@Test
	void missingAndAmbiguousEmailCandidatesFailGenerically() {
		when(userRepository.findByEmail("missing@example.edu")).thenReturn(List.of());
		when(userRepository.findByEmail("shared@example.edu"))
				.thenReturn(List.of(user(UserAccountStatus.ACTIVE, "shared@example.edu"),
						user(UserAccountStatus.ACTIVE, "shared@example.edu")));

		UsernameNotFoundException missing = assertThrows(
				UsernameNotFoundException.class,
				() -> service.loadUserByUsername("missing@example.edu"));
		UsernameNotFoundException ambiguous = assertThrows(
				UsernameNotFoundException.class,
				() -> service.loadUserByUsername("shared@example.edu"));

		assertEquals("Authentication failed.", missing.getMessage());
		assertEquals("Authentication failed.", ambiguous.getMessage());
	}

	@Test
	void nonActiveAccountsCannotAuthenticateForEveryNonActiveStatus() {
		for (UserAccountStatus status : UserAccountStatus.values()) {
			User user = user(status, status.name().toLowerCase() + "@example.edu");
			when(userRepository.findByEmail(user.getEmail())).thenReturn(List.of(user));
			if (status == UserAccountStatus.ACTIVE) {
				when(userRoleRepository.findByUserAndStatus(user, UserRoleStatus.ACTIVE)).thenReturn(List.of());
				assertTrue(service.loadUserByUsername(user.getEmail()).isEnabled());
			} else {
				DisabledException exception = assertThrows(
						DisabledException.class,
						() -> service.loadUserByUsername(user.getEmail()));
				assertEquals("Authentication failed.", exception.getMessage());
			}
		}
	}

	private static Set<String> authorityNames(SmartLabUserPrincipal principal) {
		return principal.getAuthorities()
				.stream()
				.map(GrantedAuthority::getAuthority)
				.collect(java.util.stream.Collectors.toUnmodifiableSet());
	}

	private static User user(UserAccountStatus status, String email) {
		Lab lab = new Lab();
		lab.setId(UUID.randomUUID());
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setLab(lab);
		user.setUsername(email.substring(0, email.indexOf('@')));
		user.setEmail(email);
		user.setPasswordHash("encoded-password-hash");
		user.setFullName("Full Name");
		user.setAccountStatus(status);
		return user;
	}

	private static Role role(String code) {
		Role role = new Role();
		role.setId(UUID.randomUUID());
		role.setCode(code);
		role.setName(code);
		return role;
	}

	private static UserRole userRole(User user, Role role) {
		UserRole userRole = new UserRole();
		userRole.setId(UUID.randomUUID());
		userRole.setUser(user);
		userRole.setRole(role);
		userRole.setStatus(UserRoleStatus.ACTIVE);
		return userRole;
	}
}

package com.smartlab.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.smartlab.enums.UserAccountStatus;

class SmartLabUserPrincipalTests {

	@Test
	void exposesSecurityIdentityWithoutMutableAuthoritiesOrCredentialLeakage() {
		UUID userId = UUID.randomUUID();
		SmartLabUserPrincipal principal = new SmartLabUserPrincipal(
				userId,
				UUID.randomUUID(),
				"minh@example.edu",
				"Minh Hoang",
				"$2a$10$encoded",
				UserAccountStatus.ACTIVE,
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

		assertEquals(userId, principal.userId());
		assertEquals("minh@example.edu", principal.getUsername());
		assertEquals("$2a$10$encoded", principal.getPassword());
		assertTrue(principal.isEnabled());
		assertTrue(principal.isAccountNonExpired());
		assertTrue(principal.isAccountNonLocked());
		assertTrue(principal.isCredentialsNonExpired());
		assertThrows(UnsupportedOperationException.class, () -> principal.getAuthorities().clear());
		assertFalse(principal.toString().contains("$2a$10$encoded"));
	}

	@Test
	void mapsEveryAccountStatusToExplicitUserDetailsFlags() {
		assertTrue(principal(UserAccountStatus.ACTIVE).isEnabled());
		assertTrue(principal(UserAccountStatus.ACTIVE).isAccountNonLocked());
		assertFalse(principal(UserAccountStatus.LOCKED).isAccountNonLocked());
		assertFalse(principal(UserAccountStatus.PENDING).isEnabled());
		assertFalse(principal(UserAccountStatus.DELETED).isEnabled());
		assertFalse(principal(UserAccountStatus.DELETED).isAccountNonExpired());
	}

	@Test
	void equalityUsesStableUserIdentity() {
		UUID userId = UUID.randomUUID();
		SmartLabUserPrincipal first = principal(userId);
		SmartLabUserPrincipal second = principal(userId);
		SmartLabUserPrincipal other = principal(UUID.randomUUID());

		assertEquals(first, second);
		assertEquals(first.hashCode(), second.hashCode());
		assertNotEquals(first, other);
	}

	private static SmartLabUserPrincipal principal(UserAccountStatus status) {
		return new SmartLabUserPrincipal(
				UUID.randomUUID(),
				UUID.randomUUID(),
				status.name().toLowerCase() + "@example.edu",
				"Full Name",
				"$2a$10$encoded",
				status,
				List.of());
	}

	private static SmartLabUserPrincipal principal(UUID userId) {
		return new SmartLabUserPrincipal(
				userId,
				UUID.randomUUID(),
				"minh@example.edu",
				"Full Name",
				"$2a$10$encoded",
				UserAccountStatus.ACTIVE,
				List.of());
	}
}

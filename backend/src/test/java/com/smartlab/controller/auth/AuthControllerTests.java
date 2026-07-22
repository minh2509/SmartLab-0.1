package com.smartlab.controller.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.smartlab.enums.UserAccountStatus;
import com.smartlab.security.SecurityAuthorities;
import com.smartlab.security.SmartLabUserPrincipal;

class AuthControllerTests {

	@Test
	void currentUserReturnsPrincipalIdentityAndRoleCodesOnly() {
		UUID userId = UUID.randomUUID();
		UUID labId = UUID.randomUUID();
		SmartLabUserPrincipal principal = new SmartLabUserPrincipal(
				userId,
				labId,
				"admin@smart.lab",
				"SmartLab Admin",
				"encoded-password",
				UserAccountStatus.ACTIVE,
				List.of(
						new SimpleGrantedAuthority(SecurityAuthorities.role("SUPER_ADMIN")),
						new SimpleGrantedAuthority(SecurityAuthorities.role("ADMIN")),
						new SimpleGrantedAuthority(SecurityAuthorities.permission(SecurityAuthorities.ADMIN_ACCESS))));

		var response = new AuthController().currentUser(new TestingAuthenticationToken(principal, null));

		assertEquals(userId, response.id());
		assertEquals(labId, response.labId());
		assertEquals("admin@smart.lab", response.email());
		assertEquals(UserAccountStatus.ACTIVE, response.accountStatus());
		assertTrue(response.roleCodes().contains("SUPER_ADMIN"));
		assertTrue(response.roleCodes().contains("ADMIN"));
		assertEquals(2, response.roleCodes().size());
	}
}

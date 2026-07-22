package com.smartlab.controller.auth;

import java.util.Comparator;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.response.auth.AuthCurrentUserResponse;
import com.smartlab.security.SecurityAuthorities;
import com.smartlab.security.SmartLabUserPrincipal;

@RestController
@RequestMapping("/api/auth")
@Profile("!nodb")
public class AuthController {

	@GetMapping("/me")
	public AuthCurrentUserResponse currentUser(Authentication authentication) {
		SmartLabUserPrincipal principal = (SmartLabUserPrincipal) authentication.getPrincipal();
		List<String> roleCodes = principal.getAuthorities()
				.stream()
				.map(GrantedAuthority::getAuthority)
				.filter(authority -> authority.startsWith(SecurityAuthorities.ROLE_PREFIX))
				.map(authority -> authority.substring(SecurityAuthorities.ROLE_PREFIX.length()))
				.sorted(Comparator.naturalOrder())
				.toList();
		return new AuthCurrentUserResponse(
				principal.userId(),
				principal.labId(),
				principal.email(),
				principal.fullName(),
				principal.accountStatus(),
				roleCodes);
	}
}

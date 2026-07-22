package com.smartlab.security;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@Profile("!nodb")
public class AuthenticatedActorResolver {

	public UUID requireActorUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AuthenticationCredentialsNotFoundException("Authentication is required.");
		}
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof Jwt jwt)) {
			throw new BadCredentialsException("Authentication is required.");
		}
		try {
			return UUID.fromString(jwt.getSubject());
		} catch (IllegalArgumentException exception) {
			throw new BadCredentialsException("Authentication is required.");
		}
	}
}

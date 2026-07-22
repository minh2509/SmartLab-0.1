package com.smartlab.security;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import com.smartlab.exception.InvalidAdminServiceInputException;

public final class AdminJwtClaims {

	private AdminJwtClaims() {
	}

	public static UUID userId(Jwt jwt) {
		if (jwt == null) {
			throw new InvalidAdminServiceInputException("Authenticated admin principal is required.");
		}
		return parseUuid(jwt.getSubject(), "Authenticated admin user ID");
	}

	public static UUID labId(Jwt jwt) {
		if (jwt == null) {
			throw new InvalidAdminServiceInputException("Authenticated admin principal is required.");
		}
		return parseUuid(jwt.getClaimAsString("lab_id"), "Authenticated admin lab ID");
	}

	private static UUID parseUuid(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new InvalidAdminServiceInputException(fieldName + " is required.");
		}
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException exception) {
			throw new InvalidAdminServiceInputException(fieldName + " is invalid.");
		}
	}
}

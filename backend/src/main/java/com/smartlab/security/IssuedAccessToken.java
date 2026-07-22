package com.smartlab.security;

import java.time.OffsetDateTime;

public record IssuedAccessToken(
		String tokenType,
		String tokenValue,
		long expiresIn,
		OffsetDateTime issuedAt,
		OffsetDateTime expiresAt) {
}

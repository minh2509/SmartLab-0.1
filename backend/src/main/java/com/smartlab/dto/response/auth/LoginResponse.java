package com.smartlab.dto.response.auth;

import java.time.OffsetDateTime;

public record LoginResponse(
		String tokenType,
		String accessToken,
		long expiresIn,
		OffsetDateTime issuedAt,
		OffsetDateTime expiresAt,
		AuthenticatedUserResponse user) {
}

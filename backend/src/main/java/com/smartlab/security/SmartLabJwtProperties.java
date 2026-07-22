package com.smartlab.security;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartlab.jwt")
public record SmartLabJwtProperties(
		String secretBase64,
		long accessTtlSeconds) {

	public static final String ISSUER = "https://smartlab.local";
	public static final long MAX_ACCESS_TTL_SECONDS = 86_400;

	public SecretKey secretKey() {
		if (secretBase64 == null || secretBase64.isBlank()) {
			throw new IllegalStateException("JWT secret must be provided.");
		}
		byte[] decoded;
		try {
			decoded = Base64.getDecoder().decode(secretBase64);
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("JWT secret must be Base64-encoded.", exception);
		}
		if (decoded.length < 32) {
			throw new IllegalStateException("JWT secret must decode to at least 32 bytes.");
		}
		return new SecretKeySpec(decoded, "HmacSHA256");
	}

	public long validatedAccessTtlSeconds() {
		if (accessTtlSeconds <= 0) {
			throw new IllegalStateException("JWT access token TTL must be positive.");
		}
		if (accessTtlSeconds > MAX_ACCESS_TTL_SECONDS) {
			throw new IllegalStateException("JWT access token TTL must not exceed 86400 seconds.");
		}
		return accessTtlSeconds;
	}
}

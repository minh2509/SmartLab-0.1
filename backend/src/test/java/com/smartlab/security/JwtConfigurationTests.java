package com.smartlab.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import com.smartlab.config.JwtSecurityConfig;

class JwtConfigurationTests {

	private static final String VALID_SECRET = Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes());

	private final JwtSecurityConfig jwtSecurityConfig = new JwtSecurityConfig();

	@Test
	void validBase64SecretAndPositiveTtlCreateJwtComponents() {
		SmartLabJwtProperties properties = new SmartLabJwtProperties(VALID_SECRET, 900);
		SecretKey secretKey = jwtSecurityConfig.jwtSecretKey(properties);

		JwtEncoder encoder = jwtSecurityConfig.jwtEncoder(secretKey);
		JwtDecoder decoder = jwtSecurityConfig.jwtDecoder(secretKey);

		assertNotNull(encoder);
		assertNotNull(decoder);
		assertEquals(900, properties.validatedAccessTtlSeconds());
	}

	@Test
	void invalidJwtConfigurationIsRejected() {
		assertInvalidConfiguration("smartlab.jwt.access-ttl-seconds=900");
		assertInvalidConfiguration("smartlab.jwt.secret-base64=not-base64", "smartlab.jwt.access-ttl-seconds=900");
		assertInvalidConfiguration(
				"smartlab.jwt.secret-base64=" + Base64.getEncoder().encodeToString("too-short".getBytes()),
				"smartlab.jwt.access-ttl-seconds=900");
		assertInvalidConfiguration("smartlab.jwt.secret-base64=" + VALID_SECRET, "smartlab.jwt.access-ttl-seconds=0");
		assertInvalidConfiguration("smartlab.jwt.secret-base64=" + VALID_SECRET, "smartlab.jwt.access-ttl-seconds=-1");
		assertInvalidConfiguration("smartlab.jwt.secret-base64=" + VALID_SECRET, "smartlab.jwt.access-ttl-seconds=86401");
	}

	@Test
	void decodedSecretUsesHmacSha256CompatibleKeyMaterial() {
		SmartLabJwtProperties properties = new SmartLabJwtProperties(VALID_SECRET, 900);

		SecretKey secretKey = properties.secretKey();

		assertEquals("HmacSHA256", secretKey.getAlgorithm());
		assertEquals(32, secretKey.getEncoded().length);
	}

	@Test
	void missingJwtSecretIsRejectedForRealJwtConfiguration() {
		assertThrows(Exception.class, () -> jwtSecurityConfig.jwtSecretKey(new SmartLabJwtProperties(null, 900)));
	}

	private void assertInvalidConfiguration(String... properties) {
		String secret = null;
		long ttl = 0;
		for (String property : properties) {
			if (property.startsWith("smartlab.jwt.secret-base64=")) {
				secret = property.substring("smartlab.jwt.secret-base64=".length());
			}
			if (property.startsWith("smartlab.jwt.access-ttl-seconds=")) {
				ttl = Long.parseLong(property.substring("smartlab.jwt.access-ttl-seconds=".length()));
			}
		}
		SmartLabJwtProperties jwtProperties = new SmartLabJwtProperties(secret, ttl);
		assertThrows(Exception.class, () -> jwtSecurityConfig.jwtSecretKey(jwtProperties));
	}
}

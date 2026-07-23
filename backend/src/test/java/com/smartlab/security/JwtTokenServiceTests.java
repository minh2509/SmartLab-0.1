package com.smartlab.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.smartlab.config.JwtSecurityConfig;
import com.smartlab.enums.UserAccountStatus;

class JwtTokenServiceTests {

	private static final Instant NOW = Instant.now().plusSeconds(3_600).truncatedTo(ChronoUnit.SECONDS);
	private static final String SECRET = base64("01234567890123456789012345678901");
	private static final String WRONG_SECRET = base64("abcdefghijklmnopqrstuvwxyzabcdef");

	@Test
	void createsSignedAccessTokenWithStableClaimsAndSafePayload() {
		JwtTokenService tokenService = tokenService(SECRET, 900, Clock.fixed(NOW, ZoneOffset.UTC));
		SmartLabUserPrincipal principal = principalWithAuthorities(
				"ROLE_ADMIN",
				"ROLE_MEMBER",
				"ROLE_ADMIN",
				SecurityAuthorities.permission(SecurityAuthorities.ADMIN_ACCESS));

		IssuedAccessToken issued = tokenService.issueAccessToken(principal);
		Jwt decoded = decoder(SECRET).decode(issued.tokenValue());

		assertEquals("Bearer", issued.tokenType());
		assertEquals(900, issued.expiresIn());
		assertEquals(NOW, issued.issuedAt().toInstant());
		assertEquals(NOW.plusSeconds(900), issued.expiresAt().toInstant());
		assertEquals(SmartLabJwtProperties.ISSUER, decoded.getIssuer().toString());
		assertEquals(principal.userId().toString(), decoded.getSubject());
		assertEquals(NOW, decoded.getIssuedAt());
		assertEquals(NOW.plusSeconds(900), decoded.getExpiresAt());
		assertEquals(principal.labId().toString(), decoded.getClaimAsString("lab_id"));
		assertEquals("admin@example.test", decoded.getClaimAsString("email"));
		assertEquals("Admin User", decoded.getClaimAsString("full_name"));
		assertEquals("ACTIVE", decoded.getClaimAsString("account_status"));
		assertEquals(List.of("ADMIN", "MEMBER"), decoded.getClaimAsStringList("roles"));
		assertEquals(List.of(SecurityAuthorities.ADMIN_ACCESS), decoded.getClaimAsStringList("permissions"));
		assertTrue(decoded.hasClaim("jti"));
		assertFalse(decoded.getTokenValue().contains("bcrypt-hash"));
		assertFalse(decoded.getClaims().toString().contains("bcrypt-hash"));
		assertFalse(decoded.getClaims().toString().contains("password"));
	}

	@Test
	void createsUniqueTokenIds() {
		JwtTokenService tokenService = tokenService(SECRET, 900, Clock.fixed(NOW, ZoneOffset.UTC));
		SmartLabUserPrincipal principal = principal("ADMIN");

		Jwt first = decoder(SECRET).decode(tokenService.issueAccessToken(principal).tokenValue());
		Jwt second = decoder(SECRET).decode(tokenService.issueAccessToken(principal).tokenValue());

		assertNotEquals(first.getId(), second.getId());
	}

	@Test
	void validatesSignatureExpirationIssuerAndRoleClaims() {
		JwtTokenService tokenService = tokenService(SECRET, 900, Clock.fixed(NOW, ZoneOffset.UTC));
		String token = tokenService.issueAccessToken(principal("ADMIN")).tokenValue();

		assertDoesNotThrow(() -> decoder(SECRET).decode(token));
		assertThrows(JwtException.class, () -> decoder(SECRET).decode(token + "tampered"));
		assertThrows(JwtException.class, () -> decoder(WRONG_SECRET).decode(token));
		assertThrows(JwtException.class, () -> decoder(SECRET).decode(tokenWithIssuer("wrong-issuer")));
		assertThrows(JwtException.class, () -> decoder(SECRET).decode(tokenWithRoles(List.of("ADMIN", "OWNER"))));
		assertThrows(JwtException.class, () -> decoder(SECRET).decode(tokenWithRoles("ADMIN")));
		assertThrows(JwtException.class, () -> decoder(SECRET).decode(expiredToken()));
	}

	private static JwtTokenService tokenService(String secret, long ttlSeconds, Clock clock) {
		SmartLabJwtProperties properties = new SmartLabJwtProperties(secret, ttlSeconds);
		JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(properties.secretKey()));
		return new JwtTokenService(encoder, properties, clock);
	}

	private static JwtDecoder decoder(String secret) {
		SmartLabJwtProperties properties = new SmartLabJwtProperties(secret, 900);
		return new JwtSecurityConfig().jwtDecoder(properties.secretKey());
	}

	private static String tokenWithIssuer(String issuer) {
		return encode(builder().issuer(issuer).claim("roles", List.of("ADMIN")).build());
	}

	private static String tokenWithRoles(Object roles) {
		return encode(builder().claim("roles", roles).build());
	}

	private static String expiredToken() {
		return encode(builder()
				.issuedAt(Instant.now().minusSeconds(2_000))
				.expiresAt(Instant.now().minusSeconds(1_000))
				.claim("roles", List.of("ADMIN"))
				.build());
	}

	private static JwtClaimsSet.Builder builder() {
		return JwtClaimsSet.builder()
				.issuer(SmartLabJwtProperties.ISSUER)
				.subject(UUID.randomUUID().toString())
				.issuedAt(NOW)
				.expiresAt(NOW.plusSeconds(900))
				.id(UUID.randomUUID().toString())
				.claim("lab_id", UUID.randomUUID().toString())
				.claim("email", "admin@example.test")
				.claim("full_name", "Admin User")
				.claim("account_status", "ACTIVE");
	}

	private static String encode(JwtClaimsSet claims) {
		SmartLabJwtProperties properties = new SmartLabJwtProperties(SECRET, 900);
		NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(properties.secretKey()));
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}

	private static SmartLabUserPrincipal principal(String... roles) {
		return principalWithAuthorities(
				Arrays.stream(roles).map(role -> "ROLE_" + role).toArray(String[]::new));
	}

	private static SmartLabUserPrincipal principalWithAuthorities(String... authorities) {
		return new SmartLabUserPrincipal(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"admin@example.test",
				"Admin User",
				"bcrypt-hash",
				UserAccountStatus.ACTIVE,
				Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
	}

	private static String base64(String value) {
		return Base64.getEncoder().encodeToString(value.getBytes());
	}
}

package com.smartlab.security;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@Profile("!nodb")
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;
	private final SmartLabJwtProperties properties;
	private final Clock clock;

	public JwtTokenService(JwtEncoder jwtEncoder, SmartLabJwtProperties properties, Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.clock = clock;
	}

	public IssuedAccessToken issueAccessToken(SmartLabUserPrincipal principal) {
		long ttlSeconds = properties.validatedAccessTtlSeconds();
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(SmartLabJwtProperties.ISSUER)
				.subject(principal.userId().toString())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.id(UUID.randomUUID().toString())
				.claim("lab_id", principal.labId().toString())
				.claim("email", principal.email())
				.claim("full_name", principal.fullName())
				.claim("account_status", principal.accountStatus().name())
				.claim("roles", roleCodes(principal))
				.build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(),
				claims)).getTokenValue();
		return new IssuedAccessToken(
				"Bearer",
				token,
				ttlSeconds,
				OffsetDateTime.ofInstant(issuedAt, ZoneOffset.UTC),
				OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
	}

	private static List<String> roleCodes(SmartLabUserPrincipal principal) {
		return principal.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(authority -> authority.startsWith("ROLE_"))
				.map(authority -> authority.substring("ROLE_".length()))
				.distinct()
				.sorted()
				.toList();
	}
}

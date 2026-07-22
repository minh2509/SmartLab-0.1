package com.smartlab.config;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.security.SmartLabJwtProperties;

@Configuration
@Profile("!nodb")
@EnableConfigurationProperties(SmartLabJwtProperties.class)
public class JwtSecurityConfig {

	private static final String ROLE_PREFIX = "ROLE_";
	private static final Set<String> SUPPORTED_ROLE_CODES =
			Set.of("SUPER_ADMIN", "ADMIN", "LEADER", "MEMBER");

	@Bean
	public SecretKey jwtSecretKey(SmartLabJwtProperties properties) {
		properties.validatedAccessTtlSeconds();
		return properties.secretKey();
	}

	@Bean
	public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
	}

	@Bean
	public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder
				.withSecretKey(jwtSecretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		decoder.setJwtValidator(jwtValidator());
		return decoder;
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new SmartLabJwtGrantedAuthoritiesConverter());
		return converter;
	}

	@Bean
	public Clock jwtClock() {
		return Clock.systemUTC();
	}

	private static OAuth2TokenValidator<Jwt> jwtValidator() {
		return new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(SmartLabJwtProperties.ISSUER),
				new JwtClaimValidator<>("sub", JwtSecurityConfig::isUuidString),
				new JwtClaimValidator<>("lab_id", JwtSecurityConfig::isUuidString),
				new JwtClaimValidator<>("email", value -> value instanceof String email && !email.isBlank()),
				new JwtClaimValidator<>("account_status", JwtSecurityConfig::isSupportedAccountStatus),
				new JwtClaimValidator<>("roles", JwtSecurityConfig::isSupportedRoleList));
	}

	private static boolean isUuidString(Object value) {
		if (!(value instanceof String text) || text.isBlank()) {
			return false;
		}
		try {
			UUID.fromString(text);
			return true;
		} catch (IllegalArgumentException exception) {
			return false;
		}
	}

	private static boolean isSupportedAccountStatus(Object value) {
		if (!(value instanceof String status)) {
			return false;
		}
		try {
			return UserAccountStatus.valueOf(status) == UserAccountStatus.ACTIVE;
		} catch (IllegalArgumentException exception) {
			return false;
		}
	}

	private static boolean isSupportedRoleList(Object value) {
		if (!(value instanceof List<?> roles)) {
			return false;
		}
		return roles.stream().allMatch(role -> role instanceof String code && SUPPORTED_ROLE_CODES.contains(code));
	}

	private static final class SmartLabJwtGrantedAuthoritiesConverter
			implements Converter<Jwt, Collection<GrantedAuthority>> {

		@Override
		public Collection<GrantedAuthority> convert(Jwt jwt) {
			List<String> roles = jwt.getClaimAsStringList("roles");
			if (roles == null) {
				return List.of();
			}
			return roles.stream()
					.filter(SUPPORTED_ROLE_CODES::contains)
					.distinct()
					.sorted()
					.map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
					.map(GrantedAuthority.class::cast)
					.toList();
		}
	}
}

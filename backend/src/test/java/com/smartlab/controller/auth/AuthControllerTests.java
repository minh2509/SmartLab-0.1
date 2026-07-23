package com.smartlab.controller.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.smartlab.enums.UserAccountStatus;
import com.smartlab.exception.ApiExceptionHandler;
import com.smartlab.mapper.AuthApiMapper;
import com.smartlab.security.IssuedAccessToken;
import com.smartlab.security.JwtTokenService;
import com.smartlab.security.SmartLabUserPrincipal;

class AuthControllerTests {

	private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
	private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new AuthController(authenticationManager, jwtTokenService, new AuthApiMapper()))
			.setControllerAdvice(new ApiExceptionHandler())
			.setValidator(validator())
			.build();

	@Test
	void loginReturnsBearerTokenAndSafeUserResponse() throws Exception {
		SmartLabUserPrincipal principal = principal("admin@example.test", "ROLE_ADMIN", "ROLE_MEMBER", "ROLE_ADMIN");
		when(authenticationManager.authenticate(any(Authentication.class)))
				.thenReturn(UsernamePasswordAuthenticationToken.authenticated(
						principal,
						principal.getPassword(),
						principal.getAuthorities()));
		when(jwtTokenService.issueAccessToken(principal)).thenReturn(new IssuedAccessToken(
				"Bearer",
				"signed.jwt.value",
				900,
				OffsetDateTime.parse("2026-07-22T22:00:00+07:00"),
				OffsetDateTime.parse("2026-07-22T22:15:00+07:00")));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":" Admin@Example.TEST ","password":"user-supplied-password"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.accessToken").value("signed.jwt.value"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andExpect(jsonPath("$.issuedAt").value("2026-07-22T22:00:00+07:00"))
				.andExpect(jsonPath("$.expiresAt").value("2026-07-22T22:15:00+07:00"))
				.andExpect(jsonPath("$.user.id").value(principal.userId().toString()))
				.andExpect(jsonPath("$.user.labId").value(principal.labId().toString()))
				.andExpect(jsonPath("$.user.email").value("admin@example.test"))
				.andExpect(jsonPath("$.user.accountStatus").value("ACTIVE"))
				.andExpect(jsonPath("$.user.roles[0]").value("ADMIN"))
				.andExpect(jsonPath("$.user.roles[1]").value("MEMBER"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(content().string(not(containsString("user-supplied-password"))))
				.andExpect(content().string(not(containsString("bcrypt-hash"))));

		ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
		verify(authenticationManager).authenticate(captor.capture());
		assertEquals("Admin@Example.TEST", captor.getValue().getPrincipal());
		assertEquals("user-supplied-password", captor.getValue().getCredentials());
	}

	@Test
	void failedLoginReturnsGenericUnauthorizedJson() throws Exception {
		for (org.springframework.security.core.AuthenticationException exception : List.of(
				new BadCredentialsException("wrong password"),
				new UsernameNotFoundException("missing or ambiguous"),
				new DisabledException("inactive account"))) {
			doThrow(exception).when(authenticationManager).authenticate(any(Authentication.class));

			mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"email":"missing@example.test","password":"wrong-password"}
									"""))
					.andExpect(status().isUnauthorized())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andExpect(jsonPath("$.message").value("Invalid email or password."))
					.andExpect(jsonPath("$.path").value("/api/auth/login"))
					.andExpect(content().string(not(containsString("missing@example.test"))))
					.andExpect(content().string(not(containsString("wrong-password"))))
					.andExpect(content().string(not(containsString(exception.getClass().getSimpleName()))));
		}
	}

	@Test
	void invalidLoginRequestReturnsBadRequestWithoutEchoingPassword() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"not-an-email","password":""}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed."))
				.andExpect(content().string(not(containsString("password"))));
	}

	@Test
	void meReturnsAuthenticatedJwtUserInformationWithoutRawTokenOrClaims() throws Exception {
		Jwt jwt = Jwt.withTokenValue("token-value")
				.header("alg", "HS256")
				.subject(UUID.randomUUID().toString())
				.claim("lab_id", UUID.randomUUID().toString())
				.claim("email", "admin@example.test")
				.claim("full_name", "Admin User")
				.claim("account_status", "ACTIVE")
				.claim("roles", List.of("ADMIN", "MEMBER"))
				.build();

		com.smartlab.dto.response.auth.AuthenticatedUserResponse response =
				new AuthController(authenticationManager, jwtTokenService, new AuthApiMapper()).me(jwt);

		org.junit.jupiter.api.Assertions.assertEquals(UUID.fromString(jwt.getSubject()), response.id());
		org.junit.jupiter.api.Assertions.assertEquals(UUID.fromString(jwt.getClaimAsString("lab_id")), response.labId());
		org.junit.jupiter.api.Assertions.assertEquals("admin@example.test", response.email());
		org.junit.jupiter.api.Assertions.assertEquals(UserAccountStatus.ACTIVE, response.accountStatus());
		org.junit.jupiter.api.Assertions.assertEquals(List.of("ADMIN", "MEMBER"), response.roles());
	}

	private static LocalValidatorFactoryBean validator() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		return validator;
	}

	private static SmartLabUserPrincipal principal(String email, String... authorities) {
		return new SmartLabUserPrincipal(
				UUID.randomUUID(),
				UUID.randomUUID(),
				email,
				"Admin User",
				"bcrypt-hash",
				UserAccountStatus.ACTIVE,
				java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
	}
}

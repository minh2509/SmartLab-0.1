package com.smartlab.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.config.JwtSecurityConfig;
import com.smartlab.config.SecurityConfig;
import com.smartlab.controller.auth.AuthController;
import com.smartlab.dto.request.admin.CreateAdminUserRequest;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.exception.ApiExceptionHandler;
import com.smartlab.mapper.AuthApiMapper;

import jakarta.validation.Valid;

@WebMvcTest(controllers = {AdminApiSecurityTests.SecurityProbeController.class, AuthController.class})
@ActiveProfiles("security-test")
@TestPropertySource(properties = {
		"smartlab.jwt.secret-base64=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
		"smartlab.jwt.access-ttl-seconds=900"})
@Import({
		ApiExceptionHandler.class,
		RestAccessDeniedHandler.class,
		RestAuthenticationEntryPoint.class,
		SecurityConfig.class,
		JwtSecurityConfig.class,
		AuthApiMapper.class,
		AdminApiSecurityTests.SecurityProbeController.class,
		AdminApiSecurityTests.TestUsers.class})
class AdminApiSecurityTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	void actuatorHealthIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void unauthenticatedAdminApiRequestReturnsJsonUnauthorized() throws Exception {
		mockMvc.perform(get("/api/admin/probe"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").value("Authentication is required."))
				.andExpect(jsonPath("$.path").value("/api/admin/probe"))
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(content().string(not(containsString("<html"))))
				.andExpect(content().string(not(containsString("JwtException"))))
				.andExpect(content().string(not(containsString("password"))));
	}

	@Test
	void usersWithAdminAccessPermissionCanAccessAdminApi() throws Exception {
		mockMvc.perform(get("/api/admin/probe").with(httpBasic("super@example.edu", "password")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + loginToken("admin@example.test")))
				.andExpect(status().isOk());
	}

	@Test
	void usersWithoutAdminAccessPermissionReceiveJsonForbidden() throws Exception {
		for (String username : List.of(
				"admin-role-only@example.edu",
				"leader@example.edu",
				"member@example.edu",
				"noroles@example.edu")) {
			mockMvc.perform(get("/api/admin/probe").with(httpBasic(username, "password")))
					.andExpect(status().isForbidden())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andExpect(jsonPath("$.status").value(403))
					.andExpect(jsonPath("$.error").value("Forbidden"))
					.andExpect(jsonPath("$.message").value("You do not have permission to access this resource."))
					.andExpect(jsonPath("$.path").value("/api/admin/probe"))
					.andExpect(jsonPath("$.timestamp").exists())
					.andExpect(content().string(not(containsString("<html"))))
					.andExpect(content().string(not(containsString("AccessDeniedException"))));
		}
	}

	@Test
	void bearerTokenFailuresReturnGenericJsonUnauthorized() throws Exception {
		mockMvc.perform(get("/api/admin/probe").header("Authorization", "Bearer malformed-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("Authentication is required."))
				.andExpect(content().string(not(containsString("malformed-token"))));

		String token = loginToken("admin@example.test");
		mockMvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + token + "tampered"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		mockMvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + expiredToken()))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
	}

	@Test
	void httpBasicCredentialsNoLongerAuthenticateAdminApi() throws Exception {
		mockMvc.perform(get("/api/admin/probe").with(httpBasic("admin@example.test", "password")))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
	}

	@Test
	void unrelatedApiEndpointsRequireBearerAuthenticationButNotAdminRole() throws Exception {
		mockMvc.perform(get("/api/probe"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
		mockMvc.perform(get("/api/probe").header("Authorization", "Bearer " + loginToken("member@example.test")))
				.andExpect(status().isOk());
	}

	@Test
	void validTokenAccessesCurrentUserEndpoint() throws Exception {
		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + loginToken("admin@example.test")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("admin@example.test"))
				.andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
				.andExpect(jsonPath("$.roles[0]").value("ADMIN"))
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andExpect(jsonPath("$.claims").doesNotExist());
	}

	@Test
	void csrfIsIgnoredForRestApiMutationsButAuthenticationStillRequired() throws Exception {
		String adminToken = loginToken("admin@example.test");
		mockMvc.perform(post("/api/admin/probe")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("Request validation failed."));
		mockMvc.perform(patch("/api/admin/probe").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/admin/probe").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/admin/probe").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/admin/probe")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
	}

	@Test
	void bearerAuthenticationDoesNotCreateSessionCookie() throws Exception {
		mockMvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + loginToken("admin@example.test")))
				.andExpect(status().isOk())
				.andExpect(cookie().doesNotExist("JSESSIONID"));
	}

	private String loginToken(String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"password"}
								""".formatted(email)))
				.andExpect(status().isOk())
				.andReturn();
		String content = result.getResponse().getContentAsString();
		String marker = "\"accessToken\":\"";
		int start = content.indexOf(marker) + marker.length();
		int end = content.indexOf('"', start);
		return content.substring(start, end);
	}

	private String expiredToken() {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(SmartLabJwtProperties.ISSUER)
				.subject(UUID.randomUUID().toString())
				.issuedAt(Instant.parse("2026-07-22T14:00:00Z"))
				.expiresAt(Instant.parse("2026-07-22T14:15:00Z"))
				.id(UUID.randomUUID().toString())
				.claim("lab_id", UUID.randomUUID().toString())
				.claim("email", "admin@example.test")
				.claim("full_name", "Admin User")
				.claim("account_status", "ACTIVE")
				.claim("roles", java.util.List.of("ADMIN"))
				.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}

	@RestController
	public static class SecurityProbeController {

		@GetMapping("/actuator/health")
		Map<String, String> health() {
			return Map.of("status", "UP");
		}

		@GetMapping("/api/admin/probe")
		Map<String, String> admin() {
			return Map.of("ok", "admin");
		}

		@GetMapping("/api/admin/posts")
		Map<String, String> adminPosts() {
			return Map.of("ok", "admin-posts");
		}

		@GetMapping("/api/admin/posts/pending")
		Map<String, String> pendingAdminPosts() {
			return Map.of("ok", "admin-pending-posts");
		}

		@PostMapping("/api/admin/probe")
		Map<String, String> create(@Valid @RequestBody CreateAdminUserRequest request) {
			return Map.of("ok", "created");
		}

		@PatchMapping("/api/admin/probe")
		Map<String, String> patch() {
			return Map.of("ok", "patched");
		}

		@PutMapping("/api/admin/probe")
		Map<String, String> put() {
			return Map.of("ok", "put");
		}

		@DeleteMapping("/api/admin/probe")
		org.springframework.http.ResponseEntity<Void> delete() {
			return org.springframework.http.ResponseEntity.noContent().build();
		}

		@GetMapping("/api/probe")
		Map<String, String> api() {
			return Map.of("ok", "api");
		}
	}

	@TestConfiguration
	static class TestUsers {

		@Bean
		JwtTokenService jwtTokenService(JwtEncoder jwtEncoder, SmartLabJwtProperties properties) {
			return new JwtTokenService(
					jwtEncoder,
					properties,
					Clock.fixed(Instant.now().plusSeconds(3_600), ZoneOffset.UTC));
		}

		@Bean
		UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
			String encodedPassword = passwordEncoder.encode("password");
			return username -> switch (username) {
				case "super@example.edu" -> principal(
						username,
						encodedPassword,
						"ROLE_SUPER_ADMIN",
						SecurityAuthorities.permission(SecurityAuthorities.ADMIN_ACCESS));
				case "admin@example.edu" -> principal(
						username,
						encodedPassword,
						"ROLE_ADMIN",
						SecurityAuthorities.permission(SecurityAuthorities.ADMIN_ACCESS));
				case "admin-role-only@example.edu" -> principal(username, encodedPassword, "ROLE_ADMIN");
				case "leader@example.edu" -> principal(
						username,
						encodedPassword,
						"ROLE_LEADER",
						SecurityAuthorities.permission("POST_CREATE"));
				case "member@example.edu" -> principal(
						username,
						encodedPassword,
						"ROLE_MEMBER",
						SecurityAuthorities.permission("POST_CREATE"));
				case "noroles@example.edu" -> principal(username, encodedPassword);
				default -> throw new UsernameNotFoundException("Authentication failed.");
			};
		}

		private SmartLabUserPrincipal principal(String email, String passwordHash, String... authorities) {
			return new SmartLabUserPrincipal(
					UUID.randomUUID(),
					UUID.randomUUID(),
					email,
					"Test User",
					passwordHash,
					UserAccountStatus.ACTIVE,
					java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
		}
	}

}

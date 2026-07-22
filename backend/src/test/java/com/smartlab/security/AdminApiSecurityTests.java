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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.config.SecurityConfig;
import com.smartlab.dto.request.admin.CreateAdminUserRequest;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.exception.ApiExceptionHandler;

import jakarta.validation.Valid;

@WebMvcTest(controllers = AdminApiSecurityTests.SecurityProbeController.class)
@ActiveProfiles("security-test")
@Import({
		ApiExceptionHandler.class,
		RestAccessDeniedHandler.class,
		RestAuthenticationEntryPoint.class,
		SecurityConfig.class,
		AdminApiSecurityTests.SecurityProbeController.class,
		AdminApiSecurityTests.TestUsers.class})
class AdminApiSecurityTests {

	@Autowired
	private MockMvc mockMvc;

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
				.andExpect(content().string(not(containsString("BadCredentialsException"))))
				.andExpect(content().string(not(containsString("password"))));
	}

	@Test
	void adminRolesCanAccessAdminApi() throws Exception {
		mockMvc.perform(get("/api/admin/probe").with(httpBasic("super@example.edu", "password")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/admin/probe").with(httpBasic("admin@example.edu", "password")))
				.andExpect(status().isOk());
	}

	@Test
	void nonAdminRolesReceiveJsonForbidden() throws Exception {
		for (String username : List.of("leader@example.edu", "member@example.edu", "noroles@example.edu")) {
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
	void unrelatedApiEndpointsRequireAuthenticationButNotAdminRole() throws Exception {
		mockMvc.perform(get("/api/probe"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
		mockMvc.perform(get("/api/probe").with(httpBasic("member@example.edu", "password")))
				.andExpect(status().isOk());
	}

	@Test
	void csrfIsIgnoredForRestApiMutationsButAuthenticationStillRequired() throws Exception {
		mockMvc.perform(post("/api/admin/probe")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}")
						.with(httpBasic("admin@example.edu", "password")))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("Request validation failed."));
		mockMvc.perform(patch("/api/admin/probe").with(httpBasic("admin@example.edu", "password")))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/admin/probe").with(httpBasic("admin@example.edu", "password")))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/admin/probe").with(httpBasic("admin@example.edu", "password")))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/admin/probe")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
	}

	@Test
	void statelessHttpBasicDoesNotRequireOrCreateSessionCookie() throws Exception {
		mockMvc.perform(get("/api/admin/probe").with(httpBasic("admin@example.edu", "password")))
				.andExpect(status().isOk())
				.andExpect(cookie().doesNotExist("JSESSIONID"));
	}

	@Test
	void wrongPasswordReturnsGenericJsonUnauthorized() throws Exception {
		mockMvc.perform(get("/api/admin/probe").with(httpBasic("admin@example.edu", "wrong-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("Authentication is required."))
				.andExpect(content().string(not(containsString("admin@example.edu"))))
				.andExpect(content().string(not(containsString("password"))));
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
		UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
			String encodedPassword = passwordEncoder.encode("password");
			return username -> switch (username) {
				case "super@example.edu" -> principal(username, encodedPassword, "ROLE_SUPER_ADMIN");
				case "admin@example.edu" -> principal(username, encodedPassword, "ROLE_ADMIN");
				case "leader@example.edu" -> principal(username, encodedPassword, "ROLE_LEADER");
				case "member@example.edu" -> principal(username, encodedPassword, "ROLE_MEMBER");
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

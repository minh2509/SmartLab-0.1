package com.smartlab.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;

import com.smartlab.config.SecurityConfig;

class SecurityStructuralTests {

	@Test
	void securityConfigurationUsesCurrentSpringSecurityTypes() throws NoSuchMethodException {
		assertTrue(SecurityConfig.class.getMethod("securityFilterChain",
				org.springframework.security.config.annotation.web.builders.HttpSecurity.class,
				org.springframework.security.authentication.AuthenticationProvider.class,
				RestAuthenticationEntryPoint.class,
				RestAccessDeniedHandler.class).getReturnType() == SecurityFilterChain.class);
		assertTrue(SecurityConfig.class.getMethod("authenticationProvider",
				org.springframework.security.core.userdetails.UserDetailsService.class,
				org.springframework.security.crypto.password.PasswordEncoder.class).getReturnType()
				== DaoAuthenticationProvider.class);
	}

	@Test
	void deferredSecurityFeaturesAndForbiddenImplementationsAreAbsent() throws IOException {
		List<Path> javaFiles;
		try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/smartlab"))) {
			javaFiles = files.filter(path -> path.toString().endsWith(".java"))
					.filter(path -> !hasPathSegment(path, "repository"))
					.filter(path -> !hasPathSegment(path, "entity"))
					.toList();
		}
		String allMainJava = javaFiles.stream()
				.map(SecurityStructuralTests::readString)
				.reduce("", (left, right) -> left + "\n" + right);

		assertFalse(allMainJava.contains("InMemoryUserDetailsManager"));
		assertFalse(allMainJava.contains("CorsConfiguration"));
		assertFalse(allMainJava.contains(".cors("));
		assertFalse(allMainJava.contains("Jwt"));
		assertFalse(allMainJava.contains("JWT"));
		assertFalse(allMainJava.contains("/api/auth/login"));
		assertFalse(allMainJava.contains("NotificationRepository"));
		assertFalse(allMainJava.contains("setLastLoginAt"));
	}

	private static String readString(Path path) {
		try {
			return Files.readString(path);
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private static boolean hasPathSegment(Path path, String segment) {
		for (Path part : path) {
			if (part.toString().equals(segment)) {
				return true;
			}
		}
		return false;
	}
}

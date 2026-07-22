package com.smartlab.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("nodb")
class PasswordEncoderConfigurationTests {

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void bcryptPasswordEncoderBeanMatchesOnlyTheCorrectRawPassword() {
		String encoded = passwordEncoder.encode("correct-password");

		assertTrue(passwordEncoder instanceof BCryptPasswordEncoder);
		assertTrue(passwordEncoder.matches("correct-password", encoded));
		assertFalse(passwordEncoder.matches("wrong-password", encoded));
		assertFalse(passwordEncoder.matches("correct-password", "correct-password"));
		assertFalse(passwordEncoder.matches("correct-password", "{noop}correct-password"));
	}
}

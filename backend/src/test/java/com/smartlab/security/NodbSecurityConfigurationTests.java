package com.smartlab.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("nodb")
class NodbSecurityConfigurationTests {

	@Autowired
	private UserDetailsService userDetailsService;

	@Test
	void nodbUsesNoCredentialUserDetailsServiceInsteadOfGeneratedInMemoryUser() {
		assertFalse(userDetailsService instanceof InMemoryUserDetailsManager);
		assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("admin@example.edu"));
	}
}

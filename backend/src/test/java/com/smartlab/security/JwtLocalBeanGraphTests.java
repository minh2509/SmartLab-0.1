package com.smartlab.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import com.smartlab.config.JwtSecurityConfig;
import com.smartlab.controller.auth.AuthController;
import com.smartlab.mapper.AuthApiMapper;
import com.smartlab.repository.LoginHistoryRepository;
import com.smartlab.repository.UserRepository;

class JwtLocalBeanGraphTests {

	private static final String TEST_SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(LocalJwtBeanGraphConfig.class)
			.withPropertyValues(
					"spring.profiles.active=local",
					"smartlab.jwt.secret-base64=" + TEST_SECRET,
					"smartlab.jwt.access-ttl-seconds=900");

	@Test
	void localJwtBeanGraphInstantiatesJwtServiceControllerAndSupportBeans() {
		contextRunner.run(context -> {
			assertNotNull(context.getBean(JwtTokenService.class));
			assertNotNull(context.getBean(AuthController.class));
			assertNotNull(context.getBean(JwtEncoder.class));
			assertNotNull(context.getBean(JwtDecoder.class));
			assertNotNull(context.getBean(SmartLabJwtProperties.class));
			assertNotNull(context.getBean(Clock.class));
		});
	}

	@Configuration
	@Import({
			JwtSecurityConfig.class,
			JwtTokenService.class,
			AuthController.class,
			AuthApiMapper.class})
	static class LocalJwtBeanGraphConfig {

		@Bean
		AuthenticationManager authenticationManager() {
			return authentication -> authentication;
		}

		@Bean
		UserRepository userRepository() {
			return Mockito.mock(UserRepository.class);
		}

		@Bean
		LoginHistoryRepository loginHistoryRepository() {
			return Mockito.mock(LoginHistoryRepository.class);
		}
	}
}

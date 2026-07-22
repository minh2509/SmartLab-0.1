package com.smartlab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.smartlab.security.RestAccessDeniedHandler;
import com.smartlab.security.RestAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	@Profile("!nodb")
	public DaoAuthenticationProvider authenticationProvider(
			UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	@Profile("!nodb")
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	@Profile("!nodb")
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			AuthenticationProvider authenticationProvider,
			JwtAuthenticationConverter jwtAuthenticationConverter,
			RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler) throws Exception {
		return configureRestSecurity(http, authenticationEntryPoint, accessDeniedHandler)
				.authenticationProvider(authenticationProvider)
				.oauth2ResourceServer(oauth2 -> oauth2
						.authenticationEntryPoint(authenticationEntryPoint)
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
						.requestMatchers("/api/**").authenticated()
						.anyRequest().denyAll())
				.build();
	}

	@Bean
	@Profile("nodb")
	public UserDetailsService nodbUserDetailsService() {
		return username -> {
			throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Authentication failed.");
		};
	}

	@Bean
	@Profile("nodb")
	public SecurityFilterChain nodbSecurityFilterChain(
			HttpSecurity http,
			RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler) throws Exception {
		return configureRestSecurity(http, authenticationEntryPoint, accessDeniedHandler)
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/api/**").denyAll()
						.anyRequest().denyAll())
				.build();
	}

	private static HttpSecurity configureRestSecurity(
			HttpSecurity http,
			RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler) throws Exception {
		return http
				.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable);
	}
}

package com.smartlab.config;

import java.io.IOException;
import java.util.Set;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import com.smartlab.security.RestAccessDeniedHandler;
import com.smartlab.security.RestAuthenticationEntryPoint;
import com.smartlab.security.SecurityAuthorities;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

	private static final Set<String> LOCAL_FRONTEND_ORIGINS = Set.of(
			"http://localhost:5173",
			"http://127.0.0.1:5173");

	private static final String[] SWAGGER_ENDPOINTS = {
			"/v3/api-docs",
			"/v3/api-docs/**",
			"/swagger-ui.html",
			"/swagger-ui/**"
	};

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public FilterRegistrationBean<OncePerRequestFilter> localFrontendAccessFilter() {
		FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(new OncePerRequestFilter() {
			@Override
			protected void doFilterInternal(
					HttpServletRequest request,
					HttpServletResponse response,
					FilterChain filterChain) throws ServletException, IOException {
				String origin = request.getHeader("Origin");
				if (origin != null && LOCAL_FRONTEND_ORIGINS.contains(origin)) {
					response.setHeader("Access-Control-Allow-Origin", origin);
					response.setHeader("Access-Control-Allow-Credentials", "true");
					response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
					response.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,Accept");
					response.addHeader("Vary", "Origin");
				}
				if (HttpMethod.OPTIONS.matches(request.getMethod())) {
					response.setStatus(HttpServletResponse.SC_OK);
					return;
				}
				filterChain.doFilter(request, response);
			}
		});
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		registration.addUrlPatterns("/api/*");
		return registration;
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
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			AuthenticationProvider authenticationProvider,
			RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler) throws Exception {
		return configureRestSecurity(http, authenticationEntryPoint, accessDeniedHandler)
				.authenticationProvider(authenticationProvider)
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/error").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
						.requestMatchers(SWAGGER_ENDPOINTS).permitAll()
						.requestMatchers("/api/admin/**")
						.hasAuthority(SecurityAuthorities.permission(SecurityAuthorities.ADMIN_ACCESS))
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
						.requestMatchers("/error").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
						.requestMatchers(SWAGGER_ENDPOINTS).permitAll()
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
				.httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(authenticationEntryPoint))
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable);
	}
}

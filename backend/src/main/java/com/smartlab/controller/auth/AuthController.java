package com.smartlab.controller.auth;

import java.time.OffsetDateTime;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.request.auth.LoginRequest;
import com.smartlab.dto.response.auth.AuthenticatedUserResponse;
import com.smartlab.dto.response.auth.LoginResponse;
import com.smartlab.entity.LoginHistory;
import com.smartlab.mapper.AuthApiMapper;
import com.smartlab.repository.LoginHistoryRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.security.IssuedAccessToken;
import com.smartlab.security.JwtTokenService;
import com.smartlab.security.SmartLabUserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Profile("!nodb")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenService jwtTokenService;
	private final AuthApiMapper mapper;
	private final UserRepository userRepository;
	private final LoginHistoryRepository loginHistoryRepository;

	public AuthController(
			AuthenticationManager authenticationManager,
			JwtTokenService jwtTokenService,
			AuthApiMapper mapper,
			UserRepository userRepository,
			LoginHistoryRepository loginHistoryRepository) {
		this.authenticationManager = authenticationManager;
		this.jwtTokenService = jwtTokenService;
		this.mapper = mapper;
		this.userRepository = userRepository;
		this.loginHistoryRepository = loginHistoryRepository;
	}

	@PostMapping("/login")
	@Transactional
	public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
		Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
		SmartLabUserPrincipal principal = (SmartLabUserPrincipal) authentication.getPrincipal();
		recordSuccessfulLogin(principal, servletRequest);
		IssuedAccessToken token = jwtTokenService.issueAccessToken(principal);
		return mapper.toLoginResponse(token, principal);
	}

	@GetMapping("/me")
	public AuthenticatedUserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return mapper.toUserResponse(jwt);
	}

	private void recordSuccessfulLogin(SmartLabUserPrincipal principal, HttpServletRequest servletRequest) {
		userRepository.findById(principal.userId()).ifPresent(user -> {
			user.setLastLoginAt(OffsetDateTime.now());
			LoginHistory loginHistory = new LoginHistory();
			loginHistory.setUser(user);
			loginHistory.setSuccess(true);
			loginHistory.setIpAddress(clientIp(servletRequest));
			loginHistory.setUserAgent(servletRequest.getHeader("User-Agent"));
			loginHistoryRepository.save(loginHistory);
		});
	}

	private static String clientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}

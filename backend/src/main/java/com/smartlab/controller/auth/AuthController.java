package com.smartlab.controller.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.request.auth.LoginRequest;
import com.smartlab.dto.response.auth.AuthenticatedUserResponse;
import com.smartlab.dto.response.auth.LoginResponse;
import com.smartlab.mapper.AuthApiMapper;
import com.smartlab.security.IssuedAccessToken;
import com.smartlab.security.JwtTokenService;
import com.smartlab.security.SmartLabUserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Profile("!nodb")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenService jwtTokenService;
	private final AuthApiMapper mapper;

	public AuthController(
			AuthenticationManager authenticationManager,
			JwtTokenService jwtTokenService,
			AuthApiMapper mapper) {
		this.authenticationManager = authenticationManager;
		this.jwtTokenService = jwtTokenService;
		this.mapper = mapper;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
		SmartLabUserPrincipal principal = (SmartLabUserPrincipal) authentication.getPrincipal();
		IssuedAccessToken token = jwtTokenService.issueAccessToken(principal);
		return mapper.toLoginResponse(token, principal);
	}

	@GetMapping("/me")
	public AuthenticatedUserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return mapper.toUserResponse(jwt);
	}
}

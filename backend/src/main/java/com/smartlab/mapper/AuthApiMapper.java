package com.smartlab.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.smartlab.dto.response.auth.AuthenticatedUserResponse;
import com.smartlab.dto.response.auth.LoginResponse;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.security.IssuedAccessToken;
import com.smartlab.security.SmartLabUserPrincipal;

@Component
public class AuthApiMapper {

	public LoginResponse toLoginResponse(IssuedAccessToken token, SmartLabUserPrincipal principal) {
		return new LoginResponse(
				token.tokenType(),
				token.tokenValue(),
				token.expiresIn(),
				token.issuedAt(),
				token.expiresAt(),
				toUserResponse(principal));
	}

	public AuthenticatedUserResponse toUserResponse(SmartLabUserPrincipal principal) {
		return new AuthenticatedUserResponse(
				principal.userId(),
				principal.labId(),
				principal.email(),
				principal.fullName(),
				principal.accountStatus(),
				roleCodes(principal));
	}

	public AuthenticatedUserResponse toUserResponse(Jwt jwt) {
		return new AuthenticatedUserResponse(
				UUID.fromString(jwt.getSubject()),
				UUID.fromString(jwt.getClaimAsString("lab_id")),
				jwt.getClaimAsString("email"),
				jwt.getClaimAsString("full_name"),
				UserAccountStatus.valueOf(jwt.getClaimAsString("account_status")),
				jwt.getClaimAsStringList("roles").stream().distinct().sorted().toList());
	}

	private static List<String> roleCodes(SmartLabUserPrincipal principal) {
		return principal.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(authority -> authority.startsWith("ROLE_"))
				.map(authority -> authority.substring("ROLE_".length()))
				.distinct()
				.sorted()
				.toList();
	}
}

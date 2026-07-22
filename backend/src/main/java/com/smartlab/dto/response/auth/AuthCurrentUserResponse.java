package com.smartlab.dto.response.auth;

import java.util.List;
import java.util.UUID;

import com.smartlab.enums.UserAccountStatus;

public record AuthCurrentUserResponse(
		UUID id,
		UUID labId,
		String email,
		String fullName,
		UserAccountStatus accountStatus,
		List<String> roleCodes) {
}

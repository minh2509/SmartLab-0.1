package com.smartlab.dto.response.admin;

import java.util.List;
import java.util.UUID;

import com.smartlab.enums.UserAccountStatus;

public record AdminUserResponse(
		UUID id,
		UUID labId,
		String username,
		String email,
		String fullName,
		UUID avatarFileId,
		UserAccountStatus accountStatus,
		List<String> roleCodes) {
}

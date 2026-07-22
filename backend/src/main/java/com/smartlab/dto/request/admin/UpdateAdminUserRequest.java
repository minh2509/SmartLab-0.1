package com.smartlab.dto.request.admin;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateAdminUserRequest(
		@Size(max = 100) String username,
		@Email @Size(max = 255) String email,
		@Size(max = 255) String fullName,
		UUID avatarFileId,
		boolean clearAvatarFile) {
}

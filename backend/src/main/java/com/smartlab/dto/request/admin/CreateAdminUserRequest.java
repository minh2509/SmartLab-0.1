package com.smartlab.dto.request.admin;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateAdminUserRequest(
		@NotBlank @Size(max = 100) String username,
		@NotBlank @Email @Size(max = 255) String email,
		String temporaryPassword,
		@NotBlank @Size(max = 255) String fullName,
		UUID avatarFileId,
		@NotEmpty List<@NotBlank @Size(max = 50) String> roleCodes) {
}

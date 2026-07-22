package com.smartlab.dto.request.admin;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAdminUserRequest(
		@NotNull UUID labId,
		@NotBlank @Size(max = 100) String username,
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(min = 8, max = 255) String password,
		@NotBlank @Size(max = 255) String fullName,
		UUID avatarFileId) {
}

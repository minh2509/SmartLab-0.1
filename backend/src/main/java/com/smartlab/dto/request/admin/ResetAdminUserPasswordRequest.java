package com.smartlab.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetAdminUserPasswordRequest(
		@NotBlank @Size(max = 255) String passwordHash) {
}

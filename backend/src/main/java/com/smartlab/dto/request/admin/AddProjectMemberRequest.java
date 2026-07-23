package com.smartlab.dto.request.admin;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddProjectMemberRequest(
		@NotNull UUID userId,
		@NotBlank String role) {
}

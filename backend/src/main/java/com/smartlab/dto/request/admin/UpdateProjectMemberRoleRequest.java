package com.smartlab.dto.request.admin;

import jakarta.validation.constraints.NotBlank;

public record UpdateProjectMemberRoleRequest(@NotBlank String role) {
}

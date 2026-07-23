package com.smartlab.dto.request.admin;

import jakarta.validation.constraints.NotBlank;

public record UpdateProjectStatusRequest(@NotBlank String status) {
}

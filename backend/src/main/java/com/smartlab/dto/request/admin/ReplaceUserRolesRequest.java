package com.smartlab.dto.request.admin;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ReplaceUserRolesRequest(
		@NotEmpty List<@NotBlank @Size(max = 50) String> roleCodes) {
}

package com.smartlab.dto.request.admin;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record ReplaceProjectResearchFieldsRequest(
		@NotEmpty List<@NotBlank String> fields) {
}

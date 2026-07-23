package com.smartlab.dto.request.admin;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveAdminProjectRequest(
		@NotBlank @Size(max = 100) String code,
		@NotBlank @Size(max = 255) String name,
		@NotBlank String description,
		@NotBlank String objective,
		@NotBlank String type,
		@NotEmpty List<@NotBlank String> fields,
		@NotEmpty List<@NotNull UUID> leaderIds,
		@NotNull LocalDate startDate,
		@NotNull LocalDate expectedEnd,
		@NotBlank String status,
		@NotNull @Min(0) @Max(100) Integer progress,
		@NotBlank String visibility) {
}

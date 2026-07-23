package com.smartlab.dto.request.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateProjectProgressRequest(@NotNull @Min(0) @Max(100) Integer progress) {
}

package com.smartlab.dto.request.admin;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateLabImageRequest(@NotNull UUID fileId) {
}

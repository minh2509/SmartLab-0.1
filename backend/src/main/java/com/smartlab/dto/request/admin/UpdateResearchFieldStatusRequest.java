package com.smartlab.dto.request.admin;

import com.smartlab.enums.CatalogStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateResearchFieldStatusRequest(@NotNull CatalogStatus status) {
}

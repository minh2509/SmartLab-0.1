package com.smartlab.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.smartlab.enums.CatalogStatus;

public record AdminResearchFieldResponse(
		UUID id,
		String code,
		String name,
		String description,
		CatalogStatus status,
		OffsetDateTime createdAt) {
}

package com.smartlab.dto.response.admin;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminProjectResponse(
		UUID id,
		String slug,
		String code,
		String name,
		String description,
		String objective,
		String type,
		List<String> fields,
		List<UUID> leaderIds,
		List<UUID> memberIds,
		LocalDate startDate,
		LocalDate expectedEnd,
		String status,
		int progress,
		String visibility,
		LocalDate actualEndDate,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}

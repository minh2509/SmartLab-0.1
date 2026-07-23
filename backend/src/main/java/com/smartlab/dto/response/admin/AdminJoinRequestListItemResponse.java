package com.smartlab.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.smartlab.enums.ProjectJoinRequestStatus;

public record AdminJoinRequestListItemResponse(
		UUID id,
		ProjectSummary project,
		UserSummary requester,
		String desiredPosition,
		ProjectJoinRequestStatus status,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {

	public record ProjectSummary(UUID id, String code, String name, String slug) {
	}

	public record UserSummary(UUID id, String fullName, String email) {
	}
}

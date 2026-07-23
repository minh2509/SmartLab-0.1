package com.smartlab.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.smartlab.enums.ProjectJoinRequestStatus;

public record AdminJoinRequestOptionResponse(
		UUID id,
		ProjectSummary project,
		UserSummary requester,
		ProjectJoinRequestStatus status,
		OffsetDateTime createdAt) {

	public record ProjectSummary(UUID id, String code, String name, String slug) {
	}

	public record UserSummary(UUID id, String fullName, String email) {
	}
}

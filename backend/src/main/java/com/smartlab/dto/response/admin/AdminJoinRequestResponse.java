package com.smartlab.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.smartlab.enums.FileVisibility;
import com.smartlab.enums.ProjectJoinRequestStatus;

public record AdminJoinRequestResponse(
		UUID id,
		ProjectSummary project,
		UserSummary requester,
		String desiredPosition,
		String reason,
		String skills,
		String experience,
		String introduction,
		CvFileSummary cvFile,
		ProjectJoinRequestStatus status,
		UserSummary reviewedBy,
		OffsetDateTime reviewedAt,
		String rejectionReason,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {

	public record ProjectSummary(UUID id, String code, String name, String slug) {
	}

	public record UserSummary(UUID id, String fullName, String email) {
	}

	public record CvFileSummary(
			UUID id,
			String originalName,
			String mimeType,
			Long fileSize,
			FileVisibility visibility) {
	}
}

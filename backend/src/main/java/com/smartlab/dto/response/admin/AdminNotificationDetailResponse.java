package com.smartlab.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminNotificationDetailResponse(
		UUID id,
		String title,
		String message,
		String notificationType,
		String relatedType,
		UUID relatedId,
		String linkUrl,
		UserSummary createdBy,
		long recipientCount,
		long readCount,
		OffsetDateTime createdAt,
		List<RecipientResponse> recipients) {

	public AdminNotificationDetailResponse {
		recipients = List.copyOf(recipients);
	}

	public record UserSummary(UUID id, String fullName) {
	}

	public record RecipientResponse(
			UUID userId,
			String fullName,
			OffsetDateTime readAt,
			OffsetDateTime hiddenAt,
			OffsetDateTime createdAt) {
	}
}

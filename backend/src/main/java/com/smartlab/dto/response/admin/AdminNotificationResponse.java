package com.smartlab.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminNotificationResponse(
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
		OffsetDateTime createdAt) {

	public record UserSummary(UUID id, String fullName) {
	}
}

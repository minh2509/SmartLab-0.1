package com.smartlab.dto.request.admin;

import java.util.List;
import java.util.UUID;

import com.smartlab.enums.NotificationTargetType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAdminNotificationRequest(
		@NotBlank
		@Size(max = 255)
		String title,
		String message,
		@NotBlank
		@Size(max = 80)
		String notificationType,
		@NotNull
		NotificationTargetType targetType,
		List<UUID> userIds,
		UUID projectId,
		@Size(max = 80)
		String relatedType,
		UUID relatedId,
		@Size(max = 500)
		String linkUrl) {

	public CreateAdminNotificationRequest {
		userIds = userIds == null ? List.of() : List.copyOf(userIds);
	}
}

package com.smartlab.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.smartlab.enums.PostModerationAction;
import com.smartlab.enums.PostStatus;

public record AdminPostModerationActionResponse(
		UUID postId,
		PostModerationAction action,
		PostStatus fromStatus,
		PostStatus toStatus,
		PostStatus moderationStatus,
		UUID reviewedById,
		String reviewedByName,
		OffsetDateTime reviewedAt) {
}

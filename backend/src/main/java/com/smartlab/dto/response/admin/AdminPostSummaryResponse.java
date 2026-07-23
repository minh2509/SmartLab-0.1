package com.smartlab.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.smartlab.enums.PostContentType;
import com.smartlab.enums.PostStatus;
import com.smartlab.enums.PostVisibility;

public record AdminPostSummaryResponse(
		UUID id,
		String title,
		String slug,
		String summary,
		PostContentType contentType,
		PostVisibility visibility,
		PostStatus moderationStatus,
		UUID authorId,
		String authorName,
		UUID projectId,
		String projectName,
		UUID categoryId,
		String categoryName,
		UUID coverFileId,
		OffsetDateTime publishedAt,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}

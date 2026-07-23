package com.smartlab.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.smartlab.enums.PostContentType;
import com.smartlab.enums.PostModerationAction;
import com.smartlab.enums.PostStatus;
import com.smartlab.enums.PostVisibility;

public record AdminPostDetailResponse(
		UUID id,
		String title,
		String slug,
		String summary,
		String content,
		PostContentType contentType,
		PostVisibility visibility,
		PostStatus moderationStatus,
		AuthorResponse author,
		ProjectResponse project,
		CategoryResponse category,
		FileResponse coverFile,
		List<AttachmentResponse> attachments,
		List<ModerationHistoryResponse> moderationHistory,
		OffsetDateTime publishedAt,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {

	public record AuthorResponse(
			UUID id,
			String fullName) {
	}

	public record ProjectResponse(
			UUID id,
			String name) {
	}

	public record CategoryResponse(
			UUID id,
			String name) {
	}

	public record FileResponse(
			UUID id,
			String originalName,
			String mimeType,
			Long fileSize,
			String fileExtension,
			OffsetDateTime createdAt) {
	}

	public record AttachmentResponse(
			UUID attachmentId,
			UUID fileId,
			String originalName,
			String mimeType,
			Long fileSize,
			String fileExtension,
			UUID uploadedById,
			String uploadedByName,
			OffsetDateTime createdAt) {
	}

	public record ModerationHistoryResponse(
			UUID id,
			PostModerationAction action,
			PostStatus fromStatus,
			PostStatus toStatus,
			UUID actorId,
			String actorName,
			String reason,
			OffsetDateTime createdAt) {
	}
}

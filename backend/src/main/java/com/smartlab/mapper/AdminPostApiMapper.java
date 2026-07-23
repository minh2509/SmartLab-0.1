package com.smartlab.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.smartlab.dto.response.admin.AdminPostDetailResponse;
import com.smartlab.dto.response.admin.AdminPostPageResponse;
import com.smartlab.dto.response.admin.AdminPostSummaryResponse;
import com.smartlab.entity.File;
import com.smartlab.entity.Post;
import com.smartlab.entity.PostAttachment;
import com.smartlab.entity.PostCategory;
import com.smartlab.entity.PostModerationLog;
import com.smartlab.entity.Project;
import com.smartlab.entity.User;

@Component
public class AdminPostApiMapper {

	public AdminPostPageResponse toPageResponse(Page<Post> posts) {
		return new AdminPostPageResponse(
				posts.getContent().stream().map(this::toSummaryResponse).toList(),
				posts.getNumber(),
				posts.getSize(),
				posts.getTotalElements(),
				posts.getTotalPages(),
				posts.isFirst(),
				posts.isLast());
	}

	public AdminPostSummaryResponse toSummaryResponse(Post post) {
		User author = post.getAuthor();
		Project project = post.getProject();
		PostCategory category = post.getCategory();
		File coverFile = post.getCoverFile();
		return new AdminPostSummaryResponse(
				post.getId(),
				post.getTitle(),
				post.getSlug(),
				post.getSummary(),
				post.getContentType(),
				post.getVisibility(),
				post.getModerationStatus(),
				idOf(author),
				author == null ? null : author.getFullName(),
				idOf(project),
				project == null ? null : project.getName(),
				idOf(category),
				category == null ? null : category.getName(),
				idOf(coverFile),
				post.getPublishedAt(),
				post.getCreatedAt(),
				post.getUpdatedAt());
	}

	public AdminPostDetailResponse toDetailResponse(
			Post post,
			List<PostAttachment> attachments,
			List<PostModerationLog> moderationHistory) {
		User author = post.getAuthor();
		Project project = post.getProject();
		PostCategory category = post.getCategory();
		File coverFile = post.getCoverFile();
		return new AdminPostDetailResponse(
				post.getId(),
				post.getTitle(),
				post.getSlug(),
				post.getSummary(),
				post.getContent(),
				post.getContentType(),
				post.getVisibility(),
				post.getModerationStatus(),
				authorResponse(author),
				projectResponse(project),
				categoryResponse(category),
				fileResponse(coverFile),
				attachments.stream().map(this::attachmentResponse).toList(),
				moderationHistory.stream().map(this::moderationHistoryResponse).toList(),
				post.getPublishedAt(),
				post.getCreatedAt(),
				post.getUpdatedAt());
	}

	private AdminPostDetailResponse.AuthorResponse authorResponse(User author) {
		if (author == null) {
			return null;
		}
		return new AdminPostDetailResponse.AuthorResponse(author.getId(), author.getFullName());
	}

	private AdminPostDetailResponse.ProjectResponse projectResponse(Project project) {
		if (project == null) {
			return null;
		}
		return new AdminPostDetailResponse.ProjectResponse(project.getId(), project.getName());
	}

	private AdminPostDetailResponse.CategoryResponse categoryResponse(PostCategory category) {
		if (category == null) {
			return null;
		}
		return new AdminPostDetailResponse.CategoryResponse(category.getId(), category.getName());
	}

	private AdminPostDetailResponse.FileResponse fileResponse(File file) {
		if (file == null || file.getDeletedAt() != null) {
			return null;
		}
		return new AdminPostDetailResponse.FileResponse(
				file.getId(),
				file.getOriginalName(),
				file.getMimeType(),
				file.getFileSize(),
				file.getFileExtension(),
				file.getCreatedAt());
	}

	private AdminPostDetailResponse.AttachmentResponse attachmentResponse(PostAttachment attachment) {
		File file = attachment.getFile();
		User uploadedBy = attachment.getUploadedBy();
		return new AdminPostDetailResponse.AttachmentResponse(
				attachment.getId(),
				idOf(file),
				file == null ? null : file.getOriginalName(),
				file == null ? null : file.getMimeType(),
				file == null ? null : file.getFileSize(),
				file == null ? null : file.getFileExtension(),
				idOf(uploadedBy),
				uploadedBy == null ? null : uploadedBy.getFullName(),
				attachment.getCreatedAt());
	}

	private AdminPostDetailResponse.ModerationHistoryResponse moderationHistoryResponse(PostModerationLog log) {
		User actor = log.getActor();
		return new AdminPostDetailResponse.ModerationHistoryResponse(
				log.getId(),
				log.getAction(),
				log.getFromStatus(),
				log.getToStatus(),
				idOf(actor),
				actor == null ? null : actor.getFullName(),
				log.getReason(),
				log.getCreatedAt());
	}

	private static UUID idOf(User user) {
		return user == null ? null : user.getId();
	}

	private static UUID idOf(Project project) {
		return project == null ? null : project.getId();
	}

	private static UUID idOf(PostCategory category) {
		return category == null ? null : category.getId();
	}

	private static UUID idOf(File file) {
		return file == null ? null : file.getId();
	}
}

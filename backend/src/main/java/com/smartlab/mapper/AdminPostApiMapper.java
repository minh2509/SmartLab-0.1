package com.smartlab.mapper;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.smartlab.dto.response.admin.AdminPostPageResponse;
import com.smartlab.dto.response.admin.AdminPostSummaryResponse;
import com.smartlab.entity.File;
import com.smartlab.entity.Post;
import com.smartlab.entity.PostCategory;
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

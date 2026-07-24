package com.smartlab.controller.admin;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.response.admin.AdminPostDetailResponse;
import com.smartlab.dto.response.admin.AdminPostModerationActionResponse;
import com.smartlab.dto.response.admin.AdminPostPageResponse;
import com.smartlab.enums.PostContentType;
import com.smartlab.enums.PostStatus;
import com.smartlab.enums.PostVisibility;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminPostService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/admin/posts")
@Profile("!nodb")
@Validated
public class AdminPostController {

	private final AdminPostService adminPostService;
	private final AuthenticatedActorResolver actorResolver;

	public AdminPostController(AdminPostService adminPostService, AuthenticatedActorResolver actorResolver) {
		this.adminPostService = adminPostService;
		this.actorResolver = actorResolver;
	}

	@GetMapping
	public AdminPostPageResponse listPosts(
			@RequestParam(required = false) @Min(0) Integer page,
			@RequestParam(required = false) @Min(1) @Max(100) Integer size,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) PostStatus status,
			@RequestParam(name = "type", required = false) PostContentType contentType,
			@RequestParam(required = false) UUID authorId,
			@RequestParam(required = false) UUID projectId,
			@RequestParam(required = false) PostVisibility visibility) {
		return adminPostService.listPosts(new AdminPostService.ListAdminPostsQuery(
				actorResolver.requireActorUserId(),
				page,
				size,
				keyword,
				status,
				contentType,
				authorId,
				projectId,
				visibility));
	}

	@GetMapping("/pending")
	public AdminPostPageResponse listPendingPosts(
			@RequestParam(required = false) @Min(0) Integer page,
			@RequestParam(required = false) @Min(1) @Max(100) Integer size) {
		return adminPostService.listPendingPosts(new AdminPostService.ListPendingAdminPostsQuery(
				actorResolver.requireActorUserId(),
				page,
				size));
	}

	@GetMapping("/{postId}")
	public AdminPostDetailResponse getPostDetail(@PathVariable UUID postId) {
		return adminPostService.getPostDetail(new AdminPostService.GetAdminPostDetailQuery(
				actorResolver.requireActorUserId(),
				postId));
	}

	@PostMapping("/{postId}/approve")
	public AdminPostModerationActionResponse approvePost(@PathVariable UUID postId) {
		return adminPostService.approvePost(new AdminPostService.ApproveAdminPostCommand(
				actorResolver.requireActorUserId(),
				postId));
	}

	@PatchMapping("/{postId}/publish")
	public AdminPostModerationActionResponse publishPost(@PathVariable UUID postId) {
		return adminPostService.publishPost(new AdminPostService.PublishAdminPostCommand(
				actorResolver.requireActorUserId(),
				postId));
	}

	@PatchMapping("/{postId}/unpublish")
	public AdminPostModerationActionResponse unpublishPost(@PathVariable UUID postId) {
		return adminPostService.unpublishPost(new AdminPostService.UnpublishAdminPostCommand(
				actorResolver.requireActorUserId(),
				postId));
	}
}

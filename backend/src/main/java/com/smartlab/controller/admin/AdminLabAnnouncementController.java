package com.smartlab.controller.admin;

import java.net.URI;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.request.admin.CreateAdminLabAnnouncementRequest;
import com.smartlab.dto.response.admin.AdminPostDetailResponse;
import com.smartlab.dto.response.admin.AdminPostPageResponse;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminPostService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/admin/lab-announcements")
@Profile("!nodb")
@Validated
public class AdminLabAnnouncementController {

	private final AdminPostService adminPostService;
	private final AuthenticatedActorResolver actorResolver;

	public AdminLabAnnouncementController(AdminPostService adminPostService, AuthenticatedActorResolver actorResolver) {
		this.adminPostService = adminPostService;
		this.actorResolver = actorResolver;
	}

	@GetMapping
	public AdminPostPageResponse listLabAnnouncements(
			@RequestParam(required = false) @Min(0) Integer page,
			@RequestParam(required = false) @Min(1) @Max(100) Integer size) {
		return adminPostService.listLabAnnouncements(new AdminPostService.ListLabAnnouncementsQuery(
				actorResolver.requireActorUserId(),
				page,
				size));
	}

	@GetMapping("/{postId}")
	public AdminPostDetailResponse getLabAnnouncementDetail(@PathVariable UUID postId) {
		return adminPostService.getLabAnnouncementDetail(new AdminPostService.GetAdminLabAnnouncementDetailQuery(
				actorResolver.requireActorUserId(),
				postId));
	}

	@PostMapping
	public ResponseEntity<AdminPostDetailResponse> createLabAnnouncement(
			@Valid @RequestBody CreateAdminLabAnnouncementRequest request) {
		AdminPostDetailResponse response = adminPostService.createLabAnnouncement(
				new AdminPostService.CreateAdminLabAnnouncementCommand(
						actorResolver.requireActorUserId(),
						request.title(),
						request.summary(),
						request.content(),
						request.visibility(),
						request.publishNow()));
		return ResponseEntity.created(URI.create("/api/admin/lab-announcements/" + response.id()))
				.body(response);
	}
}

package com.smartlab.controller.admin;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.response.admin.AdminPostDetailResponse;
import com.smartlab.dto.response.admin.AdminPostPageResponse;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminPostService;

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
}

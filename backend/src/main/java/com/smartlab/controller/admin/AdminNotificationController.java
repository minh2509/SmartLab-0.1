package com.smartlab.controller.admin;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.request.admin.CreateAdminNotificationRequest;
import com.smartlab.dto.response.admin.AdminNotificationDetailResponse;
import com.smartlab.dto.response.admin.AdminNotificationFilterOptionsResponse;
import com.smartlab.dto.response.admin.AdminNotificationResponse;
import com.smartlab.dto.response.admin.PageResponse;
import com.smartlab.security.AdminJwtClaims;
import com.smartlab.service.admin.AdminNotificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/notifications")
@Profile("!nodb")
public class AdminNotificationController {

	private final AdminNotificationService adminNotificationService;

	public AdminNotificationController(AdminNotificationService adminNotificationService) {
		this.adminNotificationService = adminNotificationService;
	}

	@GetMapping
	public PageResponse<AdminNotificationResponse> getNotifications(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) String notificationType,
			@RequestParam(required = false) UUID creatorId,
			@RequestParam(required = false) String relatedType,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime createdFrom,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime createdTo,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		var result = adminNotificationService.getNotifications(
				AdminJwtClaims.labId(jwt),
				new AdminNotificationService.NotificationFilter(
						notificationType, creatorId, relatedType, createdFrom, createdTo),
				page,
				size);
		return new PageResponse<>(
				result.getContent().stream().map(AdminNotificationController::toResponse).toList(),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages());
	}

	@GetMapping("/options")
	public AdminNotificationFilterOptionsResponse getFilterOptions(
			@AuthenticationPrincipal Jwt jwt) {
		AdminNotificationService.NotificationFilterOptions options =
				adminNotificationService.getFilterOptions(AdminJwtClaims.labId(jwt));
		return new AdminNotificationFilterOptionsResponse(
				options.notificationTypes(),
				options.creatableNotificationTypes(),
				options.relatedTypes(),
				options.creators()
						.stream()
						.map(creator -> new AdminNotificationFilterOptionsResponse.CreatorOption(
								creator.id(),
								creator.fullName(),
								creator.email(),
								creator.accountStatus()))
						.toList());
	}

	@GetMapping("/{notificationId}")
	public AdminNotificationDetailResponse getNotificationDetail(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID notificationId) {
		return toDetailResponse(adminNotificationService.getNotificationDetail(
				AdminJwtClaims.labId(jwt), notificationId));
	}

	@PostMapping
	public ResponseEntity<AdminNotificationDetailResponse> createNotification(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateAdminNotificationRequest request) {
		AdminNotificationService.NotificationDetail created = adminNotificationService.createNotification(
				AdminJwtClaims.labId(jwt),
				AdminJwtClaims.userId(jwt),
				new AdminNotificationService.CreateNotificationCommand(
						request.title(),
						request.message(),
						request.notificationType(),
						request.targetType(),
						request.userIds(),
						request.projectId(),
						request.relatedType(),
						request.relatedId(),
						request.linkUrl()));
		return ResponseEntity
				.created(URI.create("/api/admin/notifications/" + created.summary().id()))
				.body(toDetailResponse(created));
	}

	@DeleteMapping("/{notificationId}")
	public ResponseEntity<Void> hideNotification(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID notificationId) {
		adminNotificationService.hideNotification(
				AdminJwtClaims.labId(jwt), notificationId, AdminJwtClaims.userId(jwt));
		return ResponseEntity.noContent().build();
	}

	private static AdminNotificationResponse toResponse(AdminNotificationService.NotificationSummary summary) {
		return new AdminNotificationResponse(
				summary.id(),
				summary.title(),
				summary.message(),
				summary.notificationType(),
				summary.relatedType(),
				summary.relatedId(),
				summary.linkUrl(),
				summary.createdBy() == null
						? null
						: new AdminNotificationResponse.UserSummary(
								summary.createdBy().id(), summary.createdBy().fullName()),
				summary.recipientCount(),
				summary.readCount(),
				summary.createdAt());
	}

	private static AdminNotificationDetailResponse toDetailResponse(
			AdminNotificationService.NotificationDetail detail) {
		AdminNotificationService.NotificationSummary summary = detail.summary();
		return new AdminNotificationDetailResponse(
				summary.id(),
				summary.title(),
				summary.message(),
				summary.notificationType(),
				summary.relatedType(),
				summary.relatedId(),
				summary.linkUrl(),
				summary.createdBy() == null
						? null
						: new AdminNotificationDetailResponse.UserSummary(
								summary.createdBy().id(), summary.createdBy().fullName()),
				summary.recipientCount(),
				summary.readCount(),
				summary.createdAt(),
				detail.recipients().stream()
						.map(recipient -> new AdminNotificationDetailResponse.RecipientResponse(
								recipient.userId(),
								recipient.fullName(),
								recipient.readAt(),
								recipient.hiddenAt(),
								recipient.createdAt()))
						.toList());
	}
}

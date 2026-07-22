package com.smartlab.controller.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.request.admin.RejectJoinRequestRequest;
import com.smartlab.dto.response.admin.AdminJoinRequestResponse;
import com.smartlab.dto.response.admin.PageResponse;
import com.smartlab.enums.ProjectJoinRequestStatus;
import com.smartlab.security.AdminJwtClaims;
import com.smartlab.service.admin.AdminJoinRequestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/project-join-requests")
@Profile("!nodb")
public class AdminJoinRequestController {

	private final AdminJoinRequestService adminJoinRequestService;

	public AdminJoinRequestController(AdminJoinRequestService adminJoinRequestService) {
		this.adminJoinRequestService = adminJoinRequestService;
	}

	@GetMapping
	public PageResponse<AdminJoinRequestResponse> getJoinRequests(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) UUID projectId,
			@RequestParam(required = false) ProjectJoinRequestStatus status,
			@RequestParam(required = false) UUID requesterId,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime createdFrom,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime createdTo,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		var result = adminJoinRequestService.getJoinRequests(
				AdminJwtClaims.labId(jwt),
				new AdminJoinRequestService.JoinRequestFilter(
						projectId, status, requesterId, createdFrom, createdTo),
				page,
				size);
		return new PageResponse<>(
				result.getContent().stream().map(AdminJoinRequestController::toResponse).toList(),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages());
	}

	@GetMapping("/{requestId}")
	public AdminJoinRequestResponse getJoinRequestDetail(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID requestId) {
		return toResponse(adminJoinRequestService.getJoinRequestDetail(AdminJwtClaims.labId(jwt), requestId));
	}

	@PatchMapping("/{requestId}/approve")
	public AdminJoinRequestResponse approve(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID requestId) {
		return toResponse(adminJoinRequestService.adminApprove(
				AdminJwtClaims.labId(jwt),
				requestId,
				AdminJwtClaims.userId(jwt)));
	}

	@PatchMapping("/{requestId}/reject")
	public AdminJoinRequestResponse reject(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID requestId,
			@Valid @RequestBody RejectJoinRequestRequest request) {
		return toResponse(adminJoinRequestService.adminReject(
				AdminJwtClaims.labId(jwt),
				requestId,
				request.reason(),
				AdminJwtClaims.userId(jwt)));
	}

	private static AdminJoinRequestResponse toResponse(AdminJoinRequestService.JoinRequestSummary summary) {
		return new AdminJoinRequestResponse(
				summary.id(),
				new AdminJoinRequestResponse.ProjectSummary(
						summary.project().id(),
						summary.project().code(),
						summary.project().name(),
						summary.project().slug()),
				toUserResponse(summary.requester()),
				summary.desiredPosition(),
				summary.reason(),
				summary.skills(),
				summary.experience(),
				summary.introduction(),
				summary.cvFile() == null
						? null
						: new AdminJoinRequestResponse.CvFileSummary(
								summary.cvFile().id(),
								summary.cvFile().originalName(),
								summary.cvFile().mimeType(),
								summary.cvFile().fileSize(),
								summary.cvFile().visibility()),
				summary.status(),
				toUserResponse(summary.reviewedBy()),
				summary.reviewedAt(),
				summary.rejectionReason(),
				summary.createdAt(),
				summary.updatedAt());
	}

	private static AdminJoinRequestResponse.UserSummary toUserResponse(
			AdminJoinRequestService.UserSummary user) {
		return user == null
				? null
				: new AdminJoinRequestResponse.UserSummary(user.id(), user.fullName(), user.email());
	}
}

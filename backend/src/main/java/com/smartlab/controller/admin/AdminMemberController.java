package com.smartlab.controller.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.request.admin.UpdateMemberActivityStatusRequest;
import com.smartlab.dto.request.admin.UpdateMemberProfileRequest;
import com.smartlab.dto.request.admin.UpdateMemberResearchFieldsRequest;
import com.smartlab.dto.response.admin.AdminMemberDetailResponse;
import com.smartlab.dto.response.admin.AdminMemberEvaluationResponse;
import com.smartlab.dto.response.admin.AdminMemberProjectResponse;
import com.smartlab.dto.response.admin.AdminMemberSummaryResponse;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminMemberService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/admin/members")
@Profile("!nodb")
@Validated
public class AdminMemberController {

	private final AdminMemberService adminMemberService;
	private final AuthenticatedActorResolver actorResolver;

	public AdminMemberController(
			AdminMemberService adminMemberService,
			AuthenticatedActorResolver actorResolver) {
		this.adminMemberService = adminMemberService;
		this.actorResolver = actorResolver;
	}

	@GetMapping
	public Page<AdminMemberSummaryResponse> listMembers(
			@RequestParam(required = false) @Min(0) Integer page,
			@RequestParam(required = false) @Min(1) @Max(100) Integer size,
			@RequestParam(required = false) String keyword) {
		return adminMemberService.listMembers(new AdminMemberService.ListMembersQuery(
				actorResolver.requireActorUserId(),
				page,
				size,
				keyword));
	}

	@GetMapping("/{userId}")
	public AdminMemberDetailResponse getMemberDetail(@PathVariable UUID userId) {
		return adminMemberService.getMemberDetail(actorResolver.requireActorUserId(), userId);
	}

	@PutMapping("/{userId}/profile")
	public AdminMemberDetailResponse updateMemberProfile(
			@PathVariable UUID userId,
			@Valid @RequestBody UpdateMemberProfileRequest request) {
		return adminMemberService.updateMemberProfile(new AdminMemberService.UpdateMemberProfileCommand(
				actorResolver.requireActorUserId(),
				userId,
				request.getStudentCode(),
				request.getPhone(),
				request.getPersonalEmail(),
				request.getBio(),
				request.getSpecialization(),
				request.getJoinedAt(),
				request.getGithubUrl(),
				request.getLinkedinUrl(),
				request.getPortfolioUrl()));
	}

	@PutMapping("/{userId}/research-fields")
	public AdminMemberDetailResponse updateMemberResearchFields(
			@PathVariable UUID userId,
			@Valid @RequestBody UpdateMemberResearchFieldsRequest request) {
		return adminMemberService.updateMemberResearchFields(new AdminMemberService.UpdateMemberResearchFieldsCommand(
				actorResolver.requireActorUserId(),
				userId,
				request.fieldIds()));
	}

	@PatchMapping("/{userId}/activity-status")
	public AdminMemberDetailResponse updateMemberActivityStatus(
			@PathVariable UUID userId,
			@Valid @RequestBody UpdateMemberActivityStatusRequest request) {
		return adminMemberService.updateMemberActivityStatus(new AdminMemberService.UpdateMemberActivityStatusCommand(
				actorResolver.requireActorUserId(),
				userId,
				request.activityStatus()));
	}

	@GetMapping("/{userId}/projects")
	public List<AdminMemberProjectResponse> getMemberProjects(@PathVariable UUID userId) {
		return adminMemberService.getMemberProjects(actorResolver.requireActorUserId(), userId);
	}

	@GetMapping("/{userId}/evaluations")
	public List<AdminMemberEvaluationResponse> getMemberEvaluations(@PathVariable UUID userId) {
		return adminMemberService.getMemberEvaluations(actorResolver.requireActorUserId(), userId);
	}
}

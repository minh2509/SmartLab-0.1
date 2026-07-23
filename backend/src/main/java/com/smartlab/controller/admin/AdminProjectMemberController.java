package com.smartlab.controller.admin;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.request.admin.AddProjectMemberRequest;
import com.smartlab.dto.request.admin.UpdateProjectMemberRoleRequest;
import com.smartlab.dto.response.admin.AdminProjectMemberResponse;
import com.smartlab.mapper.AdminProjectMemberApiMapper;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminProjectMemberService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/projects/{projectId}/members")
@Profile("!nodb")
public class AdminProjectMemberController {

	private final AdminProjectMemberService service;
	private final AdminProjectMemberApiMapper mapper;
	private final AuthenticatedActorResolver actorResolver;

	public AdminProjectMemberController(
			AdminProjectMemberService service,
			AdminProjectMemberApiMapper mapper,
			AuthenticatedActorResolver actorResolver) {
		this.service = service;
		this.mapper = mapper;
		this.actorResolver = actorResolver;
	}

	@GetMapping
	public List<AdminProjectMemberResponse> getProjectMembers(
			@PathVariable UUID projectId,
			@RequestParam(required = false) String projectRole,
			@RequestParam(required = false) String memberStatus) {
		return mapper.toResponses(service.getProjectMembers(
				actorResolver.requireActorUserId(),
				projectId,
				mapper.toRole(projectRole),
				mapper.toStatus(memberStatus)));
	}

	@PostMapping
	public ResponseEntity<AdminProjectMemberResponse> addMember(
			@PathVariable UUID projectId,
			@Valid @RequestBody AddProjectMemberRequest request) {
		AdminProjectMemberResponse response = mapper.toResponse(service.addMember(
				actorResolver.requireActorUserId(),
				projectId,
				request.userId(),
				mapper.toRole(request.role())));
		return ResponseEntity.created(URI.create(
				"/api/admin/projects/" + projectId + "/members/" + response.userId())).body(response);
	}

	@PatchMapping("/{userId}/role")
	public AdminProjectMemberResponse updateProjectRole(
			@PathVariable UUID projectId,
			@PathVariable UUID userId,
			@Valid @RequestBody UpdateProjectMemberRoleRequest request) {
		return mapper.toResponse(service.updateProjectRole(
				actorResolver.requireActorUserId(),
				projectId,
				userId,
				mapper.toRole(request.role())));
	}

	@PatchMapping("/{userId}/remove")
	public AdminProjectMemberResponse removeMember(
			@PathVariable UUID projectId,
			@PathVariable UUID userId) {
		return mapper.toResponse(service.removeMember(
				actorResolver.requireActorUserId(),
				projectId,
				userId));
	}
}

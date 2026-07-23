package com.smartlab.controller.admin;

import java.net.URI;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.request.admin.ReplaceProjectLeadersRequest;
import com.smartlab.dto.request.admin.ReplaceProjectResearchFieldsRequest;
import com.smartlab.dto.request.admin.SaveAdminProjectRequest;
import com.smartlab.dto.request.admin.UpdateProjectProgressRequest;
import com.smartlab.dto.request.admin.UpdateProjectStatusRequest;
import com.smartlab.dto.response.admin.AdminProjectResponse;
import com.smartlab.dto.response.admin.PageResponse;
import com.smartlab.mapper.AdminProjectApiMapper;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminProjectService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/projects")
@Profile("!nodb")
public class AdminProjectController {

	private final AdminProjectService adminProjectService;
	private final AdminProjectApiMapper mapper;
	private final AuthenticatedActorResolver actorResolver;

	public AdminProjectController(
			AdminProjectService adminProjectService,
			AdminProjectApiMapper mapper,
			AuthenticatedActorResolver actorResolver) {
		this.adminProjectService = adminProjectService;
		this.mapper = mapper;
		this.actorResolver = actorResolver;
	}

	@GetMapping
	public PageResponse<AdminProjectResponse> getProjects(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String visibility,
			@RequestParam(required = false) String field,
			@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return mapper.toPageResponse(adminProjectService.getProjects(
				actorResolver.requireActorUserId(),
				mapper.toFilter(status, type, visibility, field),
				pageable));
	}

	@GetMapping("/{projectId}")
	public AdminProjectResponse getProjectDetail(@PathVariable UUID projectId) {
		return mapper.toResponse(adminProjectService.getProjectDetail(
				actorResolver.requireActorUserId(),
				projectId));
	}

	@PostMapping
	public ResponseEntity<AdminProjectResponse> createProject(
			@Valid @RequestBody SaveAdminProjectRequest request) {
		AdminProjectResponse response = mapper.toResponse(adminProjectService.createProject(
				actorResolver.requireActorUserId(),
				mapper.toCommand(request)));
		return ResponseEntity
				.created(URI.create("/api/admin/projects/" + response.id()))
				.body(response);
	}

	@PutMapping("/{projectId}")
	public AdminProjectResponse updateProject(
			@PathVariable UUID projectId,
			@Valid @RequestBody SaveAdminProjectRequest request) {
		return mapper.toResponse(adminProjectService.updateProject(
				actorResolver.requireActorUserId(),
				projectId,
				mapper.toCommand(request)));
	}

	@PatchMapping("/{projectId}/status")
	public AdminProjectResponse updateStatus(
			@PathVariable UUID projectId,
			@Valid @RequestBody UpdateProjectStatusRequest request) {
		return mapper.toResponse(adminProjectService.updateStatus(
				actorResolver.requireActorUserId(),
				projectId,
				mapper.toTargetStatus(request.status())));
	}

	@PatchMapping("/{projectId}/progress")
	public AdminProjectResponse updateProgress(
			@PathVariable UUID projectId,
			@Valid @RequestBody UpdateProjectProgressRequest request) {
		return mapper.toResponse(adminProjectService.updateProgress(
				actorResolver.requireActorUserId(),
				projectId,
				request.progress()));
	}

	@PutMapping("/{projectId}/research-fields")
	public AdminProjectResponse replaceResearchFields(
			@PathVariable UUID projectId,
			@Valid @RequestBody ReplaceProjectResearchFieldsRequest request) {
		return mapper.toResponse(adminProjectService.replaceResearchFields(
				actorResolver.requireActorUserId(),
				projectId,
				request.fields()));
	}

	@PutMapping("/{projectId}/leaders")
	public AdminProjectResponse replaceLeaders(
			@PathVariable UUID projectId,
			@Valid @RequestBody ReplaceProjectLeadersRequest request) {
		return mapper.toResponse(adminProjectService.replaceLeaders(
				actorResolver.requireActorUserId(),
				projectId,
				request.leaderIds()));
	}

	@DeleteMapping("/{projectId}")
	public ResponseEntity<Void> deleteProject(@PathVariable UUID projectId) {
		adminProjectService.deleteProject(actorResolver.requireActorUserId(), projectId);
		return ResponseEntity.noContent().build();
	}
}

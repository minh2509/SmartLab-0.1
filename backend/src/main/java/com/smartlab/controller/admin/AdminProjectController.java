package com.smartlab.controller.admin;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.response.admin.AdminProjectResponse;
import com.smartlab.dto.response.admin.PageResponse;
import com.smartlab.mapper.AdminProjectApiMapper;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminProjectService;

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
}

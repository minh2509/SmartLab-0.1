package com.smartlab.controller.admin;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.response.admin.AdminProjectOptionResponse;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminProjectLookupService;

@RestController
@RequestMapping("/api/admin/projects")
@Profile("!nodb")
public class AdminProjectLookupController {

	private final AdminProjectLookupService projectLookupService;
	private final AuthenticatedActorResolver actorResolver;

	public AdminProjectLookupController(
			AdminProjectLookupService projectLookupService,
			AuthenticatedActorResolver actorResolver) {
		this.projectLookupService = projectLookupService;
		this.actorResolver = actorResolver;
	}

	@GetMapping("/options")
	public List<AdminProjectOptionResponse> getProjectOptions() {
		return projectLookupService.listProjectOptions(actorResolver.requireActorUserId())
				.stream()
				.map(project -> new AdminProjectOptionResponse(
						project.id(),
						project.code(),
						project.name(),
						project.slug(),
						project.status(),
						project.activeRecipientCount()))
				.toList();
	}
}

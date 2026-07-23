package com.smartlab.controller.admin;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.request.admin.CreateResearchFieldRequest;
import com.smartlab.dto.request.admin.UpdateLabImageRequest;
import com.smartlab.dto.request.admin.UpdateLabInfoRequest;
import com.smartlab.dto.request.admin.UpdateResearchFieldRequest;
import com.smartlab.dto.request.admin.UpdateResearchFieldStatusRequest;
import com.smartlab.dto.response.admin.AdminLabResponse;
import com.smartlab.dto.response.admin.AdminResearchFieldListResponse;
import com.smartlab.dto.response.admin.AdminResearchFieldResponse;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminLabService;
import com.smartlab.service.admin.AdminResearchFieldService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@Profile("!nodb")
@Validated
public class AdminLabController {

	private final AdminLabService adminLabService;
	private final AdminResearchFieldService adminResearchFieldService;
	private final AuthenticatedActorResolver actorResolver;

	public AdminLabController(
			AdminLabService adminLabService,
			AdminResearchFieldService adminResearchFieldService,
			AuthenticatedActorResolver actorResolver) {
		this.adminLabService = adminLabService;
		this.adminResearchFieldService = adminResearchFieldService;
		this.actorResolver = actorResolver;
	}

	@GetMapping("/lab")
	public AdminLabResponse getLabInfo() {
		return adminLabService.getLabInfo(actorResolver.requireActorUserId());
	}

	@PutMapping("/lab")
	public AdminLabResponse updateLabInfo(@Valid @RequestBody UpdateLabInfoRequest request) {
		return adminLabService.updateLabInfo(new AdminLabService.UpdateLabInfoCommand(
				actorResolver.requireActorUserId(),
				request.getName(),
				request.getDescription(),
				request.getMission(),
				request.getVision(),
				request.getContactEmail(),
				request.getWebsiteUrl()));
	}

	@PostMapping("/lab/logo")
	public AdminLabResponse updateLogo(@Valid @RequestBody UpdateLabImageRequest request) {
		return adminLabService.updateLogo(new AdminLabService.UpdateLabImageCommand(
				actorResolver.requireActorUserId(),
				request.fileId()));
	}

	@PostMapping("/lab/cover")
	public AdminLabResponse updateCover(@Valid @RequestBody UpdateLabImageRequest request) {
		return adminLabService.updateCover(new AdminLabService.UpdateLabImageCommand(
				actorResolver.requireActorUserId(),
				request.fileId()));
	}

	@GetMapping("/research-fields")
	public AdminResearchFieldListResponse getResearchFields() {
		return adminResearchFieldService.getResearchFields(actorResolver.requireActorUserId());
	}

	@PostMapping("/research-fields")
	@ResponseStatus(HttpStatus.CREATED)
	public AdminResearchFieldResponse createResearchField(@Valid @RequestBody CreateResearchFieldRequest request) {
		return adminResearchFieldService.createField(new AdminResearchFieldService.CreateResearchFieldCommand(
				actorResolver.requireActorUserId(),
				request.getCode(),
				request.getName(),
				request.getDescription()));
	}

	@PutMapping("/research-fields/{fieldId}")
	public AdminResearchFieldResponse updateResearchField(
			@PathVariable UUID fieldId,
			@Valid @RequestBody UpdateResearchFieldRequest request) {
		return adminResearchFieldService.updateField(new AdminResearchFieldService.UpdateResearchFieldCommand(
				actorResolver.requireActorUserId(),
				fieldId,
				request.getName(),
				request.getDescription()));
	}

	@PatchMapping("/research-fields/{fieldId}/status")
	public AdminResearchFieldResponse updateResearchFieldStatus(
			@PathVariable UUID fieldId,
			@Valid @RequestBody UpdateResearchFieldStatusRequest request) {
		return adminResearchFieldService.updateFieldStatus(new AdminResearchFieldService.UpdateResearchFieldStatusCommand(
				actorResolver.requireActorUserId(),
				fieldId,
				request.status()));
	}
}

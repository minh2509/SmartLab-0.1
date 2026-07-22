package com.smartlab.controller.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.request.admin.ReplaceUserRolesRequest;
import com.smartlab.dto.response.admin.AdminRoleResponse;
import com.smartlab.dto.response.admin.AdminUserResponse;
import com.smartlab.dto.response.admin.AdminUserRoleResponse;
import com.smartlab.mapper.AdminUserApiMapper;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminUserRoleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users/{userId}/roles")
@Profile("!nodb")
public class AdminUserRoleController {

	private final AdminUserRoleService adminUserRoleService;
	private final AdminUserApiMapper mapper;
	private final AuthenticatedActorResolver actorResolver;

	public AdminUserRoleController(
			AdminUserRoleService adminUserRoleService,
			AdminUserApiMapper mapper,
			AuthenticatedActorResolver actorResolver) {
		this.adminUserRoleService = adminUserRoleService;
		this.mapper = mapper;
		this.actorResolver = actorResolver;
	}

	@GetMapping
	public List<AdminRoleResponse> listActiveRolesForUser(@PathVariable UUID userId) {
		return adminUserRoleService.listActiveRolesForUser(actorResolver.requireActorUserId(), userId)
				.stream()
				.map(mapper::toRoleResponse)
				.toList();
	}

	@PutMapping
	public AdminUserResponse replaceRoles(
			@PathVariable UUID userId,
			@Valid @RequestBody ReplaceUserRolesRequest request) {
		return mapper.toUserResponse(adminUserRoleService.replaceRolesForUser(
				new AdminUserRoleService.ReplaceUserRolesCommand(
						actorResolver.requireActorUserId(),
						userId,
						request.roleCodes())));
	}

	@PutMapping("/{roleId}")
	public AdminUserRoleResponse assignRole(
			@PathVariable UUID userId,
			@PathVariable UUID roleId) {
		return mapper.toUserRoleResponse(adminUserRoleService.assignRoleToUser(
				new AdminUserRoleService.AssignUserRoleCommand(actorResolver.requireActorUserId(), userId, roleId)));
	}

	@DeleteMapping("/{roleId}")
	public ResponseEntity<Void> revokeRole(
			@PathVariable UUID userId,
			@PathVariable UUID roleId) {
		adminUserRoleService.revokeRoleFromUser(
				new AdminUserRoleService.RevokeUserRoleCommand(actorResolver.requireActorUserId(), userId, roleId));
		return ResponseEntity.noContent().build();
	}
}

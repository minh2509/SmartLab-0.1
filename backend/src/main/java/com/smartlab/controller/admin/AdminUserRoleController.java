package com.smartlab.controller.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.response.admin.AdminRoleResponse;
import com.smartlab.dto.response.admin.AdminUserRoleResponse;
import com.smartlab.mapper.AdminUserApiMapper;
import com.smartlab.service.admin.AdminUserRoleService;

@RestController
@RequestMapping("/api/admin/users/{userId}/roles")
@Profile("!nodb")
public class AdminUserRoleController {

	private final AdminUserRoleService adminUserRoleService;
	private final AdminUserApiMapper mapper;

	public AdminUserRoleController(AdminUserRoleService adminUserRoleService, AdminUserApiMapper mapper) {
		this.adminUserRoleService = adminUserRoleService;
		this.mapper = mapper;
	}

	@GetMapping
	public List<AdminRoleResponse> listActiveRolesForUser(@PathVariable UUID userId) {
		return adminUserRoleService.listActiveRolesForUser(userId)
				.stream()
				.map(mapper::toRoleResponse)
				.toList();
	}

	@PutMapping("/{roleId}")
	public AdminUserRoleResponse assignRole(
			@PathVariable UUID userId,
			@PathVariable UUID roleId) {
		return mapper.toUserRoleResponse(adminUserRoleService.assignRoleToUser(
				new AdminUserRoleService.AssignUserRoleCommand(userId, roleId, null)));
	}

	@DeleteMapping("/{roleId}")
	public ResponseEntity<Void> revokeRole(
			@PathVariable UUID userId,
			@PathVariable UUID roleId) {
		adminUserRoleService.revokeRoleFromUser(new AdminUserRoleService.RevokeUserRoleCommand(userId, roleId));
		return ResponseEntity.noContent().build();
	}
}

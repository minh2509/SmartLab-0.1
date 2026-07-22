package com.smartlab.controller.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.response.admin.AdminPermissionResponse;
import com.smartlab.dto.response.admin.AdminSystemRoleResponse;
import com.smartlab.mapper.AdminRoleCatalogApiMapper;
import com.smartlab.service.admin.AdminRoleCatalogService;

@RestController
@RequestMapping("/api/admin/roles")
@Profile("!nodb")
public class AdminRoleController {

	private final AdminRoleCatalogService adminRoleCatalogService;
	private final AdminRoleCatalogApiMapper mapper;

	public AdminRoleController(AdminRoleCatalogService adminRoleCatalogService, AdminRoleCatalogApiMapper mapper) {
		this.adminRoleCatalogService = adminRoleCatalogService;
		this.mapper = mapper;
	}

	@GetMapping
	public List<AdminSystemRoleResponse> listRoles() {
		return adminRoleCatalogService.listRoles()
				.stream()
				.map(mapper::toRoleResponse)
				.toList();
	}

	@GetMapping("/{roleId}/permissions")
	public List<AdminPermissionResponse> listPermissionsForRole(@PathVariable UUID roleId) {
		return adminRoleCatalogService.listPermissionsForRole(roleId)
				.stream()
				.map(mapper::toPermissionResponse)
				.toList();
	}
}

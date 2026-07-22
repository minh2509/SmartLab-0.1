package com.smartlab.mapper;

import org.springframework.stereotype.Component;

import com.smartlab.dto.response.admin.AdminPermissionResponse;
import com.smartlab.dto.response.admin.AdminSystemRoleResponse;
import com.smartlab.service.admin.AdminRoleCatalogService;

@Component
public class AdminRoleCatalogApiMapper {

	public AdminSystemRoleResponse toRoleResponse(AdminRoleCatalogService.RoleSummary summary) {
		return new AdminSystemRoleResponse(
				summary.id(),
				summary.code(),
				summary.name(),
				summary.description());
	}

	public AdminPermissionResponse toPermissionResponse(AdminRoleCatalogService.PermissionSummary summary) {
		return new AdminPermissionResponse(
				summary.id(),
				summary.code(),
				summary.name(),
				summary.module(),
				summary.description());
	}
}

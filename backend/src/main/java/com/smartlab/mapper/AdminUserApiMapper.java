package com.smartlab.mapper;

import org.springframework.stereotype.Component;

import com.smartlab.dto.response.admin.AdminRoleCatalogResponse;
import com.smartlab.dto.response.admin.AdminRoleResponse;
import com.smartlab.dto.response.admin.AdminUserCredentialResponse;
import com.smartlab.dto.response.admin.AdminUserResponse;
import com.smartlab.dto.response.admin.AdminUserRoleResponse;
import com.smartlab.service.admin.AdminUserRoleService;
import com.smartlab.service.admin.AdminUserService;

@Component
public class AdminUserApiMapper {

	public AdminUserResponse toUserResponse(AdminUserService.ManagedUserSummary summary) {
		return new AdminUserResponse(
				summary.id(),
				summary.labId(),
				summary.username(),
				summary.email(),
				summary.fullName(),
				summary.avatarFileId(),
				summary.accountStatus(),
				summary.lastLoginAt(),
				summary.roleCodes());
	}

	public AdminUserCredentialResponse toCredentialResponse(AdminUserService.ManagedUserSummary summary) {
		return new AdminUserCredentialResponse(
				toUserResponse(summary),
				summary.temporaryPassword(),
				summary.temporaryPasswordGenerated());
	}

	public AdminUserRoleResponse toUserRoleResponse(AdminUserRoleService.AssignedRoleSummary summary) {
		return new AdminUserRoleResponse(
				summary.assignmentId(),
				summary.roleId(),
				summary.roleCode(),
				summary.roleName(),
				summary.status());
	}

	public AdminRoleResponse toRoleResponse(AdminUserRoleService.AssignedRoleSummary summary) {
		return new AdminRoleResponse(
				summary.assignmentId(),
				summary.roleId(),
				summary.roleCode(),
				summary.roleName(),
				summary.status());
	}

	public AdminRoleCatalogResponse toRoleCatalogResponse(AdminUserRoleService.RoleCatalogSummary summary) {
		return new AdminRoleCatalogResponse(
				summary.roleId(),
				summary.code(),
				summary.name(),
				summary.assignable());
	}
}

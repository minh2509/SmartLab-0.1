package com.smartlab.dto.response.admin;

import java.util.UUID;

import com.smartlab.enums.UserRoleStatus;

public record AdminUserRoleResponse(
		UUID assignmentId,
		UUID roleId,
		String roleCode,
		String roleName,
		UserRoleStatus status) {
}

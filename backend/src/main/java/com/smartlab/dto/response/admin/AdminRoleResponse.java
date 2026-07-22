package com.smartlab.dto.response.admin;

import java.util.UUID;

import com.smartlab.enums.UserRoleStatus;

public record AdminRoleResponse(
		UUID assignmentId,
		UUID roleId,
		String code,
		String name,
		UserRoleStatus status) {
}

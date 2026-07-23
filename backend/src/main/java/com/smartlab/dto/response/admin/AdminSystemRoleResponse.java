package com.smartlab.dto.response.admin;

import java.util.UUID;

public record AdminSystemRoleResponse(
		UUID id,
		String code,
		String name,
		String description) {
}

package com.smartlab.dto.response.admin;

import java.util.UUID;

public record AdminPermissionResponse(
		UUID id,
		String code,
		String name,
		String module,
		String description) {
}

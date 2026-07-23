package com.smartlab.dto.response.admin;

import java.util.UUID;

public record AdminRoleCatalogResponse(
		UUID roleId,
		String code,
		String name,
		boolean assignable) {
}

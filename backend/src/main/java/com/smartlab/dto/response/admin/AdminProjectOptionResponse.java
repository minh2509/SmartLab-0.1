package com.smartlab.dto.response.admin;

import java.util.UUID;

import com.smartlab.enums.ProjectStatus;

public record AdminProjectOptionResponse(
		UUID id,
		String code,
		String name,
		String slug,
		ProjectStatus status,
		long activeRecipientCount) {
}

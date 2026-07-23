package com.smartlab.dto.response.admin;

import java.util.UUID;

import com.smartlab.enums.LabStatus;

public record AdminLabResponse(
		UUID id,
		String name,
		String code,
		String description,
		String mission,
		String vision,
		String contactEmail,
		String websiteUrl,
		UUID logoFileId,
		UUID coverFileId,
		LabStatus status) {
}

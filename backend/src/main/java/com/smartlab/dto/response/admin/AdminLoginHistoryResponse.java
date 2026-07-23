package com.smartlab.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminLoginHistoryResponse(
		UUID id,
		UUID userId,
		String userEmail,
		String userFullName,
		OffsetDateTime loginAt,
		String ipAddress,
		String userAgent,
		Boolean success,
		String failureReason) {
}

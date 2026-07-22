package com.smartlab.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminAuditLogResponse(
		UUID id,
		UUID labId,
		UUID actorId,
		String actorEmail,
		String actorFullName,
		String action,
		String entityType,
		UUID entityId,
		String ipAddress,
		String userAgent,
		OffsetDateTime createdAt) {
}

package com.smartlab.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminProjectMemberResponse(
		UUID membershipId,
		UUID userId,
		String fullName,
		String email,
		String projectRole,
		String memberStatus,
		OffsetDateTime joinedAt,
		OffsetDateTime leftAt) {
}

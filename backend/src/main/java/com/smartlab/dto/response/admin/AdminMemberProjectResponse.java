package com.smartlab.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.smartlab.enums.ProjectMemberRole;
import com.smartlab.enums.ProjectMemberStatus;

public record AdminMemberProjectResponse(
		UUID projectId,
		String projectName,
		String projectCode,
		ProjectMemberRole role,
		ProjectMemberStatus memberStatus,
		OffsetDateTime joinedAt) {
}

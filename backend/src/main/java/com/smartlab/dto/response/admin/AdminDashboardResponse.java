package com.smartlab.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminDashboardResponse(
		long users,
		long projects,
		long posts,
		long joinRequests,
		long tasks,
		List<RecentActivityResponse> recentActivities) {

	public AdminDashboardResponse {
		recentActivities = List.copyOf(recentActivities);
	}

	public record RecentActivityResponse(
			UUID id,
			String action,
			String entityType,
			UUID entityId,
			UUID actorId,
			String actorName,
			OffsetDateTime createdAt) {
	}
}

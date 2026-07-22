package com.smartlab.service.admin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.AuditLog;
import com.smartlab.entity.User;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.repository.admin.AdminDashboardReadRepository;

@Service
@Profile("!nodb")
public class AdminDashboardService {

	private static final int MAX_ACTIVITY_LIMIT = 50;

	private final AdminDashboardReadRepository dashboardReadRepository;

	public AdminDashboardService(AdminDashboardReadRepository dashboardReadRepository) {
		this.dashboardReadRepository = dashboardReadRepository;
	}

	@Transactional(readOnly = true)
	public DashboardSummary getDashboardSummary(UUID labId, int recentActivityLimit) {
		if (labId == null) {
			throw new InvalidAdminServiceInputException("Lab ID must not be null.");
		}
		if (recentActivityLimit < 1 || recentActivityLimit > MAX_ACTIVITY_LIMIT) {
			throw new InvalidAdminServiceInputException("Recent activity limit must be between 1 and 50.");
		}

		AdminDashboardReadRepository.DashboardCounts counts = dashboardReadRepository.countVisibleRecords(labId);
		List<RecentActivity> activities = dashboardReadRepository
				.findRecentActivities(labId, recentActivityLimit)
				.stream()
				.map(AdminDashboardService::toRecentActivity)
				.toList();
		return new DashboardSummary(
				counts.users(),
				counts.projects(),
				counts.posts(),
				counts.joinRequests(),
				counts.tasks(),
				activities);
	}

	private static RecentActivity toRecentActivity(AuditLog auditLog) {
		User actor = auditLog.getActor();
		return new RecentActivity(
				auditLog.getId(),
				auditLog.getAction(),
				auditLog.getEntityType(),
				auditLog.getEntityId(),
				actor == null ? null : actor.getId(),
				actor == null ? null : actor.getFullName(),
				auditLog.getCreatedAt());
	}

	public record DashboardSummary(
			long users,
			long projects,
			long posts,
			long joinRequests,
			long tasks,
			List<RecentActivity> recentActivities) {

		public DashboardSummary {
			recentActivities = List.copyOf(recentActivities);
		}
	}

	public record RecentActivity(
			UUID id,
			String action,
			String entityType,
			UUID entityId,
			UUID actorId,
			String actorName,
			OffsetDateTime createdAt) {
	}
}

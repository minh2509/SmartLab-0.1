package com.smartlab.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.smartlab.entity.AuditLog;
import com.smartlab.entity.User;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.repository.admin.AdminDashboardReadRepository;

class AdminDashboardServiceTests {

	private final AdminDashboardReadRepository readRepository = mock(AdminDashboardReadRepository.class);
	private final AdminDashboardService service = new AdminDashboardService(readRepository);

	@Test
	void dashboardReturnsLabScopedCountsAndBoundedRecentActivities() {
		UUID labId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		User actor = new User();
		actor.setId(actorId);
		actor.setFullName("Admin User");
		AuditLog auditLog = new AuditLog();
		auditLog.setId(UUID.randomUUID());
		auditLog.setActor(actor);
		auditLog.setAction("CREATE_NOTIFICATION");
		auditLog.setEntityType("NOTIFICATION");
		auditLog.setEntityId(UUID.randomUUID());
		auditLog.setCreatedAt(OffsetDateTime.parse("2026-07-23T00:00:00Z"));
		when(readRepository.countVisibleRecords(labId))
				.thenReturn(new AdminDashboardReadRepository.DashboardCounts(10, 4, 8, 3, 12));
		when(readRepository.findRecentActivities(labId, 5)).thenReturn(List.of(auditLog));

		AdminDashboardService.DashboardSummary result = service.getDashboardSummary(labId, 5);

		assertEquals(10, result.users());
		assertEquals(4, result.projects());
		assertEquals(8, result.posts());
		assertEquals(3, result.joinRequests());
		assertEquals(12, result.tasks());
		assertEquals(actorId, result.recentActivities().getFirst().actorId());
		assertEquals("Admin User", result.recentActivities().getFirst().actorName());
		verify(readRepository).findRecentActivities(labId, 5);
	}

	@Test
	void dashboardRejectsMissingLabAndUnboundedActivityLimit() {
		assertThrows(InvalidAdminServiceInputException.class, () -> service.getDashboardSummary(null, 10));
		assertThrows(InvalidAdminServiceInputException.class, () -> service.getDashboardSummary(UUID.randomUUID(), 0));
		assertThrows(InvalidAdminServiceInputException.class, () -> service.getDashboardSummary(UUID.randomUUID(), 51));
	}
}

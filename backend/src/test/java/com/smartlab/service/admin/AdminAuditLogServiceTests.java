package com.smartlab.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.AuditLog;
import com.smartlab.entity.Lab;
import com.smartlab.entity.LoginHistory;
import com.smartlab.entity.User;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.AuditLogRepository;
import com.smartlab.repository.LoginHistoryRepository;
import com.smartlab.repository.UserRepository;

class AdminAuditLogServiceTests {

	private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
	private final LoginHistoryRepository loginHistoryRepository = mock(LoginHistoryRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final AdminAuditLogService service = new AdminAuditLogService(
			auditLogRepository,
			loginHistoryRepository,
			userRepository);

	@Test
	void listAuditLogsUsesTrimmedFiltersDescendingPageableAndDtoSafeSummary() {
		UUID labId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID entityId = UUID.randomUUID();
		AuditLog auditLog = auditLog(labId, actorId, entityId);
		when(auditLogRepository.findAll(
				any(Specification.class),
				any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(auditLog), PageRequest.of(2, 5), 6));

		Page<AdminAuditLogService.AuditLogSummary> result = service.listAuditLogs(
				new AdminAuditLogService.AuditLogFilter(
						" CREATE_USER ",
						actorId,
						" USER ",
						entityId,
						OffsetDateTime.parse("2026-07-22T00:00:00Z"),
						OffsetDateTime.parse("2026-07-23T00:00:00Z"),
						2,
						5));

		assertEquals(1, result.getContent().size());
		assertEquals("admin@example.edu", result.getContent().getFirst().actorEmail());
		assertEquals(labId, result.getContent().getFirst().labId());
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(auditLogRepository).findAll(
				any(Specification.class),
				pageableCaptor.capture());
		assertEquals(2, pageableCaptor.getValue().getPageNumber());
		assertEquals(5, pageableCaptor.getValue().getPageSize());
		assertEquals(Sort.Direction.DESC, pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection());
	}

	@Test
	void listLoginHistoriesForUserChecksUserAndAppliesPathUserFilter() {
		UUID userId = UUID.randomUUID();
		LoginHistory loginHistory = loginHistory(userId);
		when(userRepository.existsById(userId)).thenReturn(true);
		when(loginHistoryRepository.findAll(
				any(Specification.class),
				any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(loginHistory)));

		Page<AdminAuditLogService.LoginHistorySummary> result = service.listLoginHistoriesForUser(
				userId,
				new AdminAuditLogService.LoginHistoryFilter(
						null,
						false,
						" 192.0.2.10 ",
						null,
						null,
						0,
						20));

		assertEquals(userId, result.getContent().getFirst().userId());
		verify(userRepository).existsById(userId);
		verify(loginHistoryRepository).findAll(
				any(Specification.class),
				any(Pageable.class));
	}

	@Test
	void listLoginHistoriesForUserRejectsMissingUserBeforeQueryingHistory() {
		UUID userId = UUID.randomUUID();
		when(userRepository.existsById(userId)).thenReturn(false);

		assertThrows(
				ResourceNotFoundException.class,
				() -> service.listLoginHistoriesForUser(userId, AdminAuditLogService.LoginHistoryFilter.empty()));

		verify(loginHistoryRepository, never()).findAll(any(Specification.class), any(Pageable.class));
	}

	@Test
	void listMethodsRejectInvalidPaginationAndDateRange() {
		OffsetDateTime start = OffsetDateTime.parse("2026-07-24T00:00:00Z");
		OffsetDateTime end = OffsetDateTime.parse("2026-07-23T00:00:00Z");

		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.listAuditLogs(new AdminAuditLogService.AuditLogFilter(
						null,
						null,
						null,
						null,
						start,
						end,
						0,
						20)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.listLoginHistories(new AdminAuditLogService.LoginHistoryFilter(
						null,
						null,
						null,
						null,
						null,
						-1,
						20)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.listLoginHistories(new AdminAuditLogService.LoginHistoryFilter(
						null,
						null,
						null,
						null,
						null,
						0,
						101)));
	}

	@Test
	void serviceStructureUsesReadOnlyTransactions() throws NoSuchMethodException {
		assertTrue(AdminAuditLogService.class.isAnnotationPresent(Service.class));
		assertReadOnly("listAuditLogs", AdminAuditLogService.AuditLogFilter.class);
		assertReadOnly("listLoginHistories", AdminAuditLogService.LoginHistoryFilter.class);
		assertReadOnly("listLoginHistoriesForUser", UUID.class, AdminAuditLogService.LoginHistoryFilter.class);
	}

	private static void assertReadOnly(String methodName, Class<?>... argTypes) throws NoSuchMethodException {
		Transactional transactional = AdminAuditLogService.class.getMethod(methodName, argTypes)
				.getAnnotation(Transactional.class);
		assertEquals(true, transactional.readOnly());
	}

	private static AuditLog auditLog(UUID labId, UUID actorId, UUID entityId) {
		Lab lab = new Lab();
		lab.setId(labId);
		User actor = user(actorId);
		AuditLog auditLog = new AuditLog();
		auditLog.setId(UUID.randomUUID());
		auditLog.setLab(lab);
		auditLog.setActor(actor);
		auditLog.setAction("CREATE_USER");
		auditLog.setEntityType("USER");
		auditLog.setEntityId(entityId);
		auditLog.setOldValue("{\"password_hash\":\"secret\"}");
		auditLog.setNewValue("{\"password_hash\":\"secret\"}");
		auditLog.setIpAddress("127.0.0.1");
		auditLog.setUserAgent("JUnit");
		auditLog.setCreatedAt(OffsetDateTime.parse("2026-07-23T01:00:00Z"));
		return auditLog;
	}

	private static LoginHistory loginHistory(UUID userId) {
		LoginHistory loginHistory = new LoginHistory();
		loginHistory.setId(UUID.randomUUID());
		loginHistory.setUser(user(userId));
		loginHistory.setLoginAt(OffsetDateTime.parse("2026-07-23T02:00:00Z"));
		loginHistory.setIpAddress("192.0.2.10");
		loginHistory.setUserAgent("JUnit");
		loginHistory.setSuccess(false);
		loginHistory.setFailureReason("Bad credentials");
		return loginHistory;
	}

	private static User user(UUID id) {
		User user = new User();
		user.setId(id);
		user.setEmail("admin@example.edu");
		user.setFullName("Admin User");
		return user;
	}
}

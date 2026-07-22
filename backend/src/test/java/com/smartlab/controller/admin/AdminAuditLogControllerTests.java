package com.smartlab.controller.admin;

import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smartlab.exception.ApiExceptionHandler;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.mapper.AdminAuditLogApiMapper;
import com.smartlab.service.admin.AdminAuditLogService;

class AdminAuditLogControllerTests {

	private final AdminAuditLogService adminAuditLogService = mock(AdminAuditLogService.class);
	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new AdminAuditLogController(adminAuditLogService, new AdminAuditLogApiMapper()))
			.setControllerAdvice(new ApiExceptionHandler())
			.build();

	@Test
	void listAuditLogsMapsFiltersToServiceAndDoesNotExposeJsonPayloadValues() throws Exception {
		UUID auditId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID entityId = UUID.randomUUID();
		when(adminAuditLogService.listAuditLogs(any(AdminAuditLogService.AuditLogFilter.class)))
				.thenReturn(new PageImpl<>(List.of(new AdminAuditLogService.AuditLogSummary(
						auditId,
						UUID.randomUUID(),
						actorId,
						"admin@example.edu",
						"Admin User",
						"CREATE_USER",
						"USER",
						entityId,
						"127.0.0.1",
						"JUnit",
						OffsetDateTime.parse("2026-07-23T01:00:00Z")))));

		mockMvc.perform(get("/api/admin/audit-logs")
						.param("action", "CREATE_USER")
						.param("actorId", actorId.toString())
						.param("entityType", "USER")
						.param("entityId", entityId.toString())
						.param("start", "2026-07-22T00:00:00Z")
						.param("end", "2026-07-23T23:59:59Z")
						.param("page", "1")
						.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(auditId.toString()))
				.andExpect(jsonPath("$.content[0].actorEmail").value("admin@example.edu"))
				.andExpect(jsonPath("$.content[0].action").value("CREATE_USER"))
				.andExpect(jsonPath("$.content[0].entityId").value(entityId.toString()))
				.andExpect(jsonPath("$.content[0].oldValue").doesNotExist())
				.andExpect(jsonPath("$.content[0].newValue").doesNotExist())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(content().string(not(Matchers.containsString("password_hash"))));

		ArgumentCaptor<AdminAuditLogService.AuditLogFilter> captor =
				ArgumentCaptor.forClass(AdminAuditLogService.AuditLogFilter.class);
		verify(adminAuditLogService).listAuditLogs(captor.capture());
		AdminAuditLogService.AuditLogFilter filter = captor.getValue();
		org.junit.jupiter.api.Assertions.assertEquals(actorId, filter.actorId());
		org.junit.jupiter.api.Assertions.assertEquals(entityId, filter.entityId());
		org.junit.jupiter.api.Assertions.assertEquals(1, filter.page());
		org.junit.jupiter.api.Assertions.assertEquals(10, filter.size());
	}

	@Test
	void listLoginHistoriesReturnsPagedDtoWithoutUserEntityOrPasswordFields() throws Exception {
		UUID historyId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(adminAuditLogService.listLoginHistories(any(AdminAuditLogService.LoginHistoryFilter.class)))
				.thenReturn(new PageImpl<>(List.of(new AdminAuditLogService.LoginHistorySummary(
						historyId,
						userId,
						"member@example.edu",
						"Member User",
						OffsetDateTime.parse("2026-07-23T02:00:00Z"),
						"192.0.2.10",
						"JUnit",
						true,
						null))));

		mockMvc.perform(get("/api/admin/login-histories")
						.param("userId", userId.toString())
						.param("success", "true")
						.param("ipAddress", "192.0.2.10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(historyId.toString()))
				.andExpect(jsonPath("$.content[0].userId").value(userId.toString()))
				.andExpect(jsonPath("$.content[0].userEmail").value("member@example.edu"))
				.andExpect(jsonPath("$.content[0].success").value(true))
				.andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
	}

	@Test
	void listLoginHistoriesForUserMapsMissingUserToNotFound() throws Exception {
		UUID userId = UUID.randomUUID();
		when(adminAuditLogService.listLoginHistoriesForUser(
				org.mockito.ArgumentMatchers.eq(userId),
				any(AdminAuditLogService.LoginHistoryFilter.class)))
				.thenThrow(new ResourceNotFoundException("User was not found."));

		mockMvc.perform(get("/api/admin/users/{userId}/login-histories", userId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("User was not found."));
	}
}

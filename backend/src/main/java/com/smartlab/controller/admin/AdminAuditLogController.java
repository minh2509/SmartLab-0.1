package com.smartlab.controller.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.response.admin.AdminAuditLogResponse;
import com.smartlab.dto.response.admin.AdminLoginHistoryResponse;
import com.smartlab.dto.response.admin.AdminPageResponse;
import com.smartlab.mapper.AdminAuditLogApiMapper;
import com.smartlab.service.admin.AdminAuditLogService;

@RestController
@RequestMapping("/api/admin")
@Profile("!nodb")
public class AdminAuditLogController {

	private final AdminAuditLogService adminAuditLogService;
	private final AdminAuditLogApiMapper mapper;

	public AdminAuditLogController(AdminAuditLogService adminAuditLogService, AdminAuditLogApiMapper mapper) {
		this.adminAuditLogService = adminAuditLogService;
		this.mapper = mapper;
	}

	@GetMapping("/audit-logs")
	public AdminPageResponse<AdminAuditLogResponse> listAuditLogs(
			@RequestParam(required = false) String action,
			@RequestParam(required = false) UUID actorId,
			@RequestParam(required = false) String entityType,
			@RequestParam(required = false) UUID entityId,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime start,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime end,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return mapper.toAuditLogPage(adminAuditLogService.listAuditLogs(
				new AdminAuditLogService.AuditLogFilter(
						action,
						actorId,
						entityType,
						entityId,
						start,
						end,
						page,
						size)));
	}

	@GetMapping("/login-histories")
	public AdminPageResponse<AdminLoginHistoryResponse> listLoginHistories(
			@RequestParam(required = false) UUID userId,
			@RequestParam(required = false) Boolean success,
			@RequestParam(required = false) String ipAddress,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime start,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime end,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return mapper.toLoginHistoryPage(adminAuditLogService.listLoginHistories(
				new AdminAuditLogService.LoginHistoryFilter(
						userId,
						success,
						ipAddress,
						start,
						end,
						page,
						size)));
	}

	@GetMapping("/users/{userId}/login-histories")
	public AdminPageResponse<AdminLoginHistoryResponse> listLoginHistoriesForUser(
			@PathVariable UUID userId,
			@RequestParam(required = false) Boolean success,
			@RequestParam(required = false) String ipAddress,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime start,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime end,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return mapper.toLoginHistoryPage(adminAuditLogService.listLoginHistoriesForUser(
				userId,
				new AdminAuditLogService.LoginHistoryFilter(
						null,
						success,
						ipAddress,
						start,
						end,
						page,
						size)));
	}
}

package com.smartlab.mapper;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.smartlab.dto.response.admin.AdminAuditLogResponse;
import com.smartlab.dto.response.admin.AdminLoginHistoryResponse;
import com.smartlab.dto.response.admin.AdminPageResponse;
import com.smartlab.service.admin.AdminAuditLogService;

@Component
public class AdminAuditLogApiMapper {

	public AdminPageResponse<AdminAuditLogResponse> toAuditLogPage(
			Page<AdminAuditLogService.AuditLogSummary> page) {
		return AdminPageResponse.from(page.map(this::toAuditLogResponse));
	}

	public AdminPageResponse<AdminLoginHistoryResponse> toLoginHistoryPage(
			Page<AdminAuditLogService.LoginHistorySummary> page) {
		return AdminPageResponse.from(page.map(this::toLoginHistoryResponse));
	}

	private AdminAuditLogResponse toAuditLogResponse(AdminAuditLogService.AuditLogSummary summary) {
		return new AdminAuditLogResponse(
				summary.id(),
				summary.labId(),
				summary.actorId(),
				summary.actorEmail(),
				summary.actorFullName(),
				summary.action(),
				summary.entityType(),
				summary.entityId(),
				summary.ipAddress(),
				summary.userAgent(),
				summary.createdAt());
	}

	private AdminLoginHistoryResponse toLoginHistoryResponse(AdminAuditLogService.LoginHistorySummary summary) {
		return new AdminLoginHistoryResponse(
				summary.id(),
				summary.userId(),
				summary.userEmail(),
				summary.userFullName(),
				summary.loginAt(),
				summary.ipAddress(),
				summary.userAgent(),
				summary.success(),
				summary.failureReason());
	}
}

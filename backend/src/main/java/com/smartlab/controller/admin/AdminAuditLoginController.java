package com.smartlab.controller.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.response.admin.AdminAuditLogResponse;
import com.smartlab.dto.response.admin.AdminLoginHistoryResponse;
import com.smartlab.entity.AuditLog;
import com.smartlab.entity.LoginHistory;
import com.smartlab.entity.User;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminAuditLoginService;

@RestController
@RequestMapping("/api/admin")
@Profile("!nodb")
public class AdminAuditLoginController {

	private final AdminAuditLoginService service;
	private final AuthenticatedActorResolver actorResolver;

	public AdminAuditLoginController(AdminAuditLoginService service, AuthenticatedActorResolver actorResolver) {
		this.service = service;
		this.actorResolver = actorResolver;
	}

	@GetMapping("/audit-logs")
	@Transactional(readOnly = true)
	public List<AdminAuditLogResponse> listAuditLogs() {
		return service.listAuditLogs(actorResolver.requireActorUserId())
				.stream()
				.map(AdminAuditLoginController::toAuditLogResponse)
				.toList();
	}

	@GetMapping("/login-histories")
	@Transactional(readOnly = true)
	public List<AdminLoginHistoryResponse> listLoginHistories() {
		return service.listLoginHistories(actorResolver.requireActorUserId())
				.stream()
				.map(AdminAuditLoginController::toLoginHistoryResponse)
				.toList();
	}

	@GetMapping("/users/{userId}/login-histories")
	@Transactional(readOnly = true)
	public List<AdminLoginHistoryResponse> listLoginHistoriesForUser(@PathVariable UUID userId) {
		return service.listLoginHistoriesForUser(actorResolver.requireActorUserId(), userId)
				.stream()
				.map(AdminAuditLoginController::toLoginHistoryResponse)
				.toList();
	}

	private static AdminAuditLogResponse toAuditLogResponse(AuditLog auditLog) {
		User actor = auditLog.getActor();
		return new AdminAuditLogResponse(
				auditLog.getId(),
				auditLog.getLab() == null ? null : auditLog.getLab().getId(),
				actor == null ? null : actor.getId(),
				actor == null ? null : actor.getEmail(),
				actor == null ? null : actor.getFullName(),
				auditLog.getAction(),
				auditLog.getEntityType(),
				auditLog.getEntityId(),
				auditLog.getOldValue(),
				auditLog.getNewValue(),
				auditLog.getIpAddress(),
				auditLog.getUserAgent(),
				auditLog.getCreatedAt());
	}

	private static AdminLoginHistoryResponse toLoginHistoryResponse(LoginHistory loginHistory) {
		User user = loginHistory.getUser();
		return new AdminLoginHistoryResponse(
				loginHistory.getId(),
				user == null ? null : user.getId(),
				user == null ? null : user.getEmail(),
				user == null ? null : user.getFullName(),
				loginHistory.getLoginAt(),
				loginHistory.getIpAddress(),
				loginHistory.getUserAgent(),
				loginHistory.getSuccess(),
				loginHistory.getFailureReason());
	}
}

package com.smartlab.service.admin;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.AuditLog;
import com.smartlab.entity.LoginHistory;
import com.smartlab.entity.User;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.AuditLogRepository;
import com.smartlab.repository.LoginHistoryRepository;
import com.smartlab.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;

@Service
@Profile("!nodb")
public class AdminAuditLogService {

	private static final int MAX_PAGE_SIZE = 100;

	private final AuditLogRepository auditLogRepository;
	private final LoginHistoryRepository loginHistoryRepository;
	private final UserRepository userRepository;

	public AdminAuditLogService(
			AuditLogRepository auditLogRepository,
			LoginHistoryRepository loginHistoryRepository,
			UserRepository userRepository) {
		this.auditLogRepository = auditLogRepository;
		this.loginHistoryRepository = loginHistoryRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public Page<AuditLogSummary> listAuditLogs(AuditLogFilter filter) {
		AuditLogFilter safeFilter = filter == null ? AuditLogFilter.empty() : filter;
		validateDateRange(safeFilter.start(), safeFilter.end());
		Pageable pageable = pageable(safeFilter.page(), safeFilter.size(), "createdAt");
		return auditLogRepository.findAll(auditLogSpecification(
				normalizeOptionalText(safeFilter.action()),
				safeFilter.actorId(),
				normalizeOptionalText(safeFilter.entityType()),
				safeFilter.entityId(),
				safeFilter.start(),
				safeFilter.end()),
				pageable).map(AuditLogSummary::from);
	}

	@Transactional(readOnly = true)
	public Page<LoginHistorySummary> listLoginHistories(LoginHistoryFilter filter) {
		LoginHistoryFilter safeFilter = filter == null ? LoginHistoryFilter.empty() : filter;
		validateDateRange(safeFilter.start(), safeFilter.end());
		Pageable pageable = pageable(safeFilter.page(), safeFilter.size(), "loginAt");
		return loginHistoryRepository.findAll(loginHistorySpecification(
				safeFilter.userId(),
				safeFilter.success(),
				normalizeOptionalText(safeFilter.ipAddress()),
				safeFilter.start(),
				safeFilter.end()),
				pageable).map(LoginHistorySummary::from);
	}

	@Transactional(readOnly = true)
	public Page<LoginHistorySummary> listLoginHistoriesForUser(UUID userId, LoginHistoryFilter filter) {
		if (userId == null) {
			throw new InvalidAdminServiceInputException("User ID must not be null.");
		}
		if (!userRepository.existsById(userId)) {
			throw new ResourceNotFoundException("User was not found.");
		}
		LoginHistoryFilter safeFilter = filter == null ? LoginHistoryFilter.empty() : filter;
		return listLoginHistories(new LoginHistoryFilter(
				userId,
				safeFilter.success(),
				safeFilter.ipAddress(),
				safeFilter.start(),
				safeFilter.end(),
				safeFilter.page(),
				safeFilter.size()));
	}

	private static Pageable pageable(int page, int size, String sortProperty) {
		if (page < 0) {
			throw new InvalidAdminServiceInputException("Page index must not be negative.");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new InvalidAdminServiceInputException("Page size must be between 1 and 100.");
		}
		return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortProperty));
	}

	private static void validateDateRange(OffsetDateTime start, OffsetDateTime end) {
		if (start != null && end != null && start.isAfter(end)) {
			throw new InvalidAdminServiceInputException("Start date must not be after end date.");
		}
	}

	private static String normalizeOptionalText(String value) {
		if (value == null || value.trim().isBlank()) {
			return null;
		}
		return value.trim();
	}

	private static Specification<AuditLog> auditLogSpecification(
			String action,
			UUID actorId,
			String entityType,
			UUID entityId,
			OffsetDateTime start,
			OffsetDateTime end) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (action != null) {
				predicates.add(builder.equal(root.get("action"), action));
			}
			if (actorId != null) {
				predicates.add(builder.equal(root.get("actor").get("id"), actorId));
			}
			if (entityType != null) {
				predicates.add(builder.equal(root.get("entityType"), entityType));
			}
			if (entityId != null) {
				predicates.add(builder.equal(root.get("entityId"), entityId));
			}
			if (start != null) {
				predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), start));
			}
			if (end != null) {
				predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), end));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static Specification<LoginHistory> loginHistorySpecification(
			UUID userId,
			Boolean success,
			String ipAddress,
			OffsetDateTime start,
			OffsetDateTime end) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (userId != null) {
				predicates.add(builder.equal(root.get("user").get("id"), userId));
			}
			if (success != null) {
				predicates.add(builder.equal(root.get("success"), success));
			}
			if (ipAddress != null) {
				predicates.add(builder.equal(root.get("ipAddress"), ipAddress));
			}
			if (start != null) {
				predicates.add(builder.greaterThanOrEqualTo(root.get("loginAt"), start));
			}
			if (end != null) {
				predicates.add(builder.lessThanOrEqualTo(root.get("loginAt"), end));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	public record AuditLogFilter(
			String action,
			UUID actorId,
			String entityType,
			UUID entityId,
			OffsetDateTime start,
			OffsetDateTime end,
			int page,
			int size) {

		static AuditLogFilter empty() {
			return new AuditLogFilter(null, null, null, null, null, null, 0, 20);
		}
	}

	public record LoginHistoryFilter(
			UUID userId,
			Boolean success,
			String ipAddress,
			OffsetDateTime start,
			OffsetDateTime end,
			int page,
			int size) {

		static LoginHistoryFilter empty() {
			return new LoginHistoryFilter(null, null, null, null, null, 0, 20);
		}
	}

	public record AuditLogSummary(
			UUID id,
			UUID labId,
			UUID actorId,
			String actorEmail,
			String actorFullName,
			String action,
			String entityType,
			UUID entityId,
			String ipAddress,
			String userAgent,
			OffsetDateTime createdAt) {

		static AuditLogSummary from(AuditLog auditLog) {
			User actor = auditLog.getActor();
			return new AuditLogSummary(
					auditLog.getId(),
					auditLog.getLab() == null ? null : auditLog.getLab().getId(),
					actor == null ? null : actor.getId(),
					actor == null ? null : actor.getEmail(),
					actor == null ? null : actor.getFullName(),
					auditLog.getAction(),
					auditLog.getEntityType(),
					auditLog.getEntityId(),
					auditLog.getIpAddress(),
					auditLog.getUserAgent(),
					auditLog.getCreatedAt());
		}
	}

	public record LoginHistorySummary(
			UUID id,
			UUID userId,
			String userEmail,
			String userFullName,
			OffsetDateTime loginAt,
			String ipAddress,
			String userAgent,
			Boolean success,
			String failureReason) {

		static LoginHistorySummary from(LoginHistory loginHistory) {
			User user = loginHistory.getUser();
			return new LoginHistorySummary(
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
}

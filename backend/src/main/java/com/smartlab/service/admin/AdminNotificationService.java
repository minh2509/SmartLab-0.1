package com.smartlab.service.admin;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.Notification;
import com.smartlab.entity.NotificationRecipient;
import com.smartlab.entity.User;
import com.smartlab.enums.NotificationRelatedType;
import com.smartlab.enums.NotificationTargetType;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.NotificationRecipientRepository;
import com.smartlab.repository.NotificationRepository;
import com.smartlab.repository.admin.AdminNotificationReadRepository;
import com.smartlab.service.common.AuditLogService;
import com.smartlab.service.common.NotificationService;

@Service
@Profile("!nodb")
public class AdminNotificationService {

	private static final int MAX_PAGE_SIZE = 100;
	private static final String AUDIT_ENTITY_TYPE = "NOTIFICATION";
	private static final List<String> CREATABLE_NOTIFICATION_TYPES = List.of("ADMIN_ANNOUNCEMENT");

	private final NotificationRepository notificationRepository;
	private final NotificationRecipientRepository recipientRepository;
	private final AdminNotificationReadRepository notificationReadRepository;
	private final NotificationService notificationService;
	private final AuditLogService auditLogService;
	private final Clock clock;

	public AdminNotificationService(
			NotificationRepository notificationRepository,
			NotificationRecipientRepository recipientRepository,
			AdminNotificationReadRepository notificationReadRepository,
			NotificationService notificationService,
			AuditLogService auditLogService,
			Clock clock) {
		this.notificationRepository = notificationRepository;
		this.recipientRepository = recipientRepository;
		this.notificationReadRepository = notificationReadRepository;
		this.notificationService = notificationService;
		this.auditLogService = auditLogService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Page<NotificationSummary> getNotifications(
			UUID labId,
			NotificationFilter filter,
			int page,
			int size) {
		validateLabAndPagination(labId, page, size);
		NotificationFilter safeFilter = filter == null ? NotificationFilter.empty() : filter.normalized();
		validateDateRange(safeFilter.createdFrom(), safeFilter.createdTo());
		return notificationReadRepository.findPage(
				labId,
				new AdminNotificationReadRepository.NotificationCriteria(
						safeFilter.notificationType(),
						safeFilter.creatorId(),
						safeFilter.relatedType(),
						safeFilter.createdFrom(),
						safeFilter.createdTo()),
				page,
				size)
				.map(this::toSummary);
	}

	@Transactional(readOnly = true)
	public NotificationFilterOptions getFilterOptions(UUID labId) {
		if (labId == null) {
			throw new InvalidAdminServiceInputException("Lab ID must not be null.");
		}
		AdminNotificationReadRepository.NotificationFilterLookup lookup =
				notificationReadRepository.findFilterOptions(labId);
		List<String> notificationTypes = lookup.notificationTypes()
				.stream()
				.map(AdminNotificationService::normalizeOptional)
				.filter(Objects::nonNull)
				.distinct()
				.sorted()
				.toList();
		Map<UUID, CreatorOption> creatorsById = new LinkedHashMap<>();
		for (User creator : lookup.creators()) {
			if (creator != null
					&& creator.getId() != null
					&& creator.getLab() != null
					&& labId.equals(creator.getLab().getId())) {
				creatorsById.putIfAbsent(
						creator.getId(),
						new CreatorOption(
								creator.getId(),
								creator.getFullName(),
								creator.getEmail(),
								creator.getAccountStatus()));
			}
		}
		List<CreatorOption> creators = creatorsById.values()
				.stream()
				.sorted(Comparator
						.comparing(CreatorOption::fullName, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(CreatorOption::email, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(CreatorOption::id))
				.toList();
		return new NotificationFilterOptions(
				notificationTypes,
				CREATABLE_NOTIFICATION_TYPES,
				Arrays.stream(NotificationRelatedType.values()).map(Enum::name).toList(),
				creators);
	}

	@Transactional(readOnly = true)
	public NotificationDetail getNotificationDetail(UUID labId, UUID notificationId) {
		Notification notification = findScopedNotification(labId, notificationId);
		List<NotificationRecipient> recipients = recipientRepository.findByNotification(notification);
		if (recipients.stream().noneMatch(recipient -> recipient.getDeletedAt() == null)) {
			throw new ResourceNotFoundException("Notification was not found.");
		}
		return toDetail(notification, recipients);
	}

	@Transactional
	public NotificationDetail createNotification(
			UUID labId,
			UUID adminId,
			CreateNotificationCommand command) {
		if (labId == null || adminId == null) {
			throw new InvalidAdminServiceInputException("Lab ID and admin user ID must not be null.");
		}
		if (command == null || command.targetType() == null) {
			throw new InvalidAdminServiceInputException("Notification command and target type are required.");
		}
		validateTarget(command);

		NotificationService.NotificationCommand notificationCommand = new NotificationService.NotificationCommand(
				labId,
				adminId,
				command.title(),
				command.message(),
				command.notificationType(),
				command.relatedType(),
				command.relatedId(),
				command.linkUrl());
		NotificationService.NotificationResult result = switch (command.targetType()) {
			case USER -> notificationService.sendToUsers(notificationCommand, command.userIds());
			case PROJECT -> notificationService.sendToProject(notificationCommand, command.projectId());
			case LAB -> notificationService.sendToLab(notificationCommand);
		};

		Map<String, Object> auditValue = new LinkedHashMap<>();
		auditValue.put("targetType", command.targetType().name());
		auditValue.put("recipientCount", result.recipientCount());
		auditLogService.record(new AuditLogService.AuditCommand(
				adminId,
				"CREATE_NOTIFICATION",
				AUDIT_ENTITY_TYPE,
				result.notificationId(),
				null,
				auditValue));
		Notification notification = findScopedNotification(labId, result.notificationId());
		return toDetail(notification, recipientRepository.findByNotification(notification));
	}

	@Transactional
	public void hideNotification(UUID labId, UUID notificationId, UUID adminId) {
		if (adminId == null) {
			throw new InvalidAdminServiceInputException("Admin user ID must not be null.");
		}
		Notification notification = findScopedNotification(labId, notificationId);
		List<NotificationRecipient> recipients = recipientRepository.findByNotification(notification);
		OffsetDateTime hiddenAt = OffsetDateTime.now(clock);
		long hiddenCount = 0;
		for (NotificationRecipient recipient : recipients) {
			if (recipient.getDeletedAt() == null) {
				recipient.setDeletedAt(hiddenAt);
				hiddenCount++;
			}
		}
		if (hiddenCount > 0) {
			recipientRepository.saveAll(recipients);
		}
		auditLogService.record(new AuditLogService.AuditCommand(
				adminId,
				"DELETE_NOTIFICATION",
				AUDIT_ENTITY_TYPE,
				notificationId,
				null,
				Map.of("hiddenRecipientCount", hiddenCount)));
	}

	private Notification findScopedNotification(UUID labId, UUID notificationId) {
		if (labId == null || notificationId == null) {
			throw new InvalidAdminServiceInputException("Lab ID and notification ID must not be null.");
		}
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new ResourceNotFoundException("Notification was not found."));
		if (notification.getLab() == null || !labId.equals(notification.getLab().getId())) {
			throw new ResourceNotFoundException("Notification was not found.");
		}
		return notification;
	}

	private NotificationSummary toSummary(Notification notification) {
		List<NotificationRecipient> recipients = recipientRepository.findByNotification(notification);
		return new NotificationSummary(
				notification.getId(),
				notification.getTitle(),
				notification.getMessage(),
				notification.getNotificationType(),
				notification.getRelatedType(),
				notification.getRelatedId(),
				notification.getLinkUrl(),
				toUser(notification.getCreatedBy()),
				recipients.size(),
				recipients.stream().filter(recipient -> recipient.getReadAt() != null).count(),
				notification.getCreatedAt());
	}

	private NotificationDetail toDetail(
			Notification notification,
			List<NotificationRecipient> recipients) {
		NotificationSummary summary = toSummary(notification);
		return new NotificationDetail(
				summary,
				recipients.stream()
						.map(recipient -> new RecipientSummary(
								recipient.getRecipient().getId(),
								recipient.getRecipient().getFullName(),
								recipient.getReadAt(),
								recipient.getDeletedAt(),
								recipient.getCreatedAt()))
						.toList());
	}

	private static UserSummary toUser(User user) {
		return user == null ? null : new UserSummary(user.getId(), user.getFullName());
	}

	private static void validateTarget(CreateNotificationCommand command) {
		List<UUID> userIds = command.userIds() == null ? List.of() : command.userIds();
		if (userIds.stream().anyMatch(java.util.Objects::isNull)) {
			throw new InvalidAdminServiceInputException("Notification user IDs must not contain null.");
		}
		switch (command.targetType()) {
			case USER -> {
				if (userIds.isEmpty() || command.projectId() != null) {
					throw new InvalidAdminServiceInputException(
							"USER target requires userIds and does not accept projectId.");
				}
			}
			case PROJECT -> {
				if (command.projectId() == null || !userIds.isEmpty()) {
					throw new InvalidAdminServiceInputException(
							"PROJECT target requires projectId and does not accept userIds.");
				}
			}
			case LAB -> {
				if (command.projectId() != null || !userIds.isEmpty()) {
					throw new InvalidAdminServiceInputException(
							"LAB target does not accept projectId or userIds.");
				}
			}
		}
	}

	private static void validateLabAndPagination(UUID labId, int page, int size) {
		if (labId == null) {
			throw new InvalidAdminServiceInputException("Lab ID must not be null.");
		}
		if (page < 0) {
			throw new InvalidAdminServiceInputException("Page index must not be negative.");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new InvalidAdminServiceInputException("Page size must be between 1 and 100.");
		}
	}

	private static void validateDateRange(OffsetDateTime from, OffsetDateTime to) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new InvalidAdminServiceInputException("Created-from must not be after created-to.");
		}
	}

	private static String normalizeOptional(String value) {
		return value == null || value.trim().isBlank() ? null : value.trim();
	}

	public record NotificationFilter(
			String notificationType,
			UUID creatorId,
			String relatedType,
			OffsetDateTime createdFrom,
			OffsetDateTime createdTo) {

		public static NotificationFilter empty() {
			return new NotificationFilter(null, null, null, null, null);
		}

		public NotificationFilter normalized() {
			return new NotificationFilter(
					normalizeOptional(notificationType),
					creatorId,
					normalizeOptional(relatedType),
					createdFrom,
					createdTo);
		}
	}

	public record NotificationFilterOptions(
			List<String> notificationTypes,
			List<String> creatableNotificationTypes,
			List<String> relatedTypes,
			List<CreatorOption> creators) {

		public NotificationFilterOptions {
			notificationTypes = List.copyOf(notificationTypes);
			creatableNotificationTypes = List.copyOf(creatableNotificationTypes);
			relatedTypes = List.copyOf(relatedTypes);
			creators = List.copyOf(creators);
		}
	}

	public record CreatorOption(
			UUID id,
			String fullName,
			String email,
			UserAccountStatus accountStatus) {
	}

	public record CreateNotificationCommand(
			String title,
			String message,
			String notificationType,
			NotificationTargetType targetType,
			List<UUID> userIds,
			UUID projectId,
			String relatedType,
			UUID relatedId,
			String linkUrl) {
	}

	public record NotificationSummary(
			UUID id,
			String title,
			String message,
			String notificationType,
			String relatedType,
			UUID relatedId,
			String linkUrl,
			UserSummary createdBy,
			long recipientCount,
			long readCount,
			OffsetDateTime createdAt) {
	}

	public record NotificationDetail(
			NotificationSummary summary,
			List<RecipientSummary> recipients) {

		public NotificationDetail {
			recipients = List.copyOf(recipients);
		}
	}

	public record UserSummary(UUID id, String fullName) {
	}

	public record RecipientSummary(
			UUID userId,
			String fullName,
			OffsetDateTime readAt,
			OffsetDateTime hiddenAt,
			OffsetDateTime createdAt) {
	}
}

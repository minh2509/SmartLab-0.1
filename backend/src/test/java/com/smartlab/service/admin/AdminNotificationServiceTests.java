package com.smartlab.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.smartlab.entity.Lab;
import com.smartlab.entity.Notification;
import com.smartlab.entity.NotificationRecipient;
import com.smartlab.entity.User;
import com.smartlab.enums.NotificationRelatedType;
import com.smartlab.enums.NotificationTargetType;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.repository.NotificationRecipientRepository;
import com.smartlab.repository.NotificationRepository;
import com.smartlab.repository.admin.AdminNotificationReadRepository;
import com.smartlab.service.common.AuditLogService;
import com.smartlab.service.common.NotificationService;

class AdminNotificationServiceTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-23T02:00:00Z"), ZoneOffset.UTC);

	private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
	private final NotificationRecipientRepository recipientRepository = mock(NotificationRecipientRepository.class);
	private final AdminNotificationReadRepository readRepository = mock(AdminNotificationReadRepository.class);
	private final NotificationService notificationService = mock(NotificationService.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final AdminNotificationService service = new AdminNotificationService(
			notificationRepository,
			recipientRepository,
			readRepository,
			notificationService,
			auditLogService,
			CLOCK);

	@Test
	void filterOptionsUseVisibleNotificationDataAndNeverLeakCreatorsAcrossLabs() {
		UUID labId = UUID.randomUUID();
		Lab lab = new Lab();
		lab.setId(labId);
		Lab otherLab = new Lab();
		otherLab.setId(UUID.randomUUID());
		User secondCreator = creator(UUID.randomUUID(), lab, "Zoe Admin", "zoe@smart.lab");
		User firstCreator = creator(UUID.randomUUID(), lab, "Amara Admin", "amara@smart.lab");
		User crossLabCreator = creator(UUID.randomUUID(), otherLab, "Other Admin", "other@smart.lab");
		when(readRepository.findFilterOptions(labId)).thenReturn(
				new AdminNotificationReadRepository.NotificationFilterLookup(
						List.of(" SYSTEM_NOTICE ", "ADMIN_ANNOUNCEMENT", "SYSTEM_NOTICE", " "),
						List.of(secondCreator, crossLabCreator, firstCreator, secondCreator)));

		AdminNotificationService.NotificationFilterOptions result = service.getFilterOptions(labId);

		assertEquals(List.of("ADMIN_ANNOUNCEMENT", "SYSTEM_NOTICE"), result.notificationTypes());
		assertEquals(List.of("ADMIN_ANNOUNCEMENT"), result.creatableNotificationTypes());
		assertEquals(
				List.of(NotificationRelatedType.PROJECT.name(), NotificationRelatedType.PROJECT_JOIN_REQUEST.name()),
				result.relatedTypes());
		assertEquals(List.of(firstCreator.getId(), secondCreator.getId()),
				result.creators().stream().map(AdminNotificationService.CreatorOption::id).toList());
		assertEquals(UserAccountStatus.ACTIVE, result.creators().getFirst().accountStatus());
		verify(readRepository).findFilterOptions(labId);
	}

	@Test
	void createUserNotificationDelegatesToCoreServiceAndWritesAudit() {
		UUID labId = UUID.randomUUID();
		UUID adminId = UUID.randomUUID();
		UUID recipientId = UUID.randomUUID();
		UUID notificationId = UUID.randomUUID();
		Notification notification = notification(notificationId, labId, adminId);
		NotificationRecipient recipient = recipient(notification, recipientId, null, null);
		when(notificationService.sendToUsers(any(), any()))
				.thenReturn(new NotificationService.NotificationResult(notificationId, 1));
		when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
		when(recipientRepository.findByNotification(notification)).thenReturn(List.of(recipient));

		AdminNotificationService.NotificationDetail result = service.createNotification(
				labId,
				adminId,
				new AdminNotificationService.CreateNotificationCommand(
						"Manual notice",
						"Message",
						"ADMIN_MANUAL",
						NotificationTargetType.USER,
						List.of(recipientId, recipientId),
						null,
						null,
						null,
						"/app/notifications"));

		assertEquals(notificationId, result.summary().id());
		assertEquals(1, result.summary().recipientCount());
		verify(notificationService).sendToUsers(any(NotificationService.NotificationCommand.class),
				org.mockito.ArgumentMatchers.eq(List.of(recipientId, recipientId)));
		verify(auditLogService).record(any(AuditLogService.AuditCommand.class));
	}

	@Test
	void detailCountsOnlyActiveRecipientsAndReadState() {
		UUID labId = UUID.randomUUID();
		Notification notification = notification(UUID.randomUUID(), labId, UUID.randomUUID());
		NotificationRecipient read = recipient(notification, UUID.randomUUID(), OffsetDateTime.now(CLOCK), null);
		NotificationRecipient hidden = recipient(
				notification, UUID.randomUUID(), null, OffsetDateTime.now(CLOCK).minusDays(1));
		when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
		when(recipientRepository.findByNotification(notification)).thenReturn(List.of(read, hidden));

		AdminNotificationService.NotificationDetail result = service.getNotificationDetail(labId, notification.getId());

		assertEquals(2, result.summary().recipientCount());
		assertEquals(1, result.summary().readCount());
		assertEquals(2, result.recipients().size());
		assertEquals(hidden.getDeletedAt(), result.recipients().get(1).hiddenAt());
	}

	@Test
	void hideMarksEveryActiveRecipientWithoutDeletingNotification() {
		UUID labId = UUID.randomUUID();
		UUID adminId = UUID.randomUUID();
		Notification notification = notification(UUID.randomUUID(), labId, adminId);
		NotificationRecipient first = recipient(notification, UUID.randomUUID(), null, null);
		NotificationRecipient second = recipient(notification, UUID.randomUUID(), null, null);
		when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
		when(recipientRepository.findByNotification(notification)).thenReturn(List.of(first, second));

		service.hideNotification(labId, notification.getId(), adminId);

		assertNotNull(first.getDeletedAt());
		assertEquals(OffsetDateTime.now(CLOCK), first.getDeletedAt());
		assertEquals(OffsetDateTime.now(CLOCK), second.getDeletedAt());
		verify(recipientRepository).saveAll(any());
		verify(auditLogService).record(any(AuditLogService.AuditCommand.class));
	}

	@Test
	void targetAndPaginationValidationRejectAmbiguousRequests() {
		UUID labId = UUID.randomUUID();
		UUID adminId = UUID.randomUUID();
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.createNotification(
						labId,
						adminId,
						new AdminNotificationService.CreateNotificationCommand(
								"Title",
								null,
								"TYPE",
								NotificationTargetType.LAB,
								List.of(UUID.randomUUID()),
								null,
								null,
								null,
								null)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.getNotifications(labId, AdminNotificationService.NotificationFilter.empty(), 0, 101));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.getNotifications(
						labId,
						new AdminNotificationService.NotificationFilter(
								null,
								null,
								null,
								OffsetDateTime.parse("2026-07-24T00:00:00Z"),
								OffsetDateTime.parse("2026-07-23T00:00:00Z")),
						0,
						20));
	}

	private static Notification notification(UUID notificationId, UUID labId, UUID adminId) {
		Lab lab = new Lab();
		lab.setId(labId);
		User admin = user(adminId, lab, "Admin User");
		Notification notification = new Notification();
		notification.setId(notificationId);
		notification.setLab(lab);
		notification.setCreatedBy(admin);
		notification.setTitle("Manual notice");
		notification.setMessage("Message");
		notification.setNotificationType("ADMIN_MANUAL");
		notification.setCreatedAt(OffsetDateTime.parse("2026-07-23T00:00:00Z"));
		return notification;
	}

	private static NotificationRecipient recipient(
			Notification notification,
			UUID userId,
			OffsetDateTime readAt,
			OffsetDateTime deletedAt) {
		NotificationRecipient recipient = new NotificationRecipient();
		recipient.setId(UUID.randomUUID());
		recipient.setNotification(notification);
		recipient.setRecipient(user(userId, notification.getLab(), "Recipient"));
		recipient.setReadAt(readAt);
		recipient.setDeletedAt(deletedAt);
		recipient.setCreatedAt(OffsetDateTime.parse("2026-07-23T00:00:00Z"));
		return recipient;
	}

	private static User user(UUID id, Lab lab, String fullName) {
		User user = new User();
		user.setId(id);
		user.setLab(lab);
		user.setFullName(fullName);
		return user;
	}

	private static User creator(UUID id, Lab lab, String fullName, String email) {
		User user = user(id, lab, fullName);
		user.setEmail(email);
		user.setAccountStatus(UserAccountStatus.ACTIVE);
		return user;
	}
}

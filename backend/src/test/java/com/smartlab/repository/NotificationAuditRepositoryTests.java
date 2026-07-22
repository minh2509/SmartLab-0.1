package com.smartlab.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.AuditLog;
import com.smartlab.entity.Lab;
import com.smartlab.entity.LoginHistory;
import com.smartlab.entity.Notification;
import com.smartlab.entity.NotificationRecipient;
import com.smartlab.entity.User;

class NotificationAuditRepositoryTests {

	@Test
	void repositoriesUseUuidJpaRepositoryContracts() {
		assertJpaRepository(NotificationRepository.class);
		assertJpaRepository(NotificationRecipientRepository.class);
		assertJpaRepository(AuditLogRepository.class);
		assertJpaRepository(LoginHistoryRepository.class);
	}

	@Test
	void notificationRepositoryExposesFocusedQueriesFromActualFields() throws NoSuchMethodException {
		assertReturnType(NotificationRepository.class.getMethod("findByLab", Lab.class), List.class);
		assertReturnType(NotificationRepository.class.getMethod("findByCreatedBy", User.class), List.class);
		assertReturnType(NotificationRepository.class.getMethod("findByNotificationType", String.class), List.class);
		assertReturnType(NotificationRepository.class.getMethod("findByLabOrderByCreatedAtDesc", Lab.class), List.class);
		assertReturnType(
				NotificationRepository.class.getMethod("findByRelatedTypeAndRelatedId", String.class, UUID.class),
				List.class);
		assertReturnType(
				NotificationRepository.class.getMethod("findByCreatedAtBetween", OffsetDateTime.class, OffsetDateTime.class),
				List.class);
	}

	@Test
	void notificationRecipientRepositoryExposesFocusedQueriesFromActualFields() throws NoSuchMethodException {
		assertReturnType(
				NotificationRecipientRepository.class.getMethod(
						"existsByNotificationAndRecipient",
						Notification.class,
						User.class),
				boolean.class);
		assertReturnType(NotificationRecipientRepository.class.getMethod("findByNotification", Notification.class), List.class);
		assertReturnType(NotificationRecipientRepository.class.getMethod("findByRecipient", User.class), List.class);
		assertReturnType(
				NotificationRecipientRepository.class.getMethod("findByRecipientAndReadAtIsNull", User.class),
				List.class);
		assertReturnType(
				NotificationRecipientRepository.class.getMethod("findByRecipientOrderByCreatedAtDesc", User.class),
				List.class);
		assertReturnType(
				NotificationRecipientRepository.class.getMethod("countByRecipientAndReadAtIsNull", User.class),
				long.class);
	}

	@Test
	void auditLogRepositoryExposesFocusedQueriesWithoutJsonQueries() throws NoSuchMethodException {
		assertReturnType(AuditLogRepository.class.getMethod("findByLab", Lab.class), List.class);
		assertReturnType(AuditLogRepository.class.getMethod("findByActor", User.class), List.class);
		assertReturnType(AuditLogRepository.class.getMethod("findByAction", String.class), List.class);
		assertReturnType(AuditLogRepository.class.getMethod("findByEntityTypeAndEntityId", String.class, UUID.class), List.class);
		assertReturnType(AuditLogRepository.class.getMethod("findByOrderByCreatedAtDesc"), List.class);
		assertReturnType(
				AuditLogRepository.class.getMethod("findByCreatedAtBetween", OffsetDateTime.class, OffsetDateTime.class),
				List.class);
	}

	@Test
	void loginHistoryRepositoryExposesFocusedQueriesFromActualFields() throws NoSuchMethodException {
		assertReturnType(LoginHistoryRepository.class.getMethod("findByUser", User.class), List.class);
		assertReturnType(LoginHistoryRepository.class.getMethod("findBySuccess", Boolean.class), List.class);
		assertReturnType(LoginHistoryRepository.class.getMethod("findByUserOrderByLoginAtDesc", User.class), List.class);
		assertReturnType(
				LoginHistoryRepository.class.getMethod("findByLoginAtBetween", OffsetDateTime.class, OffsetDateTime.class),
				List.class);
		assertReturnType(LoginHistoryRepository.class.getMethod("findByIpAddress", String.class), List.class);
	}

	private static void assertJpaRepository(Class<?> repositoryType) {
		assertTrue(JpaRepository.class.isAssignableFrom(repositoryType));
		assertEquals(1, countEntitySpecificRepositoryInterfaces(repositoryType));
	}

	private static long countEntitySpecificRepositoryInterfaces(Class<?> repositoryType) {
		return java.util.Arrays.stream(repositoryType.getGenericInterfaces())
				.filter(type -> type.getTypeName().contains("<"))
				.filter(type -> type.getTypeName().contains(UUID.class.getName()))
				.filter(type -> type.getTypeName().contains(entityNameFor(repositoryType)))
				.count();
	}

	private static String entityNameFor(Class<?> repositoryType) {
		if (repositoryType == NotificationRepository.class) {
			return Notification.class.getName();
		}
		if (repositoryType == NotificationRecipientRepository.class) {
			return NotificationRecipient.class.getName();
		}
		if (repositoryType == AuditLogRepository.class) {
			return AuditLog.class.getName();
		}
		if (repositoryType == LoginHistoryRepository.class) {
			return LoginHistory.class.getName();
		}
		throw new IllegalArgumentException("Unsupported repository type: " + repositoryType.getName());
	}

	private static void assertReturnType(Method method, Class<?> returnType) {
		assertEquals(returnType, method.getReturnType());
	}
}

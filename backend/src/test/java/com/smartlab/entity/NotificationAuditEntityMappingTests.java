package com.smartlab.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

class NotificationAuditEntityMappingTests {

	@Test
	void notificationMapsColumnsTimestampsAndForeignKeysFromV7() throws NoSuchFieldException {
		assertEntityTable(Notification.class, "notifications");
		assertNoUniqueConstraints(Notification.class);
		assertId(Notification.class);
		assertManyToOne(Notification.class, "lab", "lab_id", false);
		assertColumn(Notification.class, "title", "title", false, 255);
		assertColumn(Notification.class, "message", "message", true, 0);
		assertColumn(Notification.class, "notificationType", "notification_type", false, 80);
		assertColumn(Notification.class, "relatedType", "related_type", true, 80);
		assertColumn(Notification.class, "relatedId", "related_id", true, 0);
		assertColumn(Notification.class, "linkUrl", "link_url", true, 500);
		assertManyToOne(Notification.class, "createdBy", "created_by", true);
		assertColumn(Notification.class, "createdAt", "created_at", false, 0);
		assertFieldType(Notification.class, "relatedId", UUID.class);
		assertFieldType(Notification.class, "createdAt", OffsetDateTime.class);
	}

	@Test
	void notificationRecipientMapsUniquePairReadStateAndRelationships() throws NoSuchFieldException {
		assertEntityTable(NotificationRecipient.class, "notification_recipients");
		assertUniqueConstraint(NotificationRecipient.class, "uq_notification_recipients", "notification_id", "recipient_id");
		assertId(NotificationRecipient.class);
		assertManyToOne(NotificationRecipient.class, "notification", "notification_id", false);
		assertManyToOne(NotificationRecipient.class, "recipient", "recipient_id", false);
		assertColumn(NotificationRecipient.class, "readAt", "read_at", true, 0);
		assertColumn(NotificationRecipient.class, "deletedAt", "deleted_at", true, 0);
		assertColumn(NotificationRecipient.class, "createdAt", "created_at", false, 0);
		assertFieldType(NotificationRecipient.class, "readAt", OffsetDateTime.class);
		assertFieldType(NotificationRecipient.class, "deletedAt", OffsetDateTime.class);
		assertFieldType(NotificationRecipient.class, "createdAt", OffsetDateTime.class);
	}

	@Test
	void auditLogMapsJsonbScalarTargetIpAddressAndNullableHistoricalRelationships() throws NoSuchFieldException {
		assertEntityTable(AuditLog.class, "audit_logs");
		assertNoUniqueConstraints(AuditLog.class);
		assertId(AuditLog.class);
		assertManyToOne(AuditLog.class, "lab", "lab_id", true);
		assertManyToOne(AuditLog.class, "actor", "actor_id", true);
		assertColumn(AuditLog.class, "action", "action", false, 100);
		assertColumn(AuditLog.class, "entityType", "entity_type", false, 100);
		assertColumn(AuditLog.class, "entityId", "entity_id", true, 0);
		assertJsonbColumn(AuditLog.class, "oldValue", "old_value");
		assertJsonbColumn(AuditLog.class, "newValue", "new_value");
		assertColumn(AuditLog.class, "ipAddress", "ip_address", true, 100);
		assertColumn(AuditLog.class, "userAgent", "user_agent", true, 0);
		assertColumn(AuditLog.class, "createdAt", "created_at", false, 0);
		assertFieldType(AuditLog.class, "entityId", UUID.class);
		assertFieldType(AuditLog.class, "oldValue", String.class);
		assertFieldType(AuditLog.class, "newValue", String.class);
		assertFieldType(AuditLog.class, "ipAddress", String.class);
		assertFieldType(AuditLog.class, "createdAt", OffsetDateTime.class);
		assertFalse(AuditLog.class.getDeclaredField("entityId").isAnnotationPresent(ManyToOne.class));
	}

	@Test
	void loginHistoryMapsNullableUserAndScalarLoginOutcome() throws NoSuchFieldException {
		assertEntityTable(LoginHistory.class, "login_histories");
		assertNoUniqueConstraints(LoginHistory.class);
		assertId(LoginHistory.class);
		assertManyToOne(LoginHistory.class, "user", "user_id", true);
		assertColumn(LoginHistory.class, "loginAt", "login_at", false, 0);
		assertColumn(LoginHistory.class, "ipAddress", "ip_address", true, 100);
		assertColumn(LoginHistory.class, "userAgent", "user_agent", true, 0);
		assertColumn(LoginHistory.class, "success", "success", false, 0);
		assertColumn(LoginHistory.class, "failureReason", "failure_reason", true, 0);
		assertFieldType(LoginHistory.class, "loginAt", OffsetDateTime.class);
		assertFieldType(LoginHistory.class, "ipAddress", String.class);
		assertFieldType(LoginHistory.class, "success", Boolean.class);
		assertFalse(new LoginHistory().getSuccess());
	}

	@Test
	void v7HasNoCheckConstrainedEnumsForThisScope() throws NoSuchFieldException {
		assertNoEnumeratedFields(Notification.class);
		assertNoEnumeratedFields(NotificationRecipient.class);
		assertNoEnumeratedFields(AuditLog.class);
		assertNoEnumeratedFields(LoginHistory.class);
		assertThrows(ClassNotFoundException.class, () -> Class.forName("com.smartlab.enums.NotificationType"));
		assertThrows(ClassNotFoundException.class, () -> Class.forName("com.smartlab.enums.NotificationScope"));
		assertThrows(ClassNotFoundException.class, () -> Class.forName("com.smartlab.enums.LoginResult"));
	}

	@Test
	void relationshipsDoNotDeclareCascadeTypes() throws NoSuchFieldException {
		assertNoCascade(Notification.class, "lab");
		assertNoCascade(Notification.class, "createdBy");
		assertNoCascade(NotificationRecipient.class, "notification");
		assertNoCascade(NotificationRecipient.class, "recipient");
		assertNoCascade(AuditLog.class, "lab");
		assertNoCascade(AuditLog.class, "actor");
		assertNoCascade(LoginHistory.class, "user");
	}

	@Test
	void entitiesDoNotExposeInverseCollectionsOrAutomaticReadLogic() {
		assertNoCollectionFields(Notification.class);
		assertNoCollectionFields(NotificationRecipient.class);
		assertNoCollectionFields(AuditLog.class);
		assertNoCollectionFields(LoginHistory.class);
	}

	@Test
	void equalityIsSafeForTransientEntitiesAndDoesNotDependOnJsonContent() {
		AuditLog left = new AuditLog();
		left.setOldValue("{\"name\":\"before\"}");
		AuditLog right = new AuditLog();
		right.setOldValue("{\"name\":\"before\"}");

		assertFalse(new Notification().equals(new Notification()));
		assertFalse(new NotificationRecipient().equals(new NotificationRecipient()));
		assertFalse(left.equals(right));
		assertFalse(new LoginHistory().equals(new LoginHistory()));
		assertEquals(new AuditLog().hashCode(), new AuditLog().hashCode());
	}

	private static void assertEntityTable(Class<?> entityType, String tableName) {
		assertNotNull(entityType.getAnnotation(Entity.class));
		assertEquals(tableName, entityType.getAnnotation(Table.class).name());
	}

	private static void assertId(Class<?> entityType) throws NoSuchFieldException {
		assertNotNull(entityType.getDeclaredField("id").getAnnotation(Id.class));
		assertEquals(UUID.class, entityType.getDeclaredField("id").getType());
	}

	private static void assertColumn(
			Class<?> entityType,
			String fieldName,
			String columnName,
			boolean nullable,
			int length) throws NoSuchFieldException {
		Column column = entityType.getDeclaredField(fieldName).getAnnotation(Column.class);
		assertNotNull(column);
		assertEquals(columnName, column.name());
		assertEquals(nullable, column.nullable());
		if (length > 0) {
			assertEquals(length, column.length());
		}
	}

	private static void assertJsonbColumn(Class<?> entityType, String fieldName, String columnName)
			throws NoSuchFieldException {
		Field field = entityType.getDeclaredField(fieldName);
		Column column = field.getAnnotation(Column.class);
		assertNotNull(column);
		assertEquals(columnName, column.name());
		assertEquals("jsonb", column.columnDefinition());
		assertEquals(true, column.nullable());
		JdbcTypeCode jdbcTypeCode = field.getAnnotation(JdbcTypeCode.class);
		assertNotNull(jdbcTypeCode);
		assertEquals(SqlTypes.JSON, jdbcTypeCode.value());
	}

	private static void assertManyToOne(
			Class<?> entityType,
			String fieldName,
			String columnName,
			boolean nullable) throws NoSuchFieldException {
		Field field = entityType.getDeclaredField(fieldName);
		ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
		assertNotNull(manyToOne);
		assertEquals(FetchType.LAZY, manyToOne.fetch());
		assertEquals(nullable, manyToOne.optional());
		JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
		assertNotNull(joinColumn);
		assertEquals(columnName, joinColumn.name());
		assertEquals(nullable, joinColumn.nullable());
	}

	private static void assertFieldType(Class<?> entityType, String fieldName, Class<?> expectedType)
			throws NoSuchFieldException {
		assertEquals(expectedType, entityType.getDeclaredField(fieldName).getType());
	}

	private static void assertNoCascade(Class<?> entityType, String fieldName) throws NoSuchFieldException {
		CascadeType[] cascadeTypes = entityType.getDeclaredField(fieldName).getAnnotation(ManyToOne.class).cascade();
		assertEquals(0, cascadeTypes.length);
	}

	private static void assertUniqueConstraint(Class<?> entityType, String constraintName, String... columnNames) {
		UniqueConstraint[] uniqueConstraints = entityType.getAnnotation(Table.class).uniqueConstraints();
		boolean hasConstraint = Arrays.stream(uniqueConstraints)
				.anyMatch(constraint ->
						constraint.name().equals(constraintName)
								&& Arrays.equals(constraint.columnNames(), columnNames));
		assertTrue(hasConstraint);
	}

	private static void assertNoUniqueConstraints(Class<?> entityType) {
		assertEquals(0, entityType.getAnnotation(Table.class).uniqueConstraints().length);
	}

	private static void assertNoEnumeratedFields(Class<?> entityType) {
		long enumeratedFields = Arrays.stream(entityType.getDeclaredFields())
				.filter(field -> field.isAnnotationPresent(jakarta.persistence.Enumerated.class))
				.count();
		assertEquals(0, enumeratedFields);
	}

	private static void assertNoCollectionFields(Class<?> entityType) {
		boolean hasCollectionField = Arrays.stream(entityType.getDeclaredFields())
				.anyMatch(field -> Collection.class.isAssignableFrom(field.getType()));
		assertFalse(hasCollectionField);
	}
}

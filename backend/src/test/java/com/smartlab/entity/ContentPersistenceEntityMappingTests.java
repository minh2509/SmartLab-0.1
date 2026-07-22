package com.smartlab.entity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.smartlab.enums.PostContentType;
import com.smartlab.enums.PostModerationAction;
import com.smartlab.enums.PostStatus;
import com.smartlab.enums.PostVisibility;
import com.smartlab.service.common.PostWorkflowService;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

class ContentPersistenceEntityMappingTests {

	@Test
	void postMapsColumnsEnumsTimestampsAndRelationships() throws NoSuchFieldException {
		assertEntityTable(Post.class, "posts");
		assertUniqueConstraint(Post.class, "uq_posts_lab_slug", "lab_id", "slug");
		assertId(Post.class);
		assertManyToOne(Post.class, "lab", "lab_id", false);
		assertManyToOne(Post.class, "project", "project_id", true);
		assertManyToOne(Post.class, "category", "category_id", true);
		assertManyToOne(Post.class, "author", "author_id", true);
		assertColumn(Post.class, "title", "title", false, 255);
		assertColumn(Post.class, "slug", "slug", false, 255);
		assertColumn(Post.class, "summary", "summary", true, 0);
		assertColumn(Post.class, "content", "content", true, 0);
		assertManyToOne(Post.class, "coverFile", "cover_file_id", true);
		assertFieldType(Post.class, "coverFile", File.class);
		assertEnumField(Post.class, "contentType", PostContentType.class);
		assertEnumField(Post.class, "visibility", PostVisibility.class);
		assertEnumField(Post.class, "moderationStatus", PostStatus.class);
		assertColumn(Post.class, "publishedAt", "published_at", true, 0);
		assertManyToOne(Post.class, "reviewedBy", "reviewed_by", true);
		assertColumn(Post.class, "reviewedAt", "reviewed_at", true, 0);
		assertColumn(Post.class, "reviewNote", "review_note", true, 0);
		assertColumn(Post.class, "createdAt", "created_at", false, 0);
		assertColumn(Post.class, "updatedAt", "updated_at", false, 0);
		assertColumn(Post.class, "deletedAt", "deleted_at", true, 0);
		assertFieldType(Post.class, "publishedAt", OffsetDateTime.class);
		assertFieldType(Post.class, "reviewedAt", OffsetDateTime.class);
		assertFieldType(Post.class, "createdAt", OffsetDateTime.class);
		assertFieldType(Post.class, "updatedAt", OffsetDateTime.class);
		assertFieldType(Post.class, "deletedAt", OffsetDateTime.class);
	}

	@Test
	void postModerationLogMapsStatusesWithExistingPostStatus() throws NoSuchFieldException {
		assertEntityTable(PostModerationLog.class, "post_moderation_logs");
		assertNoUniqueConstraints(PostModerationLog.class);
		assertId(PostModerationLog.class);
		assertManyToOne(PostModerationLog.class, "post", "post_id", false);
		assertEnumField(PostModerationLog.class, "action", PostModerationAction.class);
		assertEnumField(PostModerationLog.class, "fromStatus", PostStatus.class);
		assertEnumField(PostModerationLog.class, "toStatus", PostStatus.class);
		assertManyToOne(PostModerationLog.class, "actor", "actor_id", true);
		assertColumn(PostModerationLog.class, "reason", "reason", true, 0);
		assertColumn(PostModerationLog.class, "createdAt", "created_at", false, 0);
		assertFieldType(PostModerationLog.class, "createdAt", OffsetDateTime.class);
	}

	@Test
	void postAttachmentMapsFileEntityAndUniqueRelationships() throws NoSuchFieldException {
		assertEntityTable(PostAttachment.class, "post_attachments");
		assertUniqueConstraint(PostAttachment.class, "uq_post_attachments", "post_id", "file_id");
		assertId(PostAttachment.class);
		assertManyToOne(PostAttachment.class, "post", "post_id", false);
		assertManyToOne(PostAttachment.class, "file", "file_id", false);
		assertFieldType(PostAttachment.class, "file", File.class);
		assertManyToOne(PostAttachment.class, "uploadedBy", "uploaded_by", true);
		assertColumn(PostAttachment.class, "createdAt", "created_at", false, 0);
		assertFieldType(PostAttachment.class, "createdAt", OffsetDateTime.class);
	}

	@Test
	void enumValuesAndPostDefaultsMatchV6DatabaseChecks() {
		assertEnumNames(
				PostContentType.class,
				"NEWS",
				"LAB_ANNOUNCEMENT",
				"PROJECT_ANNOUNCEMENT",
				"MEMBER_BLOG",
				"EXPERIENCE_SHARING",
				"ACADEMIC_POST",
				"RESEARCH_RESULT",
				"EVENT_CONTENT");
		assertEnumNames(PostVisibility.class, "PUBLIC", "LAB_INTERNAL", "PROJECT_INTERNAL", "PRIVATE");
		assertEnumNames(
				PostStatus.class,
				"DRAFT",
				"PENDING_REVIEW",
				"NEEDS_REVISION",
				"APPROVED",
				"PUBLISHED",
				"REJECTED");
		assertEnumNames(
				PostModerationAction.class,
				"CREATE",
				"SUBMIT",
				"APPROVE",
				"REQUEST_REVISION",
				"REJECT",
				"PUBLISH",
				"UNPUBLISH");

		assertEquals(PostVisibility.PUBLIC, new Post().getVisibility());
		assertEquals(PostStatus.DRAFT, new Post().getModerationStatus());
	}

	@Test
	void moderationStatusIsExistingPostStatusAndNotDuplicated() throws NoSuchFieldException {
		assertFieldType(Post.class, "moderationStatus", PostStatus.class);
		assertFieldType(PostModerationLog.class, "fromStatus", PostStatus.class);
		assertFieldType(PostModerationLog.class, "toStatus", PostStatus.class);
		assertThrows(ClassNotFoundException.class, () -> Class.forName("com.smartlab.enums.PostModerationStatus"));
	}

	@Test
	void postStatusValuesRemainCompatibleWithWorkflowService() {
		PostWorkflowService workflowService = new PostWorkflowService();

		assertDoesNotThrow(() -> workflowService.validateTransition(PostStatus.DRAFT, PostStatus.PENDING_REVIEW));
		assertDoesNotThrow(() -> workflowService.validateTransition(PostStatus.PENDING_REVIEW, PostStatus.APPROVED));
		assertDoesNotThrow(() -> workflowService.validateTransition(PostStatus.APPROVED, PostStatus.PUBLISHED));
		assertDoesNotThrow(() -> workflowService.validateTransition(PostStatus.PUBLISHED, PostStatus.APPROVED));
		assertFalse(workflowService.isTransitionAllowed(PostStatus.REJECTED, PostStatus.APPROVED));
	}

	@Test
	void relationshipsDoNotDeclareCascadeTypes() throws NoSuchFieldException {
		assertNoCascade(Post.class, "lab");
		assertNoCascade(Post.class, "project");
		assertNoCascade(Post.class, "category");
		assertNoCascade(Post.class, "author");
		assertNoCascade(Post.class, "coverFile");
		assertNoCascade(Post.class, "reviewedBy");
		assertNoCascade(PostModerationLog.class, "post");
		assertNoCascade(PostModerationLog.class, "actor");
		assertNoCascade(PostAttachment.class, "post");
		assertNoCascade(PostAttachment.class, "file");
		assertNoCascade(PostAttachment.class, "uploadedBy");
	}

	@Test
	void equalityIsSafeForTransientEntities() {
		assertFalse(new Post().equals(new Post()));
		assertFalse(new PostModerationLog().equals(new PostModerationLog()));
		assertFalse(new PostAttachment().equals(new PostAttachment()));
		assertEquals(new Post().hashCode(), new Post().hashCode());
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

	private static void assertEnumField(
			Class<?> entityType,
			String fieldName,
			Class<?> enumType) throws NoSuchFieldException {
		Field field = entityType.getDeclaredField(fieldName);
		assertEquals(enumType, field.getType());
		Enumerated enumerated = field.getAnnotation(Enumerated.class);
		assertNotNull(enumerated);
		assertEquals(EnumType.STRING, enumerated.value());
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

	private static <E extends Enum<E>> void assertEnumNames(Class<E> enumType, String... expectedNames) {
		assertEquals(Arrays.asList(expectedNames), Arrays.stream(enumType.getEnumConstants()).map(Enum::name).toList());
	}
}

package com.smartlab.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.smartlab.enums.TaskAssigneeStatus;
import com.smartlab.enums.TaskPriority;
import com.smartlab.enums.TaskReportStatus;
import com.smartlab.enums.TaskStatus;

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

class TaskEvaluationEntityMappingTests {

	@Test
	void taskMapsColumnsEnumsDatesTimestampsAndRelationships() throws NoSuchFieldException {
		assertEntityTable(Task.class, "tasks");
		assertNoUniqueConstraints(Task.class);
		assertId(Task.class);
		assertManyToOne(Task.class, "project", "project_id", false);
		assertColumn(Task.class, "title", "title", false, 255);
		assertColumn(Task.class, "description", "description", true, 0);
		assertEnumField(Task.class, "priority", TaskPriority.class);
		assertEnumField(Task.class, "status", TaskStatus.class);
		assertColumn(Task.class, "startDate", "start_date", true, 0);
		assertColumn(Task.class, "dueDate", "due_date", true, 0);
		assertColumn(Task.class, "completedAt", "completed_at", true, 0);
		assertManyToOne(Task.class, "createdBy", "created_by", true);
		assertColumn(Task.class, "createdAt", "created_at", false, 0);
		assertColumn(Task.class, "updatedAt", "updated_at", false, 0);
		assertColumn(Task.class, "deletedAt", "deleted_at", true, 0);
		assertFieldType(Task.class, "startDate", LocalDate.class);
		assertFieldType(Task.class, "dueDate", LocalDate.class);
		assertFieldType(Task.class, "completedAt", OffsetDateTime.class);
		assertFieldType(Task.class, "createdAt", OffsetDateTime.class);
		assertFieldType(Task.class, "updatedAt", OffsetDateTime.class);
		assertFieldType(Task.class, "deletedAt", OffsetDateTime.class);
	}

	@Test
	void taskAssigneeMapsUniqueColumnsEnumsTimestampsAndRelationships() throws NoSuchFieldException {
		assertEntityTable(TaskAssignee.class, "task_assignees");
		assertUniqueConstraint(TaskAssignee.class, "uq_task_assignees", "task_id", "user_id");
		assertId(TaskAssignee.class);
		assertManyToOne(TaskAssignee.class, "task", "task_id", false);
		assertManyToOne(TaskAssignee.class, "user", "user_id", false);
		assertManyToOne(TaskAssignee.class, "assignedBy", "assigned_by", true);
		assertColumn(TaskAssignee.class, "assignedAt", "assigned_at", false, 0);
		assertEnumField(TaskAssignee.class, "status", TaskAssigneeStatus.class);
		assertFieldType(TaskAssignee.class, "assignedAt", OffsetDateTime.class);
	}

	@Test
	void taskReportMapsNullableReviewerReporterColumnsEnumsAndRelationships() throws NoSuchFieldException {
		assertEntityTable(TaskReport.class, "task_reports");
		assertNoUniqueConstraints(TaskReport.class);
		assertId(TaskReport.class);
		assertManyToOne(TaskReport.class, "task", "task_id", false);
		assertManyToOne(TaskReport.class, "reporter", "reporter_id", true);
		assertColumn(TaskReport.class, "content", "content", true, 0);
		assertColumn(TaskReport.class, "resultSummary", "result_summary", true, 0);
		assertEnumField(TaskReport.class, "status", TaskReportStatus.class);
		assertManyToOne(TaskReport.class, "reviewedBy", "reviewed_by", true);
		assertColumn(TaskReport.class, "reviewedAt", "reviewed_at", true, 0);
		assertColumn(TaskReport.class, "feedback", "feedback", true, 0);
		assertColumn(TaskReport.class, "createdAt", "created_at", false, 0);
		assertColumn(TaskReport.class, "updatedAt", "updated_at", false, 0);
		assertFieldType(TaskReport.class, "reviewedAt", OffsetDateTime.class);
		assertFieldType(TaskReport.class, "createdAt", OffsetDateTime.class);
		assertFieldType(TaskReport.class, "updatedAt", OffsetDateTime.class);
	}

	@Test
	void taskAttachmentMapsFileEntityAndUniqueRelationships() throws NoSuchFieldException {
		assertEntityTable(TaskAttachment.class, "task_attachments");
		assertUniqueConstraint(TaskAttachment.class, "uq_task_attachments", "task_id", "file_id");
		assertId(TaskAttachment.class);
		assertManyToOne(TaskAttachment.class, "task", "task_id", false);
		assertManyToOne(TaskAttachment.class, "file", "file_id", false);
		assertFieldType(TaskAttachment.class, "file", File.class);
		assertManyToOne(TaskAttachment.class, "uploadedBy", "uploaded_by", true);
		assertColumn(TaskAttachment.class, "createdAt", "created_at", false, 0);
		assertFieldType(TaskAttachment.class, "createdAt", OffsetDateTime.class);
	}

	@Test
	void memberEvaluationLeavesNullsNotDistinctUniquenessDatabaseManaged() throws NoSuchFieldException {
		assertEntityTable(MemberEvaluation.class, "member_evaluations");
		assertNoUniqueConstraints(MemberEvaluation.class);
		assertId(MemberEvaluation.class);
		assertManyToOne(MemberEvaluation.class, "project", "project_id", false);
		assertManyToOne(MemberEvaluation.class, "member", "member_id", false);
		assertManyToOne(MemberEvaluation.class, "evaluator", "evaluator_id", true);
		assertColumn(MemberEvaluation.class, "evaluationPeriod", "evaluation_period", false, 100);
		assertDecimalColumn(MemberEvaluation.class, "overallScore", "overall_score", true, 5, 2);
		assertColumn(MemberEvaluation.class, "comment", "comment", true, 0);
		assertColumn(MemberEvaluation.class, "evaluatedAt", "evaluated_at", false, 0);
		assertColumn(MemberEvaluation.class, "createdAt", "created_at", false, 0);
		assertColumn(MemberEvaluation.class, "updatedAt", "updated_at", false, 0);
		assertFieldType(MemberEvaluation.class, "overallScore", BigDecimal.class);
		assertFieldType(MemberEvaluation.class, "evaluatedAt", OffsetDateTime.class);
		assertFieldType(MemberEvaluation.class, "createdAt", OffsetDateTime.class);
		assertFieldType(MemberEvaluation.class, "updatedAt", OffsetDateTime.class);
	}

	@Test
	void memberEvaluationDetailMapsScorePrecisionAndUniqueRelationships() throws NoSuchFieldException {
		assertEntityTable(MemberEvaluationDetail.class, "member_evaluation_details");
		assertUniqueConstraint(MemberEvaluationDetail.class, "uq_member_evaluation_details", "evaluation_id", "criteria_id");
		assertId(MemberEvaluationDetail.class);
		assertManyToOne(MemberEvaluationDetail.class, "evaluation", "evaluation_id", false);
		assertManyToOne(MemberEvaluationDetail.class, "criteria", "criteria_id", false);
		assertDecimalColumn(MemberEvaluationDetail.class, "score", "score", false, 5, 2);
		assertColumn(MemberEvaluationDetail.class, "comment", "comment", true, 0);
		assertFieldType(MemberEvaluationDetail.class, "score", BigDecimal.class);
	}

	@Test
	void enumValuesAndDefaultsMatchV6DatabaseChecks() {
		assertEnumNames(TaskPriority.class, "LOW", "MEDIUM", "HIGH", "URGENT");
		assertEnumNames(TaskStatus.class, "TODO", "IN_PROGRESS", "COMPLETED", "OVERDUE", "CANCELLED");
		assertEnumNames(TaskAssigneeStatus.class, "ASSIGNED", "REMOVED");
		assertEnumNames(TaskReportStatus.class, "SUBMITTED", "REVIEWED", "REJECTED");

		assertEquals(TaskPriority.MEDIUM, new Task().getPriority());
		assertEquals(TaskStatus.TODO, new Task().getStatus());
		assertEquals(TaskAssigneeStatus.ASSIGNED, new TaskAssignee().getStatus());
		assertEquals(TaskReportStatus.SUBMITTED, new TaskReport().getStatus());
	}

	@Test
	void relationshipsDoNotDeclareCascadeTypes() throws NoSuchFieldException {
		assertNoCascade(Task.class, "project");
		assertNoCascade(Task.class, "createdBy");
		assertNoCascade(TaskAssignee.class, "task");
		assertNoCascade(TaskAssignee.class, "user");
		assertNoCascade(TaskAssignee.class, "assignedBy");
		assertNoCascade(TaskReport.class, "task");
		assertNoCascade(TaskReport.class, "reporter");
		assertNoCascade(TaskReport.class, "reviewedBy");
		assertNoCascade(TaskAttachment.class, "task");
		assertNoCascade(TaskAttachment.class, "file");
		assertNoCascade(TaskAttachment.class, "uploadedBy");
		assertNoCascade(MemberEvaluation.class, "project");
		assertNoCascade(MemberEvaluation.class, "member");
		assertNoCascade(MemberEvaluation.class, "evaluator");
		assertNoCascade(MemberEvaluationDetail.class, "evaluation");
		assertNoCascade(MemberEvaluationDetail.class, "criteria");
	}

	@Test
	void equalityIsSafeForTransientEntities() {
		assertFalse(new Task().equals(new Task()));
		assertFalse(new TaskAssignee().equals(new TaskAssignee()));
		assertFalse(new TaskReport().equals(new TaskReport()));
		assertFalse(new TaskAttachment().equals(new TaskAttachment()));
		assertFalse(new MemberEvaluation().equals(new MemberEvaluation()));
		assertFalse(new MemberEvaluationDetail().equals(new MemberEvaluationDetail()));
		assertEquals(new Task().hashCode(), new Task().hashCode());
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

	private static void assertDecimalColumn(
			Class<?> entityType,
			String fieldName,
			String columnName,
			boolean nullable,
			int precision,
			int scale) throws NoSuchFieldException {
		Column column = entityType.getDeclaredField(fieldName).getAnnotation(Column.class);
		assertNotNull(column);
		assertEquals(columnName, column.name());
		assertEquals(nullable, column.nullable());
		assertEquals(precision, column.precision());
		assertEquals(scale, column.scale());
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

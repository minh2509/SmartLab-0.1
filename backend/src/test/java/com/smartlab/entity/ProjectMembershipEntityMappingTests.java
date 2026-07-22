package com.smartlab.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.smartlab.enums.ProjectJoinRequestStatus;
import com.smartlab.enums.ProjectMemberRole;
import com.smartlab.enums.ProjectMemberStatus;
import com.smartlab.enums.ProjectStatus;
import com.smartlab.enums.ProjectType;
import com.smartlab.enums.ProjectVisibility;

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

class ProjectMembershipEntityMappingTests {

	@Test
	void projectMapsColumnsEnumsDatesAndRelationships() throws NoSuchFieldException {
		assertEntityTable(Project.class, "projects");
		assertUniqueConstraint(Project.class, "uq_projects_lab_code", "lab_id", "code");
		assertUniqueConstraint(Project.class, "uq_projects_lab_slug", "lab_id", "slug");
		assertId(Project.class);
		assertManyToOne(Project.class, "lab", "lab_id", false);
		assertColumn(Project.class, "code", "code", false, 100);
		assertColumn(Project.class, "name", "name", false, 255);
		assertColumn(Project.class, "slug", "slug", false, 255);
		assertColumn(Project.class, "shortDescription", "short_description", true, 0);
		assertColumn(Project.class, "description", "description", true, 0);
		assertColumn(Project.class, "objective", "objective", true, 0);
		assertEnumField(Project.class, "projectType", ProjectType.class);
		assertEnumField(Project.class, "visibility", ProjectVisibility.class);
		assertEnumField(Project.class, "status", ProjectStatus.class);
		assertColumn(Project.class, "progressPercent", "progress_percent", false, 0);
		assertColumn(Project.class, "startDate", "start_date", true, 0);
		assertColumn(Project.class, "expectedEndDate", "expected_end_date", true, 0);
		assertColumn(Project.class, "actualEndDate", "actual_end_date", true, 0);
		assertManyToOne(Project.class, "coverFile", "cover_file_id", true);
		assertManyToOne(Project.class, "createdBy", "created_by", true);
		assertColumn(Project.class, "createdAt", "created_at", false, 0);
		assertColumn(Project.class, "updatedAt", "updated_at", false, 0);
		assertColumn(Project.class, "deletedAt", "deleted_at", true, 0);
		assertFieldType(Project.class, "startDate", LocalDate.class);
		assertFieldType(Project.class, "expectedEndDate", LocalDate.class);
		assertFieldType(Project.class, "actualEndDate", LocalDate.class);
		assertFieldType(Project.class, "createdAt", OffsetDateTime.class);
		assertFieldType(Project.class, "updatedAt", OffsetDateTime.class);
		assertFieldType(Project.class, "deletedAt", OffsetDateTime.class);
	}

	@Test
	void projectResearchFieldMapsUniqueRequiredLazyRelationships() throws NoSuchFieldException {
		assertEntityTable(ProjectResearchField.class, "project_research_fields");
		assertUniqueConstraint(ProjectResearchField.class, "uq_project_research_fields", "project_id", "research_field_id");
		assertId(ProjectResearchField.class);
		assertManyToOne(ProjectResearchField.class, "project", "project_id", false);
		assertManyToOne(ProjectResearchField.class, "researchField", "research_field_id", false);
	}

	@Test
	void projectMemberMapsColumnsEnumsTimestampsAndRelationships() throws NoSuchFieldException {
		assertEntityTable(ProjectMember.class, "project_members");
		assertUniqueConstraint(ProjectMember.class, "uq_project_members", "project_id", "user_id");
		assertId(ProjectMember.class);
		assertManyToOne(ProjectMember.class, "project", "project_id", false);
		assertManyToOne(ProjectMember.class, "user", "user_id", false);
		assertEnumField(ProjectMember.class, "projectRole", ProjectMemberRole.class);
		assertEnumField(ProjectMember.class, "memberStatus", ProjectMemberStatus.class);
		assertColumn(ProjectMember.class, "joinedAt", "joined_at", false, 0);
		assertColumn(ProjectMember.class, "leftAt", "left_at", true, 0);
		assertManyToOne(ProjectMember.class, "addedBy", "added_by", true);
		assertColumn(ProjectMember.class, "note", "note", true, 0);
		assertFieldType(ProjectMember.class, "joinedAt", OffsetDateTime.class);
		assertFieldType(ProjectMember.class, "leftAt", OffsetDateTime.class);
	}

	@Test
	void projectJoinRequestMapsColumnsEnumsTimestampsAndRelationships() throws NoSuchFieldException {
		assertEntityTable(ProjectJoinRequest.class, "project_join_requests");
		assertNoUniqueConstraints(ProjectJoinRequest.class);
		assertId(ProjectJoinRequest.class);
		assertManyToOne(ProjectJoinRequest.class, "project", "project_id", false);
		assertManyToOne(ProjectJoinRequest.class, "requester", "requester_id", false);
		assertColumn(ProjectJoinRequest.class, "desiredPosition", "desired_position", true, 255);
		assertColumn(ProjectJoinRequest.class, "reason", "reason", true, 0);
		assertColumn(ProjectJoinRequest.class, "skills", "skills", true, 0);
		assertColumn(ProjectJoinRequest.class, "experience", "experience", true, 0);
		assertColumn(ProjectJoinRequest.class, "introduction", "introduction", true, 0);
		assertManyToOne(ProjectJoinRequest.class, "cvFile", "cv_file_id", true);
		assertEnumField(ProjectJoinRequest.class, "status", ProjectJoinRequestStatus.class);
		assertManyToOne(ProjectJoinRequest.class, "reviewedBy", "reviewed_by", true);
		assertColumn(ProjectJoinRequest.class, "reviewedAt", "reviewed_at", true, 0);
		assertColumn(ProjectJoinRequest.class, "rejectionReason", "rejection_reason", true, 0);
		assertColumn(ProjectJoinRequest.class, "createdAt", "created_at", false, 0);
		assertColumn(ProjectJoinRequest.class, "updatedAt", "updated_at", false, 0);
		assertFieldType(ProjectJoinRequest.class, "reviewedAt", OffsetDateTime.class);
		assertFieldType(ProjectJoinRequest.class, "createdAt", OffsetDateTime.class);
		assertFieldType(ProjectJoinRequest.class, "updatedAt", OffsetDateTime.class);
	}

	@Test
	void enumValuesAndDefaultsMatchV5DatabaseChecks() {
		assertEnumNames(ProjectType.class, "PRODUCTION", "RESEARCH");
		assertEnumNames(ProjectVisibility.class, "PUBLIC", "LAB_INTERNAL", "PROJECT_INTERNAL", "PRIVATE");
		assertEnumNames(ProjectStatus.class, "PROPOSED", "PREPARING", "IN_PROGRESS", "PAUSED", "COMPLETED", "CLOSED");
		assertEnumNames(ProjectMemberRole.class, "PROJECT_LEADER", "PROJECT_MEMBER");
		assertEnumNames(ProjectMemberStatus.class, "ACTIVE", "REMOVED", "LEFT");
		assertEnumNames(ProjectJoinRequestStatus.class, "PENDING", "APPROVED", "REJECTED", "CANCELLED");

		assertEquals(ProjectVisibility.PUBLIC, new Project().getVisibility());
		assertEquals(ProjectStatus.PROPOSED, new Project().getStatus());
		assertEquals(0, new Project().getProgressPercent());
		assertEquals(ProjectMemberRole.PROJECT_MEMBER, new ProjectMember().getProjectRole());
		assertEquals(ProjectMemberStatus.ACTIVE, new ProjectMember().getMemberStatus());
		assertEquals(ProjectJoinRequestStatus.PENDING, new ProjectJoinRequest().getStatus());
	}

	@Test
	void relationshipsDoNotDeclareCascadeTypes() throws NoSuchFieldException {
		assertNoCascade(Project.class, "lab");
		assertNoCascade(Project.class, "coverFile");
		assertNoCascade(Project.class, "createdBy");
		assertNoCascade(ProjectResearchField.class, "project");
		assertNoCascade(ProjectResearchField.class, "researchField");
		assertNoCascade(ProjectMember.class, "project");
		assertNoCascade(ProjectMember.class, "user");
		assertNoCascade(ProjectMember.class, "addedBy");
		assertNoCascade(ProjectJoinRequest.class, "project");
		assertNoCascade(ProjectJoinRequest.class, "requester");
		assertNoCascade(ProjectJoinRequest.class, "cvFile");
		assertNoCascade(ProjectJoinRequest.class, "reviewedBy");
	}

	@Test
	void equalityIsSafeForTransientEntities() {
		assertFalse(new Project().equals(new Project()));
		assertFalse(new ProjectResearchField().equals(new ProjectResearchField()));
		assertFalse(new ProjectMember().equals(new ProjectMember()));
		assertFalse(new ProjectJoinRequest().equals(new ProjectJoinRequest()));
		assertEquals(new Project().hashCode(), new Project().hashCode());
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
		assertEquals(!nullable, !manyToOne.optional());
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

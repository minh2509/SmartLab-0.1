package com.smartlab.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.smartlab.enums.CatalogStatus;
import com.smartlab.enums.MemberProfileActivityStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

class ProfileCatalogEntityMappingTests {

	@Test
	void memberProfileMapsColumnsStatusAndUserRelationship() throws NoSuchFieldException {
		assertEntityTable(MemberProfile.class, "member_profiles");
		assertUniqueConstraint(MemberProfile.class, "uq_member_profiles_user", "user_id");
		assertId(MemberProfile.class);
		assertOneToOne(MemberProfile.class, "user", "user_id", false, false);
		assertColumn(MemberProfile.class, "studentCode", "student_code", true, 100);
		assertColumn(MemberProfile.class, "phone", "phone", true, 50);
		assertColumn(MemberProfile.class, "personalEmail", "personal_email", true, 255);
		assertColumn(MemberProfile.class, "bio", "bio", true, 0);
		assertColumn(MemberProfile.class, "specialization", "specialization", true, 255);
		assertColumn(MemberProfile.class, "joinedAt", "joined_at", true, 0);
		assertColumn(MemberProfile.class, "githubUrl", "github_url", true, 255);
		assertColumn(MemberProfile.class, "linkedinUrl", "linkedin_url", true, 255);
		assertColumn(MemberProfile.class, "portfolioUrl", "portfolio_url", true, 255);
		assertColumn(MemberProfile.class, "createdAt", "created_at", false, 0);
		assertColumn(MemberProfile.class, "updatedAt", "updated_at", false, 0);
		assertEquals(LocalDate.class, MemberProfile.class.getDeclaredField("joinedAt").getType());
		assertEnumField(MemberProfile.class, "activityStatus", MemberProfileActivityStatus.class);
	}

	@Test
	void researchFieldMapsCatalogColumnsAndSharedStatus() throws NoSuchFieldException {
		assertEntityTable(ResearchField.class, "research_fields");
		assertId(ResearchField.class);
		assertColumn(ResearchField.class, "code", "code", false, 50);
		assertColumn(ResearchField.class, "name", "name", false, 150);
		assertColumn(ResearchField.class, "description", "description", true, 0);
		assertColumn(ResearchField.class, "createdAt", "created_at", false, 0);
		assertEnumField(ResearchField.class, "status", CatalogStatus.class);
	}

	@Test
	void postCategoryMapsCatalogColumnsAndSharedStatus() throws NoSuchFieldException {
		assertEntityTable(PostCategory.class, "post_categories");
		assertId(PostCategory.class);
		assertColumn(PostCategory.class, "code", "code", false, 100);
		assertColumn(PostCategory.class, "name", "name", false, 150);
		assertColumn(PostCategory.class, "description", "description", true, 0);
		assertColumn(PostCategory.class, "createdAt", "created_at", false, 0);
		assertEnumField(PostCategory.class, "status", CatalogStatus.class);
	}

	@Test
	void evaluationCriteriaMapsCatalogColumnsScoreAndSharedStatus() throws NoSuchFieldException {
		assertEntityTable(EvaluationCriteria.class, "evaluation_criteria");
		assertId(EvaluationCriteria.class);
		assertColumn(EvaluationCriteria.class, "code", "code", false, 100);
		assertColumn(EvaluationCriteria.class, "name", "name", false, 255);
		assertColumn(EvaluationCriteria.class, "description", "description", true, 0);
		assertColumn(EvaluationCriteria.class, "maxScore", "max_score", false, 0);
		assertColumn(EvaluationCriteria.class, "createdAt", "created_at", false, 0);
		assertEquals(Integer.class, EvaluationCriteria.class.getDeclaredField("maxScore").getType());
		assertEnumField(EvaluationCriteria.class, "status", CatalogStatus.class);
	}

	@Test
	void memberResearchFieldMapsRequiredLazyRelationships() throws NoSuchFieldException {
		assertEntityTable(MemberResearchField.class, "member_research_fields");
		assertUniqueConstraint(MemberResearchField.class, "uq_member_research_fields", "member_profile_id", "research_field_id");
		assertId(MemberResearchField.class);
		assertManyToOne(MemberResearchField.class, "memberProfile", "member_profile_id", false, false);
		assertManyToOne(MemberResearchField.class, "researchField", "research_field_id", false, false);
	}

	@Test
	void enumValuesAndJavaDefaultsMatchDatabaseChecks() {
		assertEnumNames(MemberProfileActivityStatus.class, "ACTIVE", "INACTIVE", "ALUMNI");
		assertEnumNames(CatalogStatus.class, "ACTIVE", "INACTIVE");

		assertEquals(MemberProfileActivityStatus.ACTIVE, new MemberProfile().getActivityStatus());
		assertEquals(CatalogStatus.ACTIVE, new ResearchField().getStatus());
		assertEquals(CatalogStatus.ACTIVE, new PostCategory().getStatus());
		assertEquals(CatalogStatus.ACTIVE, new EvaluationCriteria().getStatus());
		assertEquals(10, new EvaluationCriteria().getMaxScore());
	}

	@Test
	void equalityIsSafeForTransientEntities() {
		assertFalse(new MemberProfile().equals(new MemberProfile()));
		assertFalse(new ResearchField().equals(new ResearchField()));
		assertFalse(new MemberResearchField().equals(new MemberResearchField()));
		assertFalse(new PostCategory().equals(new PostCategory()));
		assertFalse(new EvaluationCriteria().equals(new EvaluationCriteria()));
		assertEquals(new MemberProfile().hashCode(), new MemberProfile().hashCode());
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
			boolean nullable,
			boolean optional) throws NoSuchFieldException {
		Field field = entityType.getDeclaredField(fieldName);
		ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
		assertNotNull(manyToOne);
		assertEquals(FetchType.LAZY, manyToOne.fetch());
		assertEquals(optional, manyToOne.optional());
		JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
		assertNotNull(joinColumn);
		assertEquals(columnName, joinColumn.name());
		assertEquals(nullable, joinColumn.nullable());
	}

	private static void assertOneToOne(
			Class<?> entityType,
			String fieldName,
			String columnName,
			boolean nullable,
			boolean optional) throws NoSuchFieldException {
		Field field = entityType.getDeclaredField(fieldName);
		OneToOne oneToOne = field.getAnnotation(OneToOne.class);
		assertNotNull(oneToOne);
		assertEquals(FetchType.LAZY, oneToOne.fetch());
		assertEquals(optional, oneToOne.optional());
		JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
		assertNotNull(joinColumn);
		assertEquals(columnName, joinColumn.name());
		assertEquals(nullable, joinColumn.nullable());
	}

	private static void assertUniqueConstraint(Class<?> entityType, String constraintName, String... columnNames) {
		UniqueConstraint[] uniqueConstraints = entityType.getAnnotation(Table.class).uniqueConstraints();
		boolean hasConstraint = Arrays.stream(uniqueConstraints)
				.anyMatch(constraint ->
						constraint.name().equals(constraintName)
								&& Arrays.equals(constraint.columnNames(), columnNames));
		assertTrue(hasConstraint);
	}

	private static <E extends Enum<E>> void assertEnumNames(Class<E> enumType, String... expectedNames) {
		assertEquals(Arrays.asList(expectedNames), Arrays.stream(enumType.getEnumConstants()).map(Enum::name).toList());
	}
}

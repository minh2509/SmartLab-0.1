package com.smartlab.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.EvaluationCriteria;
import com.smartlab.entity.MemberProfile;
import com.smartlab.entity.MemberResearchField;
import com.smartlab.entity.PostCategory;
import com.smartlab.entity.ResearchField;
import com.smartlab.entity.User;

class ProfileCatalogRepositoryTests {

	@Test
	void repositoriesUseUuidJpaRepositoryContracts() {
		assertJpaRepository(MemberProfileRepository.class);
		assertJpaRepository(ResearchFieldRepository.class);
		assertJpaRepository(MemberResearchFieldRepository.class);
		assertJpaRepository(PostCategoryRepository.class);
		assertJpaRepository(EvaluationCriteriaRepository.class);
	}

	@Test
	void repositoriesExposeFocusedProfileAndCatalogQueries() throws NoSuchMethodException {
		assertReturnType(MemberProfileRepository.class.getMethod("findByUser", User.class), Optional.class);
		assertReturnType(ResearchFieldRepository.class.getMethod("findByCode", String.class), Optional.class);
		assertReturnType(PostCategoryRepository.class.getMethod("findByCode", String.class), Optional.class);
		assertReturnType(EvaluationCriteriaRepository.class.getMethod("findByCode", String.class), Optional.class);
		assertReturnType(
				MemberResearchFieldRepository.class.getMethod(
						"existsByMemberProfileAndResearchField",
						MemberProfile.class,
						ResearchField.class),
				boolean.class);
		assertReturnType(
				MemberResearchFieldRepository.class.getMethod("findByMemberProfile", MemberProfile.class),
				List.class);
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
		if (repositoryType == MemberProfileRepository.class) {
			return MemberProfile.class.getName();
		}
		if (repositoryType == ResearchFieldRepository.class) {
			return ResearchField.class.getName();
		}
		if (repositoryType == MemberResearchFieldRepository.class) {
			return MemberResearchField.class.getName();
		}
		if (repositoryType == PostCategoryRepository.class) {
			return PostCategory.class.getName();
		}
		if (repositoryType == EvaluationCriteriaRepository.class) {
			return EvaluationCriteria.class.getName();
		}
		throw new IllegalArgumentException("Unsupported repository type: " + repositoryType.getName());
	}

	private static void assertReturnType(Method method, Class<?> returnType) {
		assertEquals(returnType, method.getReturnType());
	}
}

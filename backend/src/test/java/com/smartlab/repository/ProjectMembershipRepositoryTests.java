package com.smartlab.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Lab;
import com.smartlab.entity.Project;
import com.smartlab.entity.ProjectJoinRequest;
import com.smartlab.entity.ProjectMember;
import com.smartlab.entity.ProjectResearchField;
import com.smartlab.entity.ResearchField;
import com.smartlab.entity.User;
import com.smartlab.enums.ProjectJoinRequestStatus;
import com.smartlab.enums.ProjectMemberStatus;
import com.smartlab.enums.ProjectStatus;
import com.smartlab.enums.ProjectType;
import com.smartlab.enums.ProjectVisibility;

class ProjectMembershipRepositoryTests {

	@Test
	void repositoriesUseUuidJpaRepositoryContracts() {
		assertJpaRepository(ProjectRepository.class);
		assertJpaRepository(ProjectResearchFieldRepository.class);
		assertJpaRepository(ProjectMemberRepository.class);
		assertJpaRepository(ProjectJoinRequestRepository.class);
	}

	@Test
	void projectRepositoryExposesFocusedQueries() throws NoSuchMethodException {
		assertReturnType(ProjectRepository.class.getMethod("findByLabAndCode", Lab.class, String.class), Optional.class);
		assertReturnType(ProjectRepository.class.getMethod("findByLabAndSlug", Lab.class, String.class), Optional.class);
		assertReturnType(ProjectRepository.class.getMethod("existsByLabAndCode", Lab.class, String.class), boolean.class);
		assertReturnType(ProjectRepository.class.getMethod("existsByLabAndSlug", Lab.class, String.class), boolean.class);
		assertReturnType(ProjectRepository.class.getMethod("findByLabAndStatus", Lab.class, ProjectStatus.class), List.class);
		assertReturnType(
				ProjectRepository.class.getMethod("findByIdAndLabAndDeletedAtIsNull", UUID.class, Lab.class),
				Optional.class);
		assertReturnType(
				ProjectRepository.class.getMethod(
						"findAdminProjects",
						Lab.class,
						Collection.class,
						Collection.class,
						Collection.class,
						String.class,
						Pageable.class),
				Page.class);
	}

	@Test
	void associationAndMembershipRepositoriesExposeFocusedQueries() throws NoSuchMethodException {
		assertReturnType(
				ProjectResearchFieldRepository.class.getMethod(
						"existsByProjectAndResearchField",
						Project.class,
						ResearchField.class),
				boolean.class);
		assertReturnType(ProjectResearchFieldRepository.class.getMethod("findByProject", Project.class), List.class);
		assertReturnType(ProjectMemberRepository.class.getMethod("existsByProjectAndUser", Project.class, User.class), boolean.class);
		assertReturnType(ProjectMemberRepository.class.getMethod("findByProject", Project.class), List.class);
		assertReturnType(ProjectMemberRepository.class.getMethod("findByUser", User.class), List.class);
		assertReturnType(
				ProjectMemberRepository.class.getMethod(
						"findByProjectAndMemberStatus",
						Project.class,
						ProjectMemberStatus.class),
				List.class);
		assertReturnType(
				ProjectMemberRepository.class.getMethod(
						"findByProjectInAndMemberStatus",
						Collection.class,
						ProjectMemberStatus.class),
				List.class);
		assertReturnType(
				ProjectResearchFieldRepository.class.getMethod("findByProjectIn", Collection.class),
				List.class);
	}

	@Test
	void joinRequestRepositoryExposesFocusedQueries() throws NoSuchMethodException {
		assertReturnType(
				ProjectJoinRequestRepository.class.getMethod("findByProjectAndRequester", Project.class, User.class),
				Optional.class);
		assertReturnType(
				ProjectJoinRequestRepository.class.getMethod("findByProjectAndStatus", Project.class, ProjectJoinRequestStatus.class),
				List.class);
		assertReturnType(
				ProjectJoinRequestRepository.class.getMethod("findByRequesterAndStatus", User.class, ProjectJoinRequestStatus.class),
				List.class);
		assertReturnType(
				ProjectJoinRequestRepository.class.getMethod(
						"existsByProjectAndRequesterAndStatus",
						Project.class,
						User.class,
						ProjectJoinRequestStatus.class),
				boolean.class);
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
		if (repositoryType == ProjectRepository.class) {
			return Project.class.getName();
		}
		if (repositoryType == ProjectResearchFieldRepository.class) {
			return ProjectResearchField.class.getName();
		}
		if (repositoryType == ProjectMemberRepository.class) {
			return ProjectMember.class.getName();
		}
		if (repositoryType == ProjectJoinRequestRepository.class) {
			return ProjectJoinRequest.class.getName();
		}
		throw new IllegalArgumentException("Unsupported repository type: " + repositoryType.getName());
	}

	private static void assertReturnType(Method method, Class<?> returnType) {
		assertEquals(returnType, method.getReturnType());
	}
}

package com.smartlab.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.File;
import com.smartlab.entity.Lab;
import com.smartlab.entity.Post;
import com.smartlab.entity.PostAttachment;
import com.smartlab.entity.PostCategory;
import com.smartlab.entity.PostModerationLog;
import com.smartlab.entity.Project;
import com.smartlab.entity.User;
import com.smartlab.enums.PostModerationAction;
import com.smartlab.enums.PostStatus;
import com.smartlab.enums.PostVisibility;

class ContentPersistenceRepositoryTests {

	@Test
	void repositoriesUseUuidJpaRepositoryContracts() {
		assertJpaRepository(PostRepository.class);
		assertJpaRepository(PostModerationLogRepository.class);
		assertJpaRepository(PostAttachmentRepository.class);
	}

	@Test
	void postRepositoryExposesFocusedQueries() throws NoSuchMethodException {
		assertReturnType(PostRepository.class.getMethod("findByLabAndSlug", Lab.class, String.class), Optional.class);
		assertReturnType(PostRepository.class.getMethod("existsByLabAndSlug", Lab.class, String.class), boolean.class);
		assertReturnType(PostRepository.class.getMethod("findByLab", Lab.class), List.class);
		assertReturnType(
				PostRepository.class.getMethod("findByLabAndModerationStatus", Lab.class, PostStatus.class),
				List.class);
		assertReturnType(
				PostRepository.class.getMethod("findByLabAndVisibility", Lab.class, PostVisibility.class),
				List.class);
		assertReturnType(PostRepository.class.getMethod("findByProject", Project.class), List.class);
		assertReturnType(
				PostRepository.class.getMethod("findByProjectAndModerationStatus", Project.class, PostStatus.class),
				List.class);
		assertReturnType(PostRepository.class.getMethod("findByAuthor", User.class), List.class);
		assertReturnType(PostRepository.class.getMethod("findByCategory", PostCategory.class), List.class);
		assertReturnType(
				PostRepository.class.getMethod("findByLabAndPublishedAtIsNotNullOrderByPublishedAtDesc", Lab.class),
				List.class);
	}

	@Test
	void moderationLogRepositoryExposesFocusedQueries() throws NoSuchMethodException {
		assertReturnType(
				PostModerationLogRepository.class.getMethod("findByPostOrderByCreatedAtAsc", Post.class),
				List.class);
		assertReturnType(
				PostModerationLogRepository.class.getMethod("findByPostOrderByCreatedAtDesc", Post.class),
				List.class);
		assertReturnType(PostModerationLogRepository.class.getMethod("findByActor", User.class), List.class);
		assertReturnType(
				PostModerationLogRepository.class.getMethod("findByPostAndAction", Post.class, PostModerationAction.class),
				List.class);
	}

	@Test
	void postAttachmentRepositoryExposesFocusedQueries() throws NoSuchMethodException {
		assertReturnType(PostAttachmentRepository.class.getMethod("existsByPostAndFile", Post.class, File.class), boolean.class);
		assertReturnType(PostAttachmentRepository.class.getMethod("findByPost", Post.class), List.class);
		assertReturnType(PostAttachmentRepository.class.getMethod("findByFile", File.class), List.class);
		assertReturnType(PostAttachmentRepository.class.getMethod("findByUploadedBy", User.class), List.class);
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
		if (repositoryType == PostRepository.class) {
			return Post.class.getName();
		}
		if (repositoryType == PostModerationLogRepository.class) {
			return PostModerationLog.class.getName();
		}
		if (repositoryType == PostAttachmentRepository.class) {
			return PostAttachment.class.getName();
		}
		throw new IllegalArgumentException("Unsupported repository type: " + repositoryType.getName());
	}

	private static void assertReturnType(Method method, Class<?> returnType) {
		assertEquals(returnType, method.getReturnType());
	}
}

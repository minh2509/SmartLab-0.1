package com.smartlab.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smartlab.entity.File;
import com.smartlab.entity.Lab;
import com.smartlab.entity.Post;
import com.smartlab.entity.PostAttachment;
import com.smartlab.entity.PostCategory;
import com.smartlab.entity.PostModerationLog;
import com.smartlab.entity.Project;
import com.smartlab.entity.User;
import com.smartlab.enums.PostContentType;
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
		assertReturnType(
				PostRepository.class.getMethod(
						"findAdminPosts",
						Lab.class,
						String.class,
						PostStatus.class,
						PostContentType.class,
						UUID.class,
						UUID.class,
						PostVisibility.class,
						Pageable.class),
				Page.class);
		assertReturnType(
				PostRepository.class.getMethod(
						"findPendingAdminPostIds",
						UUID.class,
						Pageable.class),
				Page.class);
		assertReturnType(
				PostRepository.class.getMethod("findAdminPostsByIdIn", Collection.class),
				List.class);
	}

	@Test
	void adminPostListQueryIsLabScopedSoftDeleteFilteredAndStableOrdered() throws NoSuchMethodException {
		Method method = PostRepository.class.getMethod(
				"findAdminPosts",
				Lab.class,
				String.class,
				PostStatus.class,
				PostContentType.class,
				UUID.class,
				UUID.class,
				PostVisibility.class,
				Pageable.class);
		Query query = method.getAnnotation(Query.class);
		EntityGraph entityGraph = method.getAnnotation(EntityGraph.class);

		assertTrue(query.value().contains("post.lab = :lab"));
		assertTrue(query.value().contains("post.deletedAt is null"));
		assertTrue(query.value().contains(":keywordPattern is null"));
		assertTrue(query.value().contains("lower(post.title) like :keywordPattern escape '!'"));
		assertTrue(query.value().contains("lower(post.slug) like :keywordPattern escape '!'"));
		assertTrue(query.value().contains("lower(post.summary) like :keywordPattern escape '!'"));
		assertFalse(query.value().contains("lower(concat"));
		assertFalse(query.value().contains("lower(:keywordPattern"));
		assertFalse(query.value().contains("lower(post.content)"));
		assertFalse(query.value().contains("post.reviewNote"));
		assertTrue(query.value().contains(":status is null or post.moderationStatus = :status"));
		assertTrue(query.value().contains(":contentType is null or post.contentType = :contentType"));
		assertTrue(query.value().contains(":authorId is null or post.author.id = :authorId"));
		assertTrue(query.value().contains(":projectId is null or post.project.id = :projectId"));
		assertTrue(query.value().contains(":visibility is null or post.visibility = :visibility"));
		assertTrue(query.value().contains("order by post.createdAt desc, post.id desc"));
		assertTrue(query.countQuery().contains("post.lab = :lab"));
		assertTrue(query.countQuery().contains("post.deletedAt is null"));
		assertTrue(query.countQuery().contains(":keywordPattern is null"));
		assertTrue(query.countQuery().contains("lower(post.title) like :keywordPattern escape '!'"));
		assertTrue(query.countQuery().contains("lower(post.slug) like :keywordPattern escape '!'"));
		assertTrue(query.countQuery().contains("lower(post.summary) like :keywordPattern escape '!'"));
		assertFalse(query.countQuery().contains("lower(concat"));
		assertFalse(query.countQuery().contains("lower(:keywordPattern"));
		assertFalse(query.countQuery().contains("lower(post.content)"));
		assertFalse(query.countQuery().contains("post.reviewNote"));
		assertTrue(query.countQuery().contains(":status is null or post.moderationStatus = :status"));
		assertTrue(query.countQuery().contains(":contentType is null or post.contentType = :contentType"));
		assertTrue(query.countQuery().contains(":authorId is null or post.author.id = :authorId"));
		assertTrue(query.countQuery().contains(":projectId is null or post.project.id = :projectId"));
		assertTrue(query.countQuery().contains(":visibility is null or post.visibility = :visibility"));
		assertEquals(
				List.of("author", "project", "category", "coverFile"),
				List.of(entityGraph.attributePaths()));
	}

	@Test
	void adminPendingPostQueueUsesPostgresOrderedIdQueryAndEntityGraphFetch() throws NoSuchMethodException {
		Method idMethod = PostRepository.class.getMethod(
				"findPendingAdminPostIds",
				UUID.class,
				Pageable.class);
		Query idQuery = idMethod.getAnnotation(Query.class);

		assertTrue(idQuery.nativeQuery());
		assertTrue(idQuery.value().contains("select post.id"));
		assertTrue(idQuery.value().contains("from posts post"));
		assertTrue(idQuery.value().contains("left join ("));
		assertTrue(idQuery.value().contains("select log.post_id, max(log.created_at) as latest_submitted_at"));
		assertTrue(idQuery.value().contains("from post_moderation_logs log"));
		assertTrue(idQuery.value().contains("where log.action = 'SUBMIT'"));
		assertTrue(idQuery.value().contains("group by log.post_id"));
		assertTrue(idQuery.value().contains("latest_submit on latest_submit.post_id = post.id"));
		assertTrue(idQuery.value().contains("post.lab_id = :labId"));
		assertTrue(idQuery.value().contains("post.deleted_at is null"));
		assertTrue(idQuery.value().contains("post.moderation_status = 'PENDING_REVIEW'"));
		assertTrue(idQuery.value().contains(
				"order by latest_submit.latest_submitted_at asc nulls last, post.id asc"));
		assertFalse(idQuery.value().contains("post.created_at"));
		assertFalse(idQuery.value().contains("post.updated_at"));
		assertTrue(idQuery.countQuery().contains("select count(*)"));
		assertTrue(idQuery.countQuery().contains("from posts post"));
		assertTrue(idQuery.countQuery().contains("post.lab_id = :labId"));
		assertTrue(idQuery.countQuery().contains("post.deleted_at is null"));
		assertTrue(idQuery.countQuery().contains("post.moderation_status = 'PENDING_REVIEW'"));
		assertFalse(idQuery.countQuery().contains("post_moderation_logs"));

		Method fetchMethod = PostRepository.class.getMethod("findAdminPostsByIdIn", Collection.class);
		Query fetchQuery = fetchMethod.getAnnotation(Query.class);
		EntityGraph entityGraph = fetchMethod.getAnnotation(EntityGraph.class);
		assertTrue(fetchQuery.value().contains("select post from Post post where post.id in :ids"));
		assertEquals(
				List.of("author", "project", "category", "coverFile"),
				List.of(entityGraph.attributePaths()));
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

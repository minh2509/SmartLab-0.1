package com.smartlab.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.File;
import com.smartlab.entity.Lab;
import com.smartlab.entity.Post;
import com.smartlab.entity.PostCategory;
import com.smartlab.entity.Project;
import com.smartlab.entity.Role;
import com.smartlab.entity.User;
import com.smartlab.entity.UserRole;
import com.smartlab.enums.PostContentType;
import com.smartlab.enums.PostStatus;
import com.smartlab.enums.PostVisibility;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;
import com.smartlab.exception.ForbiddenAdminOperationException;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.mapper.AdminPostApiMapper;
import com.smartlab.repository.PostRepository;
import com.smartlab.repository.RoleRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.UserRoleRepository;

class AdminPostServiceTests {

	private final PostRepository postRepository = mock(PostRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final RoleRepository roleRepository = mock(RoleRepository.class);
	private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
	private final AdminRolePolicy rolePolicy = new AdminRolePolicy(userRepository, roleRepository, userRoleRepository);
	private final AdminPostService service = new AdminPostService(
			postRepository,
			rolePolicy,
			new AdminPostApiMapper());

	@Test
	void listPostsRejectsInvalidActorOrActorWithoutCurrentAdminRole() {
		UUID missingActorId = UUID.randomUUID();
		when(userRepository.findById(missingActorId)).thenReturn(Optional.empty());

		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.listPosts(new AdminPostService.ListAdminPostsQuery(
						missingActorId,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null)));

		Lab lab = lab(UUID.randomUUID());
		User memberActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(memberActor, role(UUID.randomUUID(), AdminRolePolicy.MEMBER_ROLE_CODE));

		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.listPosts(new AdminPostService.ListAdminPostsQuery(
						memberActor.getId(),
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null)));
	}

	@Test
	void listPostsUsesActorLabDefaultsPaginationAndPassesFiltersToRepository() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		UUID authorId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		when(postRepository.findAdminPosts(
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(Pageable.class)))
						.thenReturn(Page.empty());

		service.listPosts(new AdminPostService.ListAdminPostsQuery(
				actor.getId(),
				null,
				null,
				"  robotics  ",
				PostStatus.PENDING_REVIEW,
				PostContentType.NEWS,
				authorId,
				projectId,
				PostVisibility.LAB_INTERNAL));

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(postRepository).findAdminPosts(
				org.mockito.Mockito.eq(lab),
				org.mockito.Mockito.eq("%robotics%"),
				org.mockito.Mockito.eq(PostStatus.PENDING_REVIEW),
				org.mockito.Mockito.eq(PostContentType.NEWS),
				org.mockito.Mockito.eq(authorId),
				org.mockito.Mockito.eq(projectId),
				org.mockito.Mockito.eq(PostVisibility.LAB_INTERNAL),
				pageableCaptor.capture());
		assertEquals(0, pageableCaptor.getValue().getPageNumber());
		assertEquals(20, pageableCaptor.getValue().getPageSize());
	}

	@Test
	void listPostsRejectsPageSizeAboveOneHundred() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));

		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.listPosts(new AdminPostService.ListAdminPostsQuery(
						actor.getId(),
						0,
						101,
						null,
						null,
						null,
						null,
						null,
						null)));
	}

	@Test
	void listPostsRejectsNegativePageAndZeroPageSize() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));

		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.listPosts(new AdminPostService.ListAdminPostsQuery(
						actor.getId(),
						-1,
						20,
						null,
						null,
						null,
						null,
						null,
						null)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.listPosts(new AdminPostService.ListAdminPostsQuery(
						actor.getId(),
						0,
						0,
						null,
						null,
						null,
						null,
						null,
						null)));
	}

	@Test
	void listPostsLowercasesMixedCaseKeywordPatternForCaseInsensitiveSearch() {
		assertKeywordPattern("  RoBoTiCs  ", "%robotics%");
	}

	@Test
	void listPostsEscapesKeywordLikeMetacharactersAsLiterals() {
		assertKeywordPattern("%", "%!%%");
		assertKeywordPattern("_", "%!_%");
		assertKeywordPattern("!", "%!!%");
		assertKeywordPattern("A!_%*", "%a!!!_!%*%");
	}

	@Test
	void listPostsTreatsNullKeywordAsAbsent() {
		assertKeywordPattern(null, null);
	}

	@Test
	void listPostsTreatsBlankKeywordAsAbsent() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		when(postRepository.findAdminPosts(
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(Pageable.class)))
						.thenReturn(Page.empty());

		service.listPosts(new AdminPostService.ListAdminPostsQuery(
				actor.getId(),
				0,
				20,
				"   ",
				null,
				null,
				null,
				null,
				null));

		verify(postRepository).findAdminPosts(
				org.mockito.Mockito.eq(lab),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.isNull(),
				any(Pageable.class));
	}

	@Test
	void listPostsMapsRepositoryPageWithoutFullContentOrSensitiveFields() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.SUPER_ADMIN_ROLE_CODE));
		Post post = post(lab);
		when(postRepository.findAdminPosts(
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(Pageable.class)))
						.thenReturn(new PageImpl<>(List.of(post), Pageable.ofSize(20), 1));

		var response = service.listPosts(new AdminPostService.ListAdminPostsQuery(
				actor.getId(),
				0,
				20,
				null,
				null,
				null,
				null,
				null,
				null));

		assertEquals(1, response.totalElements());
		assertEquals(post.getId(), response.content().getFirst().id());
		assertEquals("Author Name", response.content().getFirst().authorName());
		assertEquals("Project Name", response.content().getFirst().projectName());
		assertEquals("Category Name", response.content().getFirst().categoryName());
		assertEquals(post.getCoverFile().getId(), response.content().getFirst().coverFileId());
		assertFalse(java.util.Arrays.stream(response.content().getFirst().getClass().getRecordComponents())
				.anyMatch(component -> List.of("content", "email", "passwordHash", "reviewNote")
						.contains(component.getName())));
	}

	@Test
	void listPendingPostsRejectsNullQuery() {
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.listPendingPosts(null));
	}

	@Test
	void listPendingPostsRejectsMissingActorOrActorWithoutCurrentAdminRole() {
		UUID missingActorId = UUID.randomUUID();
		when(userRepository.findById(missingActorId)).thenReturn(Optional.empty());

		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.listPendingPosts(new AdminPostService.ListPendingAdminPostsQuery(
						missingActorId,
						null,
						null)));

		Lab lab = lab(UUID.randomUUID());
		User memberActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(memberActor, role(UUID.randomUUID(), AdminRolePolicy.MEMBER_ROLE_CODE));

		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.listPendingPosts(new AdminPostService.ListPendingAdminPostsQuery(
						memberActor.getId(),
						null,
						null)));
	}

	@Test
	void listPendingPostsAcceptsAdminAndUsesActorLabDefaultsPagination() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		when(postRepository.findPendingAdminPostIds(any(), any(Pageable.class)))
				.thenReturn(Page.empty(PageRequest.of(0, 20)));

		var response = service.listPendingPosts(new AdminPostService.ListPendingAdminPostsQuery(
				actor.getId(),
				null,
				null));

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(postRepository).findPendingAdminPostIds(
				org.mockito.Mockito.eq(lab.getId()),
				pageableCaptor.capture());
		verify(postRepository, never()).findPendingAdminPostsByIdIn(any(), any());
		assertEquals(0, pageableCaptor.getValue().getPageNumber());
		assertEquals(20, pageableCaptor.getValue().getPageSize());
		assertEquals(0, response.page());
		assertEquals(20, response.size());
	}

	@Test
	void listPendingPostsAcceptsSuperAdmin() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.SUPER_ADMIN_ROLE_CODE));
		when(postRepository.findPendingAdminPostIds(any(), any(Pageable.class)))
				.thenReturn(Page.empty(PageRequest.of(0, 20)));

		service.listPendingPosts(new AdminPostService.ListPendingAdminPostsQuery(
				actor.getId(),
				0,
				20));

		verify(postRepository).findPendingAdminPostIds(org.mockito.Mockito.eq(lab.getId()), any(Pageable.class));
	}

	@Test
	void listPendingPostsRejectsInvalidPageAndSize() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));

		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.listPendingPosts(new AdminPostService.ListPendingAdminPostsQuery(
						actor.getId(),
						-1,
						20)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.listPendingPosts(new AdminPostService.ListPendingAdminPostsQuery(
						actor.getId(),
						0,
						0)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.listPendingPosts(new AdminPostService.ListPendingAdminPostsQuery(
						actor.getId(),
						0,
						101)));
	}

	@Test
	void listPendingPostsReordersFetchedPostsAndKeepsIdPageTotals() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000002");
		Post firstPost = post(lab, firstId);
		Post secondPost = post(lab, secondId);
		PageRequest pageable = PageRequest.of(1, 2);
		when(postRepository.findPendingAdminPostIds(any(), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(secondId, firstId), pageable, 7));
		when(postRepository.findPendingAdminPostsByIdIn(lab.getId(), List.of(secondId, firstId)))
				.thenReturn(List.of(firstPost, secondPost));

		var response = service.listPendingPosts(new AdminPostService.ListPendingAdminPostsQuery(
				actor.getId(),
				1,
				2));

		verify(postRepository).findPendingAdminPostIds(org.mockito.Mockito.eq(lab.getId()), any(Pageable.class));
		verify(postRepository).findPendingAdminPostsByIdIn(lab.getId(), List.of(secondId, firstId));
		assertEquals(2, response.content().size());
		assertEquals(secondId, response.content().get(0).id());
		assertEquals(firstId, response.content().get(1).id());
		assertEquals(1, response.page());
		assertEquals(2, response.size());
		assertEquals(7, response.totalElements());
		assertEquals(4, response.totalPages());
	}

	@Test
	void listPendingPostsThrowsWhenFetchedPostsDoNotCoverOrderedIdPage() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000002");
		PageRequest pageable = PageRequest.of(0, 2);
		when(postRepository.findPendingAdminPostIds(any(), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(firstId, secondId), pageable, 2));
		when(postRepository.findPendingAdminPostsByIdIn(lab.getId(), List.of(firstId, secondId)))
				.thenReturn(List.of(post(lab, firstId)));

		IllegalStateException exception = assertThrows(
				IllegalStateException.class,
				() -> service.listPendingPosts(new AdminPostService.ListPendingAdminPostsQuery(
						actor.getId(),
						0,
						2)));

		assertEquals("Pending admin post page fetch returned incomplete results.", exception.getMessage());
	}

	@Test
	void mapperHandlesNullOptionalRelationsAndPublishedAt() {
		Post post = new Post();
		post.setId(UUID.randomUUID());
		post.setTitle("Post Title");
		post.setSlug("post-title");
		post.setSummary("Short summary");
		post.setContentType(PostContentType.NEWS);
		post.setVisibility(PostVisibility.PUBLIC);
		post.setModerationStatus(PostStatus.DRAFT);
		post.setPublishedAt(null);
		post.setCreatedAt(OffsetDateTime.parse("2026-07-19T10:15:30Z"));
		post.setUpdatedAt(OffsetDateTime.parse("2026-07-21T10:15:30Z"));

		var response = new AdminPostApiMapper().toSummaryResponse(post);

		assertEquals(post.getId(), response.id());
		assertNull(response.authorId());
		assertNull(response.authorName());
		assertNull(response.projectId());
		assertNull(response.projectName());
		assertNull(response.categoryId());
		assertNull(response.categoryName());
		assertNull(response.coverFileId());
		assertNull(response.publishedAt());
	}

	@Test
	void serviceStructureUsesReadOnlyTransaction() throws NoSuchMethodException {
		assertNotNull(AdminPostService.class.getAnnotation(Service.class));
		assertReadOnlyTransaction(AdminPostService.class.getMethod(
				"listPosts",
				AdminPostService.ListAdminPostsQuery.class));
		assertReadOnlyTransaction(AdminPostService.class.getMethod(
				"listPendingPosts",
				AdminPostService.ListPendingAdminPostsQuery.class));
	}

	private void stubActiveActor(User actor, Role role) {
		when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
		when(userRoleRepository.findByUserAndStatus(actor, UserRoleStatus.ACTIVE))
				.thenReturn(List.of(userRole(actor, role)));
	}

	private void assertKeywordPattern(String keyword, String expectedPattern) {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		when(postRepository.findAdminPosts(
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(Pageable.class)))
						.thenReturn(Page.empty());

		service.listPosts(new AdminPostService.ListAdminPostsQuery(
				actor.getId(),
				0,
				20,
				keyword,
				null,
				null,
				null,
				null,
				null));

		ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
		verify(postRepository).findAdminPosts(
				org.mockito.Mockito.eq(lab),
				keywordCaptor.capture(),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.isNull(),
				any(Pageable.class));
		assertEquals(expectedPattern, keywordCaptor.getValue());
		org.mockito.Mockito.clearInvocations(postRepository, userRepository, roleRepository, userRoleRepository);
	}

	private static void assertReadOnlyTransaction(Method method) {
		Transactional transactional = method.getAnnotation(Transactional.class);
		assertNotNull(transactional);
		assertEquals(true, transactional.readOnly());
	}

	private static Lab lab(UUID id) {
		Lab lab = new Lab();
		lab.setId(id);
		lab.setCode("SMART");
		return lab;
	}

	private static User user(UUID id, Lab lab, UserAccountStatus status) {
		User user = new User();
		user.setId(id);
		user.setLab(lab);
		user.setUsername("author");
		user.setEmail("author@example.edu");
		user.setPasswordHash("hash");
		user.setFullName("Author Name");
		user.setAccountStatus(status);
		return user;
	}

	private static Role role(UUID id, String code) {
		Role role = new Role();
		role.setId(id);
		role.setCode(code);
		role.setName(code);
		return role;
	}

	private static UserRole userRole(User user, Role role) {
		UserRole userRole = new UserRole();
		userRole.setId(UUID.randomUUID());
		userRole.setUser(user);
		userRole.setRole(role);
		userRole.setStatus(UserRoleStatus.ACTIVE);
		return userRole;
	}

	private static Post post(Lab lab) {
		return post(lab, UUID.randomUUID());
	}

	private static Post post(Lab lab, UUID postId) {
		User author = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		Project project = new Project();
		project.setId(UUID.randomUUID());
		project.setLab(lab);
		project.setName("Project Name");
		PostCategory category = new PostCategory();
		category.setId(UUID.randomUUID());
		category.setName("Category Name");
		File coverFile = new File();
		coverFile.setId(UUID.randomUUID());
		Post post = new Post();
		post.setId(postId);
		post.setLab(lab);
		post.setTitle("Post Title");
		post.setSlug("post-title");
		post.setSummary("Short summary");
		post.setContent("Full private content");
		post.setContentType(PostContentType.NEWS);
		post.setVisibility(PostVisibility.PUBLIC);
		post.setModerationStatus(PostStatus.PUBLISHED);
		post.setAuthor(author);
		post.setProject(project);
		post.setCategory(category);
		post.setCoverFile(coverFile);
		post.setPublishedAt(OffsetDateTime.parse("2026-07-20T10:15:30Z"));
		post.setCreatedAt(OffsetDateTime.parse("2026-07-19T10:15:30Z"));
		post.setUpdatedAt(OffsetDateTime.parse("2026-07-21T10:15:30Z"));
		return post;
	}
}

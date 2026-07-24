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
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
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
import com.smartlab.entity.PostAttachment;
import com.smartlab.entity.PostCategory;
import com.smartlab.entity.PostModerationLog;
import com.smartlab.entity.Project;
import com.smartlab.entity.Role;
import com.smartlab.entity.User;
import com.smartlab.entity.UserRole;
import com.smartlab.enums.PostContentType;
import com.smartlab.enums.PostModerationAction;
import com.smartlab.enums.PostStatus;
import com.smartlab.enums.PostVisibility;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;
import com.smartlab.exception.ConflictingAdminOperationException;
import com.smartlab.exception.ForbiddenAdminOperationException;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.mapper.AdminPostApiMapper;
import com.smartlab.repository.PostAttachmentRepository;
import com.smartlab.repository.PostModerationLogRepository;
import com.smartlab.repository.PostRepository;
import com.smartlab.repository.RoleRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.UserRoleRepository;
import com.smartlab.service.common.AuditLogService;
import com.smartlab.service.common.PostWorkflowService;

class AdminPostServiceTests {

	private final PostRepository postRepository = mock(PostRepository.class);
	private final PostAttachmentRepository postAttachmentRepository = mock(PostAttachmentRepository.class);
	private final PostModerationLogRepository postModerationLogRepository = mock(PostModerationLogRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final RoleRepository roleRepository = mock(RoleRepository.class);
	private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final AdminRolePolicy rolePolicy = new AdminRolePolicy(userRepository, roleRepository, userRoleRepository);
	private final Clock clock = Clock.fixed(Instant.parse("2026-07-23T08:15:30Z"), ZoneOffset.UTC);
	private final AdminPostService service = new AdminPostService(
			postRepository,
			postAttachmentRepository,
			postModerationLogRepository,
			rolePolicy,
			new PostWorkflowService(),
			auditLogService,
			new AdminPostApiMapper(),
			clock);

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
	void listLabAnnouncementsRejectsNullQuery() {
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.listLabAnnouncements(null));
	}

	@Test
	void listLabAnnouncementsRejectsActorsWithoutCurrentDatabaseAdminRole() {
		UUID missingActorId = UUID.randomUUID();
		when(userRepository.findById(missingActorId)).thenReturn(Optional.empty());

		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.listLabAnnouncements(new AdminPostService.ListLabAnnouncementsQuery(
						missingActorId,
						null,
						null)));

		Lab lab = lab(UUID.randomUUID());
		User memberActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(memberActor, role(UUID.randomUUID(), AdminRolePolicy.MEMBER_ROLE_CODE));
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.listLabAnnouncements(new AdminPostService.ListLabAnnouncementsQuery(
						memberActor.getId(),
						null,
						null)));

		User leaderActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(leaderActor, role(UUID.randomUUID(), AdminRolePolicy.LEADER_ROLE_CODE));
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.listLabAnnouncements(new AdminPostService.ListLabAnnouncementsQuery(
						leaderActor.getId(),
						null,
						null)));

		User revokedActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		when(userRepository.findById(revokedActor.getId())).thenReturn(Optional.of(revokedActor));
		when(userRoleRepository.findByUserAndStatus(revokedActor, UserRoleStatus.ACTIVE)).thenReturn(List.of());
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.listLabAnnouncements(new AdminPostService.ListLabAnnouncementsQuery(
						revokedActor.getId(),
						null,
						null)));
	}

	@Test
	void listLabAnnouncementsUsesActorLabFixedContentTypeDefaultsAndSafeMappingForAdmin() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post announcement = post(lab);
		announcement.setContentType(PostContentType.LAB_ANNOUNCEMENT);
		when(postRepository.findAdminPosts(
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(Pageable.class)))
						.thenReturn(new PageImpl<>(List.of(announcement), PageRequest.of(0, 20), 1));

		var response = service.listLabAnnouncements(new AdminPostService.ListLabAnnouncementsQuery(
				actor.getId(),
				null,
				null));

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(postRepository).findAdminPosts(
				org.mockito.Mockito.eq(lab),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.eq(PostContentType.LAB_ANNOUNCEMENT),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.isNull(),
				pageableCaptor.capture());
		assertEquals(0, pageableCaptor.getValue().getPageNumber());
		assertEquals(20, pageableCaptor.getValue().getPageSize());
		assertEquals(1, response.totalElements());
		assertEquals(PostContentType.LAB_ANNOUNCEMENT, response.content().getFirst().contentType());
		assertFalse(java.util.Arrays.stream(response.content().getFirst().getClass().getRecordComponents())
				.anyMatch(component -> List.of("content", "email", "passwordHash", "reviewNote", "deletedAt")
						.contains(component.getName())));
	}

	@Test
	void listLabAnnouncementsAcceptsSuperAdminAndReturnsEmptyPage() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.SUPER_ADMIN_ROLE_CODE));
		when(postRepository.findAdminPosts(
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(Pageable.class)))
						.thenReturn(Page.empty(PageRequest.of(1, 10)));

		var response = service.listLabAnnouncements(new AdminPostService.ListLabAnnouncementsQuery(
				actor.getId(),
				1,
				10));

		verify(postRepository).findAdminPosts(
				org.mockito.Mockito.eq(lab),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.eq(PostContentType.LAB_ANNOUNCEMENT),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.isNull(),
				org.mockito.Mockito.isNull(),
				any(Pageable.class));
		assertEquals(List.of(), response.content());
		assertEquals(1, response.page());
		assertEquals(10, response.size());
		assertEquals(0, response.totalElements());
	}

	@Test
	void listLabAnnouncementsRejectsInvalidPageAndSizeLikeAdminPostList() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));

		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.listLabAnnouncements(new AdminPostService.ListLabAnnouncementsQuery(
						actor.getId(),
						-1,
						20)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.listLabAnnouncements(new AdminPostService.ListLabAnnouncementsQuery(
						actor.getId(),
						0,
						0)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.listLabAnnouncements(new AdminPostService.ListLabAnnouncementsQuery(
						actor.getId(),
						0,
						101)));
	}

	@Test
	void getPostDetailRejectsNullQueryActorAndPostId() {
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.getPostDetail(null));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.getPostDetail(new AdminPostService.GetAdminPostDetailQuery(
						null,
						UUID.randomUUID())));

		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.getPostDetail(new AdminPostService.GetAdminPostDetailQuery(
						actor.getId(),
						null)));
	}

	@Test
	void getPostDetailRejectsMissingActorOrActorWithoutCurrentAdminRole() {
		UUID missingActorId = UUID.randomUUID();
		when(userRepository.findById(missingActorId)).thenReturn(Optional.empty());

		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.getPostDetail(new AdminPostService.GetAdminPostDetailQuery(
						missingActorId,
						UUID.randomUUID())));

		Lab lab = lab(UUID.randomUUID());
		User memberActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(memberActor, role(UUID.randomUUID(), AdminRolePolicy.MEMBER_ROLE_CODE));

		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.getPostDetail(new AdminPostService.GetAdminPostDetailQuery(
						memberActor.getId(),
						UUID.randomUUID())));
	}

	@Test
	void getPostDetailUsesActorLabAndPostIdAndMapsOrderedChildren() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		PostAttachment firstAttachment = attachment(post, "00000000-0000-0000-0000-000000000001");
		PostAttachment secondAttachment = attachment(post, "00000000-0000-0000-0000-000000000002");
		PostModerationLog firstLog = moderationLog(post, "00000000-0000-0000-0000-000000000011");
		PostModerationLog secondLog = moderationLog(post, "00000000-0000-0000-0000-000000000012");
		when(postRepository.findAdminPostDetail(lab.getId(), post.getId())).thenReturn(Optional.of(post));
		when(postAttachmentRepository.findVisibleAdminPostAttachments(post))
				.thenReturn(List.of(secondAttachment, firstAttachment));
		when(postModerationLogRepository.findAdminPostModerationHistory(post))
				.thenReturn(List.of(secondLog, firstLog));

		var response = service.getPostDetail(new AdminPostService.GetAdminPostDetailQuery(
				actor.getId(),
				post.getId()));

		verify(postRepository).findAdminPostDetail(lab.getId(), post.getId());
		verify(postAttachmentRepository).findVisibleAdminPostAttachments(post);
		verify(postModerationLogRepository).findAdminPostModerationHistory(post);
		assertEquals(post.getId(), response.id());
		assertEquals("Full private content", response.content());
		assertEquals(post.getAuthor().getId(), response.author().id());
		assertEquals("Author Name", response.author().fullName());
		assertEquals(secondAttachment.getId(), response.attachments().get(0).attachmentId());
		assertEquals(firstAttachment.getId(), response.attachments().get(1).attachmentId());
		assertEquals(secondLog.getId(), response.moderationHistory().get(0).id());
		assertEquals(firstLog.getId(), response.moderationHistory().get(1).id());
	}

	@Test
	void getPostDetailAcceptsSuperAdminAndConvertsEmptyRootLookupToNotFound() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.SUPER_ADMIN_ROLE_CODE));
		UUID postId = UUID.randomUUID();
		when(postRepository.findAdminPostDetail(lab.getId(), postId)).thenReturn(Optional.empty());

		assertThrows(
				ResourceNotFoundException.class,
				() -> service.getPostDetail(new AdminPostService.GetAdminPostDetailQuery(
						actor.getId(),
						postId)));
	}

	@Test
	void getPostDetailMapsOptionalRelationshipsAndSoftDeletedCoverSafely() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		File deletedCoverFile = file(UUID.randomUUID());
		deletedCoverFile.setDeletedAt(OffsetDateTime.parse("2026-07-22T10:15:30Z"));
		post.setAuthor(null);
		post.setProject(null);
		post.setCategory(null);
		post.setCoverFile(deletedCoverFile);
		when(postRepository.findAdminPostDetail(lab.getId(), post.getId())).thenReturn(Optional.of(post));
		when(postAttachmentRepository.findVisibleAdminPostAttachments(post)).thenReturn(List.of());
		when(postModerationLogRepository.findAdminPostModerationHistory(post)).thenReturn(List.of());

		var response = service.getPostDetail(new AdminPostService.GetAdminPostDetailQuery(
				actor.getId(),
				post.getId()));

		assertNull(response.author());
		assertNull(response.project());
		assertNull(response.category());
		assertNull(response.coverFile());
		assertEquals(List.of(), response.attachments());
		assertEquals(List.of(), response.moderationHistory());
	}

	@Test
	void getLabAnnouncementDetailRejectsNullQueryActorAndPostId() {
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.getLabAnnouncementDetail(null));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.getLabAnnouncementDetail(
						new AdminPostService.GetAdminLabAnnouncementDetailQuery(null, UUID.randomUUID())));

		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.getLabAnnouncementDetail(
						new AdminPostService.GetAdminLabAnnouncementDetailQuery(actor.getId(), null)));
	}

	@Test
	void getLabAnnouncementDetailRejectsActorWithoutCurrentAdminRole() {
		Lab lab = lab(UUID.randomUUID());
		User memberActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(memberActor, role(UUID.randomUUID(), AdminRolePolicy.MEMBER_ROLE_CODE));

		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.getLabAnnouncementDetail(new AdminPostService.GetAdminLabAnnouncementDetailQuery(
						memberActor.getId(),
						UUID.randomUUID())));
	}

	@Test
	void getLabAnnouncementDetailUsesActorLabAndPostIdAndMapsAnnouncementForAdmin() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setContentType(PostContentType.LAB_ANNOUNCEMENT);
		PostAttachment attachment = attachment(post, "00000000-0000-0000-0000-000000000001");
		PostModerationLog log = moderationLog(post, "00000000-0000-0000-0000-000000000011");
		when(postRepository.findAdminPostDetail(lab.getId(), post.getId())).thenReturn(Optional.of(post));
		when(postAttachmentRepository.findVisibleAdminPostAttachments(post)).thenReturn(List.of(attachment));
		when(postModerationLogRepository.findAdminPostModerationHistory(post)).thenReturn(List.of(log));

		var response = service.getLabAnnouncementDetail(new AdminPostService.GetAdminLabAnnouncementDetailQuery(
				actor.getId(),
				post.getId()));

		verify(postRepository).findAdminPostDetail(lab.getId(), post.getId());
		verify(postAttachmentRepository).findVisibleAdminPostAttachments(post);
		verify(postModerationLogRepository).findAdminPostModerationHistory(post);
		verify(postRepository, never()).save(any(Post.class));
		assertEquals(post.getId(), response.id());
		assertEquals(PostContentType.LAB_ANNOUNCEMENT, response.contentType());
		assertEquals(attachment.getId(), response.attachments().getFirst().attachmentId());
		assertEquals(log.getId(), response.moderationHistory().getFirst().id());
	}

	@Test
	void getLabAnnouncementDetailAcceptsSuperAdmin() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.SUPER_ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setContentType(PostContentType.LAB_ANNOUNCEMENT);
		when(postRepository.findAdminPostDetail(lab.getId(), post.getId())).thenReturn(Optional.of(post));
		when(postAttachmentRepository.findVisibleAdminPostAttachments(post)).thenReturn(List.of());
		when(postModerationLogRepository.findAdminPostModerationHistory(post)).thenReturn(List.of());

		var response = service.getLabAnnouncementDetail(new AdminPostService.GetAdminLabAnnouncementDetailQuery(
				actor.getId(),
				post.getId()));

		assertEquals(PostContentType.LAB_ANNOUNCEMENT, response.contentType());
		verify(postRepository).findAdminPostDetail(lab.getId(), post.getId());
	}

	@Test
	void getLabAnnouncementDetailConvertsEmptyRootLookupToGenericNotFound() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		UUID postId = UUID.randomUUID();
		when(postRepository.findAdminPostDetail(lab.getId(), postId)).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(
				ResourceNotFoundException.class,
				() -> service.getLabAnnouncementDetail(new AdminPostService.GetAdminLabAnnouncementDetailQuery(
						actor.getId(),
						postId)));

		assertEquals("Post was not found.", exception.getMessage());
		verify(postAttachmentRepository, never()).findVisibleAdminPostAttachments(any(Post.class));
		verify(postModerationLogRepository, never()).findAdminPostModerationHistory(any(Post.class));
		verify(postRepository, never()).save(any(Post.class));
	}

	@Test
	void getLabAnnouncementDetailHidesWrongContentTypeWithoutLoadingChildrenOrMutating() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setContentType(PostContentType.NEWS);
		when(postRepository.findAdminPostDetail(lab.getId(), post.getId())).thenReturn(Optional.of(post));

		ResourceNotFoundException exception = assertThrows(
				ResourceNotFoundException.class,
				() -> service.getLabAnnouncementDetail(new AdminPostService.GetAdminLabAnnouncementDetailQuery(
						actor.getId(),
						post.getId())));

		assertEquals("Post was not found.", exception.getMessage());
		verify(postAttachmentRepository, never()).findVisibleAdminPostAttachments(any(Post.class));
		verify(postModerationLogRepository, never()).findAdminPostModerationHistory(any(Post.class));
		verify(postRepository, never()).save(any(Post.class));
		verify(postAttachmentRepository, never()).save(any(PostAttachment.class));
		verify(postModerationLogRepository, never()).save(any(PostModerationLog.class));
	}

	@Test
	void approvePostRejectsNullCommandActorAndPostId() {
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.approvePost(null));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.approvePost(new AdminPostService.ApproveAdminPostCommand(
						null,
						UUID.randomUUID())));

		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.approvePost(new AdminPostService.ApproveAdminPostCommand(
						actor.getId(),
						null)));
	}

	@Test
	void approvePostRejectsActorsWithoutCurrentDatabaseAdminRole() {
		Lab lab = lab(UUID.randomUUID());
		User memberActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(memberActor, role(UUID.randomUUID(), AdminRolePolicy.MEMBER_ROLE_CODE));
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.approvePost(new AdminPostService.ApproveAdminPostCommand(
						memberActor.getId(),
						UUID.randomUUID())));

		User leaderActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(leaderActor, role(UUID.randomUUID(), AdminRolePolicy.LEADER_ROLE_CODE));
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.approvePost(new AdminPostService.ApproveAdminPostCommand(
						leaderActor.getId(),
						UUID.randomUUID())));

		User revokedActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		when(userRepository.findById(revokedActor.getId())).thenReturn(Optional.of(revokedActor));
		when(userRoleRepository.findByUserAndStatus(revokedActor, UserRoleStatus.ACTIVE)).thenReturn(List.of());
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.approvePost(new AdminPostService.ApproveAdminPostCommand(
						revokedActor.getId(),
						UUID.randomUUID())));
	}

	@Test
	void approvePostReturnsNotFoundAndDoesNotMutateWhenPostIsMissing() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		UUID postId = UUID.randomUUID();
		when(postRepository.findAdminPostForApproval(lab.getId(), postId)).thenReturn(Optional.empty());

		assertThrows(
				ResourceNotFoundException.class,
				() -> service.approvePost(new AdminPostService.ApproveAdminPostCommand(
						actor.getId(),
						postId)));

		verify(postModerationLogRepository, never()).save(any(PostModerationLog.class));
		verify(postRepository, never()).save(any(Post.class));
	}

	@Test
	void approvePostApprovesPendingPostWithExactMutationLogAndResponseForAdmin() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setModerationStatus(PostStatus.PENDING_REVIEW);
		post.setReviewNote("stale note");
		when(postRepository.findAdminPostForApproval(lab.getId(), post.getId())).thenReturn(Optional.of(post));

		var response = service.approvePost(new AdminPostService.ApproveAdminPostCommand(
				actor.getId(),
				post.getId()));

		ArgumentCaptor<PostModerationLog> logCaptor = ArgumentCaptor.forClass(PostModerationLog.class);
		verify(postModerationLogRepository).save(logCaptor.capture());
		verify(postRepository, never()).save(any(Post.class));
		PostModerationLog log = logCaptor.getValue();
		OffsetDateTime expectedReviewedAt = OffsetDateTime.parse("2026-07-23T08:15:30Z");
		assertEquals(PostStatus.APPROVED, post.getModerationStatus());
		assertEquals(actor, post.getReviewedBy());
		assertEquals(expectedReviewedAt, post.getReviewedAt());
		assertNull(post.getReviewNote());
		assertEquals(post, log.getPost());
		assertEquals(PostModerationAction.APPROVE, log.getAction());
		assertEquals(PostStatus.PENDING_REVIEW, log.getFromStatus());
		assertEquals(PostStatus.APPROVED, log.getToStatus());
		assertEquals(actor, log.getActor());
		assertNull(log.getReason());
		assertEquals(post.getId(), response.postId());
		assertEquals(PostModerationAction.APPROVE, response.action());
		assertEquals(PostStatus.PENDING_REVIEW, response.fromStatus());
		assertEquals(PostStatus.APPROVED, response.toStatus());
		assertEquals(PostStatus.APPROVED, response.moderationStatus());
		assertEquals(actor.getId(), response.reviewedById());
		assertEquals(actor.getFullName(), response.reviewedByName());
		assertEquals(expectedReviewedAt, response.reviewedAt());
	}

	@Test
	void approvePostAcceptsSuperAdmin() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.SUPER_ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setModerationStatus(PostStatus.PENDING_REVIEW);
		when(postRepository.findAdminPostForApproval(lab.getId(), post.getId())).thenReturn(Optional.of(post));

		var response = service.approvePost(new AdminPostService.ApproveAdminPostCommand(
				actor.getId(),
				post.getId()));

		assertEquals(PostStatus.APPROVED, response.moderationStatus());
		verify(postModerationLogRepository).save(any(PostModerationLog.class));
	}

	@Test
	void approvePostRejectsEveryInvalidSourceStatusWithoutMutationOrLog() {
		for (PostStatus status : List.of(
				PostStatus.DRAFT,
				PostStatus.NEEDS_REVISION,
				PostStatus.APPROVED,
				PostStatus.PUBLISHED,
				PostStatus.REJECTED)) {
			org.mockito.Mockito.clearInvocations(
					postRepository,
					postModerationLogRepository,
					userRepository,
					roleRepository,
					userRoleRepository);
			Lab lab = lab(UUID.randomUUID());
			User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
			stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
			Post post = post(lab);
			post.setModerationStatus(status);
			post.setReviewedBy(null);
			post.setReviewedAt(null);
			post.setReviewNote("keep me");
			when(postRepository.findAdminPostForApproval(lab.getId(), post.getId())).thenReturn(Optional.of(post));

			assertThrows(
					ConflictingAdminOperationException.class,
					() -> service.approvePost(new AdminPostService.ApproveAdminPostCommand(
							actor.getId(),
							post.getId())));

			assertEquals(status, post.getModerationStatus());
			assertNull(post.getReviewedBy());
			assertNull(post.getReviewedAt());
			assertEquals("keep me", post.getReviewNote());
			verify(postModerationLogRepository, never()).save(any(PostModerationLog.class));
			verify(postRepository, never()).save(any(Post.class));
		}
	}

	@Test
	void approvePostPropagatesModerationLogSaveFailureForTransactionalRollback() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setModerationStatus(PostStatus.PENDING_REVIEW);
		RuntimeException failure = new RuntimeException("log insert failed");
		when(postRepository.findAdminPostForApproval(lab.getId(), post.getId())).thenReturn(Optional.of(post));
		when(postModerationLogRepository.save(any(PostModerationLog.class))).thenThrow(failure);

		RuntimeException exception = assertThrows(
				RuntimeException.class,
				() -> service.approvePost(new AdminPostService.ApproveAdminPostCommand(
						actor.getId(),
						post.getId())));

		assertEquals(failure, exception);
		verify(postRepository, never()).save(any(Post.class));
	}

	@Test
	void publishPostRejectsNullCommandActorAndPostId() {
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.publishPost(null));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.publishPost(new AdminPostService.PublishAdminPostCommand(
						null,
						UUID.randomUUID())));

		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.publishPost(new AdminPostService.PublishAdminPostCommand(
						actor.getId(),
						null)));
	}

	@Test
	void publishPostRejectsActorsWithoutCurrentDatabaseAdminRole() {
		Lab lab = lab(UUID.randomUUID());
		User memberActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(memberActor, role(UUID.randomUUID(), AdminRolePolicy.MEMBER_ROLE_CODE));
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.publishPost(new AdminPostService.PublishAdminPostCommand(
						memberActor.getId(),
						UUID.randomUUID())));

		User leaderActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(leaderActor, role(UUID.randomUUID(), AdminRolePolicy.LEADER_ROLE_CODE));
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.publishPost(new AdminPostService.PublishAdminPostCommand(
						leaderActor.getId(),
						UUID.randomUUID())));

		User revokedActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		when(userRepository.findById(revokedActor.getId())).thenReturn(Optional.of(revokedActor));
		when(userRoleRepository.findByUserAndStatus(revokedActor, UserRoleStatus.ACTIVE)).thenReturn(List.of());
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.publishPost(new AdminPostService.PublishAdminPostCommand(
						revokedActor.getId(),
						UUID.randomUUID())));
	}

	@Test
	void publishPostReturnsGenericNotFoundForMissingCrossLabAndSoftDeletedPosts() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		List<UUID> hiddenPostIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
		for (UUID postId : hiddenPostIds) {
			when(postRepository.findAdminPostForApproval(lab.getId(), postId)).thenReturn(Optional.empty());

			ResourceNotFoundException exception = assertThrows(
					ResourceNotFoundException.class,
					() -> service.publishPost(new AdminPostService.PublishAdminPostCommand(
							actor.getId(),
							postId)));

			assertEquals("Post was not found.", exception.getMessage());
		}

		verify(postModerationLogRepository, never()).save(any(PostModerationLog.class));
		verify(auditLogService, never()).record(any(AuditLogService.AuditCommand.class));
		verify(postRepository, never()).save(any(Post.class));
	}

	@Test
	void publishPostPublishesApprovedPostWithUtcTimestampModerationLogAuditAndSafeResponse() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		User originalReviewer = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setModerationStatus(PostStatus.APPROVED);
		post.setPublishedAt(null);
		post.setReviewedBy(originalReviewer);
		OffsetDateTime originalReviewedAt = OffsetDateTime.parse("2026-07-22T08:15:30Z");
		post.setReviewedAt(originalReviewedAt);
		post.setReviewNote("approved note");
		String originalContent = post.getContent();
		when(postRepository.findAdminPostForApproval(lab.getId(), post.getId())).thenReturn(Optional.of(post));

		var response = service.publishPost(new AdminPostService.PublishAdminPostCommand(
				actor.getId(),
				post.getId()));

		ArgumentCaptor<PostModerationLog> logCaptor = ArgumentCaptor.forClass(PostModerationLog.class);
		ArgumentCaptor<AuditLogService.AuditCommand> auditCaptor =
				ArgumentCaptor.forClass(AuditLogService.AuditCommand.class);
		verify(postRepository).findAdminPostForApproval(lab.getId(), post.getId());
		verify(postModerationLogRepository).save(logCaptor.capture());
		verify(auditLogService).record(auditCaptor.capture());
		verify(postRepository, never()).save(any(Post.class));
		PostModerationLog log = logCaptor.getValue();
		OffsetDateTime expectedPublishedAt = OffsetDateTime.parse("2026-07-23T08:15:30Z");
		assertEquals(PostStatus.PUBLISHED, post.getModerationStatus());
		assertEquals(expectedPublishedAt, post.getPublishedAt());
		assertEquals(originalContent, post.getContent());
		assertEquals(originalReviewer, post.getReviewedBy());
		assertEquals(originalReviewedAt, post.getReviewedAt());
		assertEquals("approved note", post.getReviewNote());
		assertEquals(post, log.getPost());
		assertEquals(PostModerationAction.PUBLISH, log.getAction());
		assertEquals(PostStatus.APPROVED, log.getFromStatus());
		assertEquals(PostStatus.PUBLISHED, log.getToStatus());
		assertEquals(actor, log.getActor());
		assertNull(log.getReason());
		AuditLogService.AuditCommand audit = auditCaptor.getValue();
		assertEquals(actor.getId(), audit.actorId());
		assertEquals("PUBLISH_POST", audit.action());
		assertEquals("POST", audit.entityType());
		assertEquals(post.getId(), audit.entityId());
		assertEquals(
				auditSnapshot(post.getId(), PostStatus.APPROVED, null),
				audit.oldValue());
		assertEquals(
				auditSnapshot(post.getId(), PostStatus.PUBLISHED, expectedPublishedAt),
				audit.newValue());
		assertFalse(audit.oldValue().toString().contains(originalContent));
		assertFalse(audit.newValue().toString().contains(originalContent));
		assertEquals(post.getId(), response.postId());
		assertEquals(PostModerationAction.PUBLISH, response.action());
		assertEquals(PostStatus.APPROVED, response.fromStatus());
		assertEquals(PostStatus.PUBLISHED, response.toStatus());
		assertEquals(PostStatus.PUBLISHED, response.moderationStatus());
		assertEquals(originalReviewer.getId(), response.reviewedById());
		assertEquals(originalReviewer.getFullName(), response.reviewedByName());
		assertEquals(originalReviewedAt, response.reviewedAt());
	}

	@Test
	void publishPostAcceptsSuperAdmin() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.SUPER_ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setModerationStatus(PostStatus.APPROVED);
		when(postRepository.findAdminPostForApproval(lab.getId(), post.getId())).thenReturn(Optional.of(post));

		var response = service.publishPost(new AdminPostService.PublishAdminPostCommand(
				actor.getId(),
				post.getId()));

		assertEquals(PostStatus.PUBLISHED, response.moderationStatus());
		verify(postModerationLogRepository).save(any(PostModerationLog.class));
		verify(auditLogService).record(any(AuditLogService.AuditCommand.class));
	}

	@Test
	void publishPostRejectsEveryInvalidSourceStatusWithoutMutationLogOrAudit() {
		for (PostStatus status : List.of(
				PostStatus.DRAFT,
				PostStatus.PENDING_REVIEW,
				PostStatus.NEEDS_REVISION,
				PostStatus.PUBLISHED,
				PostStatus.REJECTED)) {
			org.mockito.Mockito.clearInvocations(
					postRepository,
					postModerationLogRepository,
					auditLogService,
					userRepository,
					roleRepository,
					userRoleRepository);
			Lab lab = lab(UUID.randomUUID());
			User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
			stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
			Post post = post(lab);
			post.setModerationStatus(status);
			OffsetDateTime originalPublishedAt = post.getPublishedAt();
			when(postRepository.findAdminPostForApproval(lab.getId(), post.getId())).thenReturn(Optional.of(post));

			assertThrows(
					ConflictingAdminOperationException.class,
					() -> service.publishPost(new AdminPostService.PublishAdminPostCommand(
							actor.getId(),
							post.getId())));

			assertEquals(status, post.getModerationStatus());
			assertEquals(originalPublishedAt, post.getPublishedAt());
			verify(postModerationLogRepository, never()).save(any(PostModerationLog.class));
			verify(auditLogService, never()).record(any(AuditLogService.AuditCommand.class));
			verify(postRepository, never()).save(any(Post.class));
		}
	}

	@Test
	void publishPostPropagatesAuditFailureForTransactionalRollback() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setModerationStatus(PostStatus.APPROVED);
		RuntimeException failure = new RuntimeException("audit insert failed");
		when(postRepository.findAdminPostForApproval(lab.getId(), post.getId())).thenReturn(Optional.of(post));
		when(auditLogService.record(any(AuditLogService.AuditCommand.class))).thenThrow(failure);

		RuntimeException exception = assertThrows(
				RuntimeException.class,
				() -> service.publishPost(new AdminPostService.PublishAdminPostCommand(
						actor.getId(),
						post.getId())));

		assertEquals(failure, exception);
		verify(postModerationLogRepository).save(any(PostModerationLog.class));
		verify(postRepository, never()).save(any(Post.class));
	}

	@Test
	void publishPostDoesNotDependOnNotificationCreation() {
		assertFalse(java.util.Arrays.stream(AdminPostService.class.getDeclaredFields())
				.anyMatch(field -> field.getType().getName().contains("Notification")));
	}

	@Test
	void unpublishPostRejectsNullCommandActorAndPostId() {
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.unpublishPost(null));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.unpublishPost(new AdminPostService.UnpublishAdminPostCommand(
						null,
						UUID.randomUUID())));

		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.unpublishPost(new AdminPostService.UnpublishAdminPostCommand(
						actor.getId(),
						null)));
	}

	@Test
	void unpublishPostRejectsActorsWithoutCurrentDatabaseAdminRole() {
		Lab lab = lab(UUID.randomUUID());
		User memberActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(memberActor, role(UUID.randomUUID(), AdminRolePolicy.MEMBER_ROLE_CODE));
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.unpublishPost(new AdminPostService.UnpublishAdminPostCommand(
						memberActor.getId(),
						UUID.randomUUID())));

		User leaderActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(leaderActor, role(UUID.randomUUID(), AdminRolePolicy.LEADER_ROLE_CODE));
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.unpublishPost(new AdminPostService.UnpublishAdminPostCommand(
						leaderActor.getId(),
						UUID.randomUUID())));

		User revokedActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		when(userRepository.findById(revokedActor.getId())).thenReturn(Optional.of(revokedActor));
		when(userRoleRepository.findByUserAndStatus(revokedActor, UserRoleStatus.ACTIVE)).thenReturn(List.of());
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.unpublishPost(new AdminPostService.UnpublishAdminPostCommand(
						revokedActor.getId(),
						UUID.randomUUID())));
	}

	@Test
	void unpublishPostReturnsGenericNotFoundForMissingCrossLabAndSoftDeletedPosts() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		List<UUID> hiddenPostIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
		for (UUID postId : hiddenPostIds) {
			when(postRepository.findAdminPostForApproval(lab.getId(), postId)).thenReturn(Optional.empty());

			ResourceNotFoundException exception = assertThrows(
					ResourceNotFoundException.class,
					() -> service.unpublishPost(new AdminPostService.UnpublishAdminPostCommand(
							actor.getId(),
							postId)));

			assertEquals("Post was not found.", exception.getMessage());
		}

		verify(postModerationLogRepository, never()).save(any(PostModerationLog.class));
		verify(auditLogService, never()).record(any(AuditLogService.AuditCommand.class));
		verify(postRepository, never()).save(any(Post.class));
	}

	@Test
	void unpublishPostUnpublishesPublishedPostWithModerationLogAuditAndSafeResponse() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		User originalReviewer = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setModerationStatus(PostStatus.PUBLISHED);
		OffsetDateTime originalPublishedAt = OffsetDateTime.parse("2026-07-20T10:15:30Z");
		post.setPublishedAt(originalPublishedAt);
		post.setReviewedBy(originalReviewer);
		OffsetDateTime originalReviewedAt = OffsetDateTime.parse("2026-07-19T08:15:30Z");
		post.setReviewedAt(originalReviewedAt);
		post.setReviewNote("approved note");
		String originalTitle = post.getTitle();
		String originalSummary = post.getSummary();
		String originalContent = post.getContent();
		PostContentType originalContentType = post.getContentType();
		PostVisibility originalVisibility = post.getVisibility();
		User originalAuthor = post.getAuthor();
		Project originalProject = post.getProject();
		PostCategory originalCategory = post.getCategory();
		when(postRepository.findAdminPostForApproval(lab.getId(), post.getId())).thenReturn(Optional.of(post));

		var response = service.unpublishPost(new AdminPostService.UnpublishAdminPostCommand(
				actor.getId(),
				post.getId()));

		ArgumentCaptor<PostModerationLog> logCaptor = ArgumentCaptor.forClass(PostModerationLog.class);
		ArgumentCaptor<AuditLogService.AuditCommand> auditCaptor =
				ArgumentCaptor.forClass(AuditLogService.AuditCommand.class);
		verify(postRepository).findAdminPostForApproval(lab.getId(), post.getId());
		verify(postModerationLogRepository).save(logCaptor.capture());
		verify(auditLogService).record(auditCaptor.capture());
		verify(postRepository, never()).save(any(Post.class));
		PostModerationLog log = logCaptor.getValue();
		assertEquals(PostStatus.APPROVED, post.getModerationStatus());
		assertNull(post.getPublishedAt());
		assertEquals(originalTitle, post.getTitle());
		assertEquals(originalSummary, post.getSummary());
		assertEquals(originalContent, post.getContent());
		assertEquals(originalContentType, post.getContentType());
		assertEquals(originalVisibility, post.getVisibility());
		assertEquals(originalAuthor, post.getAuthor());
		assertEquals(originalProject, post.getProject());
		assertEquals(originalCategory, post.getCategory());
		assertEquals(originalReviewer, post.getReviewedBy());
		assertEquals(originalReviewedAt, post.getReviewedAt());
		assertEquals("approved note", post.getReviewNote());
		assertEquals(post, log.getPost());
		assertEquals(PostModerationAction.UNPUBLISH, log.getAction());
		assertEquals(PostStatus.PUBLISHED, log.getFromStatus());
		assertEquals(PostStatus.APPROVED, log.getToStatus());
		assertEquals(actor, log.getActor());
		assertNull(log.getReason());
		AuditLogService.AuditCommand audit = auditCaptor.getValue();
		assertEquals(actor.getId(), audit.actorId());
		assertEquals("UNPUBLISH_POST", audit.action());
		assertEquals("POST", audit.entityType());
		assertEquals(post.getId(), audit.entityId());
		assertEquals(
				auditSnapshot(post.getId(), PostStatus.PUBLISHED, originalPublishedAt),
				audit.oldValue());
		assertEquals(
				auditSnapshot(post.getId(), PostStatus.APPROVED, null),
				audit.newValue());
		assertFalse(audit.oldValue().toString().contains(originalContent));
		assertFalse(audit.newValue().toString().contains(originalContent));
		assertEquals(post.getId(), response.postId());
		assertEquals(PostModerationAction.UNPUBLISH, response.action());
		assertEquals(PostStatus.PUBLISHED, response.fromStatus());
		assertEquals(PostStatus.APPROVED, response.toStatus());
		assertEquals(PostStatus.APPROVED, response.moderationStatus());
		assertEquals(originalReviewer.getId(), response.reviewedById());
		assertEquals(originalReviewer.getFullName(), response.reviewedByName());
		assertEquals(originalReviewedAt, response.reviewedAt());
	}

	@Test
	void unpublishPostAcceptsSuperAdmin() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.SUPER_ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setModerationStatus(PostStatus.PUBLISHED);
		when(postRepository.findAdminPostForApproval(lab.getId(), post.getId())).thenReturn(Optional.of(post));

		var response = service.unpublishPost(new AdminPostService.UnpublishAdminPostCommand(
				actor.getId(),
				post.getId()));

		assertEquals(PostStatus.APPROVED, response.moderationStatus());
		verify(postModerationLogRepository).save(any(PostModerationLog.class));
		verify(auditLogService).record(any(AuditLogService.AuditCommand.class));
	}

	@Test
	void unpublishPostRejectsEveryInvalidSourceStatusWithoutMutationLogOrAudit() {
		for (PostStatus status : List.of(
				PostStatus.DRAFT,
				PostStatus.PENDING_REVIEW,
				PostStatus.NEEDS_REVISION,
				PostStatus.APPROVED,
				PostStatus.REJECTED)) {
			org.mockito.Mockito.clearInvocations(
					postRepository,
					postModerationLogRepository,
					auditLogService,
					userRepository,
					roleRepository,
					userRoleRepository);
			Lab lab = lab(UUID.randomUUID());
			User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
			stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
			Post post = post(lab);
			post.setModerationStatus(status);
			OffsetDateTime originalPublishedAt = post.getPublishedAt();
			when(postRepository.findAdminPostForApproval(lab.getId(), post.getId())).thenReturn(Optional.of(post));

			ConflictingAdminOperationException exception = assertThrows(
					ConflictingAdminOperationException.class,
					() -> service.unpublishPost(new AdminPostService.UnpublishAdminPostCommand(
							actor.getId(),
							post.getId())));

			assertEquals("Only published posts can be unpublished.", exception.getMessage());
			assertEquals(status, post.getModerationStatus());
			assertEquals(originalPublishedAt, post.getPublishedAt());
			verify(postModerationLogRepository, never()).save(any(PostModerationLog.class));
			verify(auditLogService, never()).record(any(AuditLogService.AuditCommand.class));
			verify(postRepository, never()).save(any(Post.class));
		}
	}

	@Test
	void unpublishPostPropagatesModerationLogSaveFailureForTransactionalRollback() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setModerationStatus(PostStatus.PUBLISHED);
		RuntimeException failure = new RuntimeException("log insert failed");
		when(postRepository.findAdminPostForApproval(lab.getId(), post.getId())).thenReturn(Optional.of(post));
		when(postModerationLogRepository.save(any(PostModerationLog.class))).thenThrow(failure);

		RuntimeException exception = assertThrows(
				RuntimeException.class,
				() -> service.unpublishPost(new AdminPostService.UnpublishAdminPostCommand(
						actor.getId(),
						post.getId())));

		assertEquals(failure, exception);
		verify(auditLogService, never()).record(any(AuditLogService.AuditCommand.class));
		verify(postRepository, never()).save(any(Post.class));
	}

	@Test
	void unpublishPostPropagatesAuditFailureForTransactionalRollback() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setModerationStatus(PostStatus.PUBLISHED);
		RuntimeException failure = new RuntimeException("audit insert failed");
		when(postRepository.findAdminPostForApproval(lab.getId(), post.getId())).thenReturn(Optional.of(post));
		when(auditLogService.record(any(AuditLogService.AuditCommand.class))).thenThrow(failure);

		RuntimeException exception = assertThrows(
				RuntimeException.class,
				() -> service.unpublishPost(new AdminPostService.UnpublishAdminPostCommand(
						actor.getId(),
						post.getId())));

		assertEquals(failure, exception);
		verify(postModerationLogRepository).save(any(PostModerationLog.class));
		verify(postRepository, never()).save(any(Post.class));
	}

	@Test
	void unpublishPostDoesNotDependOnNotificationCreation() {
		assertFalse(java.util.Arrays.stream(AdminPostService.class.getDeclaredFields())
				.anyMatch(field -> field.getType().getName().contains("Notification")));
	}

	@Test
	void softDeletePostRejectsNullCommandActorAndPostId() {
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.softDeletePost(null));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.softDeletePost(new AdminPostService.DeleteAdminPostCommand(
						null,
						UUID.randomUUID())));

		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.softDeletePost(new AdminPostService.DeleteAdminPostCommand(
						actor.getId(),
						null)));
	}

	@Test
	void softDeletePostRejectsMissingActorAndNonAdminActors() {
		UUID missingActorId = UUID.randomUUID();
		when(userRepository.findById(missingActorId)).thenReturn(Optional.empty());
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.softDeletePost(new AdminPostService.DeleteAdminPostCommand(
						missingActorId,
						UUID.randomUUID())));

		Lab lab = lab(UUID.randomUUID());
		User memberActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(memberActor, role(UUID.randomUUID(), AdminRolePolicy.MEMBER_ROLE_CODE));
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.softDeletePost(new AdminPostService.DeleteAdminPostCommand(
						memberActor.getId(),
						UUID.randomUUID())));

		User leaderActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(leaderActor, role(UUID.randomUUID(), AdminRolePolicy.LEADER_ROLE_CODE));
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.softDeletePost(new AdminPostService.DeleteAdminPostCommand(
						leaderActor.getId(),
						UUID.randomUUID())));

		User revokedActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		when(userRepository.findById(revokedActor.getId())).thenReturn(Optional.of(revokedActor));
		when(userRoleRepository.findByUserAndStatus(revokedActor, UserRoleStatus.ACTIVE)).thenReturn(List.of());
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.softDeletePost(new AdminPostService.DeleteAdminPostCommand(
						revokedActor.getId(),
						UUID.randomUUID())));
	}

	@Test
	void softDeletePostReturnsGenericNotFoundForMissingCrossLabAndAlreadyDeletedPosts() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		List<UUID> hiddenPostIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
		for (UUID postId : hiddenPostIds) {
			when(postRepository.findAdminPostForDeletion(lab.getId(), postId)).thenReturn(Optional.empty());

			ResourceNotFoundException exception = assertThrows(
					ResourceNotFoundException.class,
					() -> service.softDeletePost(new AdminPostService.DeleteAdminPostCommand(
							actor.getId(),
							postId)));

			assertEquals("Post was not found.", exception.getMessage());
		}

		verify(auditLogService, never()).record(any(AuditLogService.AuditCommand.class));
		verify(postModerationLogRepository, never()).save(any(PostModerationLog.class));
		verify(postRepository, never()).save(any(Post.class));
		verify(postRepository, never()).delete(any(Post.class));
	}

	@Test
	void softDeletePostSecondAttemptReturnsNotFoundAfterScopedLookupFiltersDeletedPost() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		when(postRepository.findAdminPostForDeletion(lab.getId(), post.getId()))
				.thenReturn(Optional.of(post), Optional.empty());

		service.softDeletePost(new AdminPostService.DeleteAdminPostCommand(
				actor.getId(),
				post.getId()));
		org.mockito.Mockito.clearInvocations(auditLogService, postModerationLogRepository, postRepository);

		ResourceNotFoundException exception = assertThrows(
				ResourceNotFoundException.class,
				() -> service.softDeletePost(new AdminPostService.DeleteAdminPostCommand(
						actor.getId(),
						post.getId())));

		assertEquals("Post was not found.", exception.getMessage());
		verify(postRepository).findAdminPostForDeletion(lab.getId(), post.getId());
		verify(auditLogService, never()).record(any(AuditLogService.AuditCommand.class));
		verify(postModerationLogRepository, never()).save(any(PostModerationLog.class));
		verify(postRepository, never()).save(any(Post.class));
		verify(postRepository, never()).delete(any(Post.class));
	}

	@Test
	void softDeletePostSetsDeletedAtAuditsSafeSnapshotsAndPreservesPostFieldsForAdmin() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		User reviewer = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setReviewedBy(reviewer);
		post.setReviewedAt(OffsetDateTime.parse("2026-07-20T09:15:30Z"));
		post.setReviewNote("approved note");
		String originalTitle = post.getTitle();
		String originalSummary = post.getSummary();
		String originalContent = post.getContent();
		PostContentType originalContentType = post.getContentType();
		PostVisibility originalVisibility = post.getVisibility();
		PostStatus originalStatus = post.getModerationStatus();
		OffsetDateTime originalPublishedAt = post.getPublishedAt();
		User originalAuthor = post.getAuthor();
		Project originalProject = post.getProject();
		PostCategory originalCategory = post.getCategory();
		File originalCoverFile = post.getCoverFile();
		when(postRepository.findAdminPostForDeletion(lab.getId(), post.getId())).thenReturn(Optional.of(post));

		service.softDeletePost(new AdminPostService.DeleteAdminPostCommand(
				actor.getId(),
				post.getId()));

		ArgumentCaptor<AuditLogService.AuditCommand> auditCaptor =
				ArgumentCaptor.forClass(AuditLogService.AuditCommand.class);
		verify(postRepository).findAdminPostForDeletion(lab.getId(), post.getId());
		verify(auditLogService).record(auditCaptor.capture());
		verify(postRepository, never()).save(any(Post.class));
		verify(postRepository, never()).delete(any(Post.class));
		verify(postRepository, never()).deleteById(any(UUID.class));
		verify(postModerationLogRepository, never()).save(any(PostModerationLog.class));
		verify(postAttachmentRepository, never()).findVisibleAdminPostAttachments(any(Post.class));
		verify(postModerationLogRepository, never()).findAdminPostModerationHistory(any(Post.class));
		OffsetDateTime expectedDeletedAt = OffsetDateTime.parse("2026-07-23T08:15:30Z");
		assertEquals(expectedDeletedAt, post.getDeletedAt());
		assertEquals(originalTitle, post.getTitle());
		assertEquals(originalSummary, post.getSummary());
		assertEquals(originalContent, post.getContent());
		assertEquals(originalContentType, post.getContentType());
		assertEquals(originalVisibility, post.getVisibility());
		assertEquals(originalStatus, post.getModerationStatus());
		assertEquals(originalPublishedAt, post.getPublishedAt());
		assertEquals(originalAuthor, post.getAuthor());
		assertEquals(originalProject, post.getProject());
		assertEquals(originalCategory, post.getCategory());
		assertEquals(originalCoverFile, post.getCoverFile());
		assertEquals(reviewer, post.getReviewedBy());
		assertEquals(OffsetDateTime.parse("2026-07-20T09:15:30Z"), post.getReviewedAt());
		assertEquals("approved note", post.getReviewNote());
		AuditLogService.AuditCommand audit = auditCaptor.getValue();
		assertEquals(actor.getId(), audit.actorId());
		assertEquals("DELETE_POST", audit.action());
		assertEquals("POST", audit.entityType());
		assertEquals(post.getId(), audit.entityId());
		assertDeletionSnapshot(audit.oldValue(), post, null);
		assertDeletionSnapshot(audit.newValue(), post, expectedDeletedAt);
		assertSafeDeletionSnapshot(audit.oldValue(), originalTitle, originalSummary, originalContent);
		assertSafeDeletionSnapshot(audit.newValue(), originalTitle, originalSummary, originalContent);
	}

	@Test
	void softDeletePostAcceptsSuperAdmin() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.SUPER_ADMIN_ROLE_CODE));
		Post post = post(lab);
		when(postRepository.findAdminPostForDeletion(lab.getId(), post.getId())).thenReturn(Optional.of(post));

		service.softDeletePost(new AdminPostService.DeleteAdminPostCommand(
				actor.getId(),
				post.getId()));

		assertEquals(OffsetDateTime.parse("2026-07-23T08:15:30Z"), post.getDeletedAt());
		verify(auditLogService).record(any(AuditLogService.AuditCommand.class));
	}

	@Test
	void softDeletePostPropagatesAuditFailureForTransactionalRollback() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		RuntimeException failure = new RuntimeException("audit insert failed");
		when(postRepository.findAdminPostForDeletion(lab.getId(), post.getId())).thenReturn(Optional.of(post));
		when(auditLogService.record(any(AuditLogService.AuditCommand.class))).thenThrow(failure);

		RuntimeException exception = assertThrows(
				RuntimeException.class,
				() -> service.softDeletePost(new AdminPostService.DeleteAdminPostCommand(
						actor.getId(),
						post.getId())));

		assertEquals(failure, exception);
		verify(postRepository, never()).save(any(Post.class));
		verify(postRepository, never()).delete(any(Post.class));
		verify(postModerationLogRepository, never()).save(any(PostModerationLog.class));
	}

	@Test
	void softDeletePostDoesNotDependOnNotificationCreation() {
		assertFalse(java.util.Arrays.stream(AdminPostService.class.getDeclaredFields())
				.anyMatch(field -> field.getType().getName().contains("Notification")));
	}

	@Test
	void softDeletePostUsesPessimisticWriteScopedDeletionLookup() throws NoSuchMethodException {
		Method deletionLookup = PostRepository.class.getMethod("findAdminPostForDeletion", UUID.class, UUID.class);
		org.springframework.data.jpa.repository.Lock lock =
				deletionLookup.getAnnotation(org.springframework.data.jpa.repository.Lock.class);

		assertNotNull(lock);
		assertEquals(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE, lock.value());
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
		assertReadOnlyTransaction(AdminPostService.class.getMethod(
				"listLabAnnouncements",
				AdminPostService.ListLabAnnouncementsQuery.class));
		assertReadOnlyTransaction(AdminPostService.class.getMethod(
				"getPostDetail",
				AdminPostService.GetAdminPostDetailQuery.class));
		assertReadOnlyTransaction(AdminPostService.class.getMethod(
				"getLabAnnouncementDetail",
				AdminPostService.GetAdminLabAnnouncementDetailQuery.class));
		assertMutationTransaction(AdminPostService.class.getMethod(
				"approvePost",
				AdminPostService.ApproveAdminPostCommand.class));
		assertMutationTransaction(AdminPostService.class.getMethod(
				"publishPost",
				AdminPostService.PublishAdminPostCommand.class));
		assertMutationTransaction(AdminPostService.class.getMethod(
				"unpublishPost",
				AdminPostService.UnpublishAdminPostCommand.class));
		assertMutationTransaction(AdminPostService.class.getMethod(
				"softDeletePost",
				AdminPostService.DeleteAdminPostCommand.class));
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

	private static void assertMutationTransaction(Method method) {
		Transactional transactional = method.getAnnotation(Transactional.class);
		assertNotNull(transactional);
		assertEquals(false, transactional.readOnly());
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

	private static PostAttachment attachment(Post post, String attachmentId) {
		PostAttachment attachment = new PostAttachment();
		attachment.setId(UUID.fromString(attachmentId));
		attachment.setPost(post);
		attachment.setFile(file(UUID.randomUUID()));
		attachment.setUploadedBy(user(UUID.randomUUID(), post.getLab(), UserAccountStatus.ACTIVE));
		attachment.setCreatedAt(OffsetDateTime.parse("2026-07-20T10:15:30Z"));
		return attachment;
	}

	private static PostModerationLog moderationLog(Post post, String logId) {
		PostModerationLog log = new PostModerationLog();
		log.setId(UUID.fromString(logId));
		log.setPost(post);
		log.setAction(PostModerationAction.SUBMIT);
		log.setFromStatus(PostStatus.DRAFT);
		log.setToStatus(PostStatus.PENDING_REVIEW);
		log.setActor(user(UUID.randomUUID(), post.getLab(), UserAccountStatus.ACTIVE));
		log.setReason("Submitted for review");
		log.setCreatedAt(OffsetDateTime.parse("2026-07-20T10:15:30Z"));
		return log;
	}

	private static Map<String, Object> auditSnapshot(
			UUID postId,
			PostStatus moderationStatus,
			OffsetDateTime publishedAt) {
		Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
		snapshot.put("postId", postId);
		snapshot.put("moderationStatus", moderationStatus);
		snapshot.put("publishedAt", publishedAt);
		return snapshot;
	}

	private static void assertDeletionSnapshot(Object snapshot, Post post, OffsetDateTime deletedAt) {
		assertEquals(post.getId(), snapshotValue(snapshot, "postId"));
		assertEquals(post.getModerationStatus(), snapshotValue(snapshot, "moderationStatus"));
		assertEquals(post.getContentType(), snapshotValue(snapshot, "contentType"));
		assertEquals(post.getVisibility(), snapshotValue(snapshot, "visibility"));
		assertEquals(post.getPublishedAt(), snapshotValue(snapshot, "publishedAt"));
		assertEquals(post.getReviewedBy().getId(), snapshotValue(snapshot, "reviewedById"));
		assertEquals(post.getReviewedAt(), snapshotValue(snapshot, "reviewedAt"));
		assertEquals(deletedAt, snapshotValue(snapshot, "deletedAt"));
	}

	private static void assertSafeDeletionSnapshot(
			Object snapshot,
			String originalTitle,
			String originalSummary,
			String originalContent) {
		String renderedSnapshot = snapshot.toString();
		assertFalse(renderedSnapshot.contains(originalTitle));
		assertFalse(renderedSnapshot.contains(originalSummary));
		assertFalse(renderedSnapshot.contains(originalContent));
		assertFalse(renderedSnapshot.contains("author@example.edu"));
		assertFalse(renderedSnapshot.contains("author"));
		assertFalse(renderedSnapshot.contains("hash"));
		assertFalse(renderedSnapshot.contains("approved note"));
		assertFalse(renderedSnapshot.contains("internal-name"));
		assertFalse(renderedSnapshot.contains("/private/storage/paper.pdf"));
	}

	private static Object snapshotValue(Object snapshot, String componentName) {
		try {
			Method componentAccessor = snapshot.getClass().getDeclaredMethod(componentName);
			componentAccessor.setAccessible(true);
			return componentAccessor.invoke(snapshot);
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError("Audit snapshot is missing component " + componentName, exception);
		}
	}

	private static File file(UUID fileId) {
		File file = new File();
		file.setId(fileId);
		file.setOriginalName("paper.pdf");
		file.setStoredName("internal-name");
		file.setStoragePath("/private/storage/paper.pdf");
		file.setMimeType("application/pdf");
		file.setFileSize(1024L);
		file.setFileExtension("pdf");
		file.setCreatedAt(OffsetDateTime.parse("2026-07-20T10:15:30Z"));
		return file;
	}
}

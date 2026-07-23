package com.smartlab.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.InOrder;
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
	private final AdminRolePolicy rolePolicy = new AdminRolePolicy(userRepository, roleRepository, userRoleRepository);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final Clock clock = Clock.fixed(Instant.parse("2026-07-23T08:15:30Z"), ZoneOffset.UTC);
	private final AdminPostService service = new AdminPostService(
			postRepository,
			postAttachmentRepository,
			postModerationLogRepository,
			rolePolicy,
			new PostWorkflowService(),
			new AdminPostApiMapper(),
			auditLogService,
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
	void updateLabAnnouncementRejectsNullCommandActorPostIdAndInvalidInputs() {
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.updateLabAnnouncement(null));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.updateLabAnnouncement(new AdminPostService.UpdateAdminLabAnnouncementCommand(
						null,
						UUID.randomUUID(),
						"Updated Announcement",
						null,
						"Updated full content",
						PostVisibility.PUBLIC)));

		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));

		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.updateLabAnnouncement(new AdminPostService.UpdateAdminLabAnnouncementCommand(
						actor.getId(),
						null,
						"Updated Announcement",
						null,
						"Updated full content",
						PostVisibility.PUBLIC)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.updateLabAnnouncement(updateCommand(actor.getId(), UUID.randomUUID(), " ", null,
						"Updated full content", PostVisibility.PUBLIC)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.updateLabAnnouncement(updateCommand(actor.getId(), UUID.randomUUID(), "A".repeat(256),
						null, "Updated full content", PostVisibility.PUBLIC)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.updateLabAnnouncement(updateCommand(actor.getId(), UUID.randomUUID(), "Updated Announcement",
						null, " ", PostVisibility.PUBLIC)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.updateLabAnnouncement(updateCommand(actor.getId(), UUID.randomUUID(), "Updated Announcement",
						null, "Updated full content", null)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.updateLabAnnouncement(updateCommand(actor.getId(), UUID.randomUUID(), "Updated Announcement",
						null, "Updated full content", PostVisibility.PROJECT_INTERNAL)));

		verify(postRepository, never()).findAdminPostDetail(any(), any());
		verify(postRepository, never()).saveAndFlush(any(Post.class));
		verify(auditLogService, never()).record(any(AuditLogService.AuditCommand.class));
	}

	@Test
	void updateLabAnnouncementRejectsMissingOrNonAdminActorThroughRolePolicy() {
		UUID missingActorId = UUID.randomUUID();
		when(userRepository.findById(missingActorId)).thenReturn(Optional.empty());
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.updateLabAnnouncement(updateCommand(missingActorId, UUID.randomUUID())));

		Lab lab = lab(UUID.randomUUID());
		User memberActor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(memberActor, role(UUID.randomUUID(), AdminRolePolicy.MEMBER_ROLE_CODE));
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.updateLabAnnouncement(updateCommand(memberActor.getId(), UUID.randomUUID())));

		verify(postRepository, never()).findAdminPostDetail(any(), any());
		verify(postRepository, never()).saveAndFlush(any(Post.class));
		verify(auditLogService, never()).record(any(AuditLogService.AuditCommand.class));
	}

	@Test
	void updateLabAnnouncementAdminUpdatesAnnouncementAndPreservesImmutableFields() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setContentType(PostContentType.LAB_ANNOUNCEMENT);
		UUID postId = post.getId();
		User author = post.getAuthor();
		String slug = post.getSlug();
		Project project = post.getProject();
		PostCategory category = post.getCategory();
		File coverFile = post.getCoverFile();
		PostStatus moderationStatus = post.getModerationStatus();
		OffsetDateTime publishedAt = post.getPublishedAt();
		OffsetDateTime createdAt = post.getCreatedAt();
		OffsetDateTime updatedAt = post.getUpdatedAt();
		PostAttachment attachment = attachment(post, "00000000-0000-0000-0000-000000000001");
		PostModerationLog log = moderationLog(post, "00000000-0000-0000-0000-000000000011");
		when(postRepository.findAdminPostDetail(lab.getId(), postId)).thenReturn(Optional.of(post));
		stubSaveAndFlush();
		when(postAttachmentRepository.findVisibleAdminPostAttachments(post)).thenReturn(List.of(attachment));
		when(postModerationLogRepository.findAdminPostModerationHistory(post)).thenReturn(List.of(log));

		var response = service.updateLabAnnouncement(updateCommand(
				actor.getId(),
				postId,
				"  Updated Announcement  ",
				"  Updated summary  ",
				"  Updated full content  ",
				PostVisibility.LAB_INTERNAL));

		verify(postRepository).findAdminPostDetail(lab.getId(), postId);
		verify(postRepository).saveAndFlush(post);
		verify(postAttachmentRepository).findVisibleAdminPostAttachments(post);
		verify(postModerationLogRepository).findAdminPostModerationHistory(post);
		assertEquals(postId, post.getId());
		assertEquals(lab, post.getLab());
		assertEquals(author, post.getAuthor());
		assertEquals(slug, post.getSlug());
		assertEquals(PostContentType.LAB_ANNOUNCEMENT, post.getContentType());
		assertEquals(project, post.getProject());
		assertEquals(category, post.getCategory());
		assertEquals(coverFile, post.getCoverFile());
		assertEquals(moderationStatus, post.getModerationStatus());
		assertEquals(publishedAt, post.getPublishedAt());
		assertNull(post.getReviewedBy());
		assertNull(post.getReviewedAt());
		assertNull(post.getReviewNote());
		assertEquals(createdAt, post.getCreatedAt());
		assertEquals(updatedAt, post.getUpdatedAt());
		assertNull(post.getDeletedAt());
		assertEquals("Updated Announcement", post.getTitle());
		assertEquals("Updated summary", post.getSummary());
		assertEquals("Updated full content", post.getContent());
		assertEquals(PostVisibility.LAB_INTERNAL, post.getVisibility());
		assertEquals(PostContentType.LAB_ANNOUNCEMENT, response.contentType());
		assertEquals(attachment.getId(), response.attachments().getFirst().attachmentId());
		assertEquals(log.getId(), response.moderationHistory().getFirst().id());
	}

	@Test
	void updateLabAnnouncementAcceptsSuperAdminAndAllowedVisibilities() {
		for (PostVisibility visibility : List.of(
				PostVisibility.PUBLIC,
				PostVisibility.LAB_INTERNAL,
				PostVisibility.PRIVATE)) {
			org.mockito.Mockito.clearInvocations(
					postRepository,
					postAttachmentRepository,
					postModerationLogRepository,
					auditLogService,
					userRepository,
					roleRepository,
					userRoleRepository);
			Lab lab = lab(UUID.randomUUID());
			User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
			stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.SUPER_ADMIN_ROLE_CODE));
			Post post = post(lab);
			post.setContentType(PostContentType.LAB_ANNOUNCEMENT);
			when(postRepository.findAdminPostDetail(lab.getId(), post.getId())).thenReturn(Optional.of(post));
			stubSaveAndFlush();
			when(postAttachmentRepository.findVisibleAdminPostAttachments(post)).thenReturn(List.of());
			when(postModerationLogRepository.findAdminPostModerationHistory(post)).thenReturn(List.of());
			String summary = visibility == PostVisibility.PUBLIC ? null : " ";

			var response = service.updateLabAnnouncement(updateCommand(
					actor.getId(),
					post.getId(),
					"Updated Announcement",
					summary,
					"Updated full content",
					visibility));

			assertEquals(visibility, post.getVisibility());
			assertEquals(visibility, response.visibility());
			assertNull(post.getSummary());
		}
	}

	@Test
	void updateLabAnnouncementEmptyLookupBecomesGenericNotFound() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		UUID postId = UUID.randomUUID();
		when(postRepository.findAdminPostDetail(lab.getId(), postId)).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(
				ResourceNotFoundException.class,
				() -> service.updateLabAnnouncement(updateCommand(actor.getId(), postId)));

		assertEquals("Post was not found.", exception.getMessage());
		verify(postRepository).findAdminPostDetail(lab.getId(), postId);
		verify(postRepository, never()).saveAndFlush(any(Post.class));
		verify(postAttachmentRepository, never()).findVisibleAdminPostAttachments(any(Post.class));
		verify(postModerationLogRepository, never()).findAdminPostModerationHistory(any(Post.class));
		verify(auditLogService, never()).record(any(AuditLogService.AuditCommand.class));
	}

	@Test
	void updateLabAnnouncementWrongContentTypeReturnsGenericNotFoundWithoutMutation() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setContentType(PostContentType.NEWS);
		String originalTitle = post.getTitle();
		String originalSummary = post.getSummary();
		String originalContent = post.getContent();
		PostVisibility originalVisibility = post.getVisibility();
		when(postRepository.findAdminPostDetail(lab.getId(), post.getId())).thenReturn(Optional.of(post));

		ResourceNotFoundException exception = assertThrows(
				ResourceNotFoundException.class,
				() -> service.updateLabAnnouncement(updateCommand(actor.getId(), post.getId())));

		assertEquals("Post was not found.", exception.getMessage());
		assertEquals(originalTitle, post.getTitle());
		assertEquals(originalSummary, post.getSummary());
		assertEquals(originalContent, post.getContent());
		assertEquals(originalVisibility, post.getVisibility());
		verify(postRepository, never()).saveAndFlush(any(Post.class));
		verify(postAttachmentRepository, never()).findVisibleAdminPostAttachments(any(Post.class));
		verify(postModerationLogRepository, never()).findAdminPostModerationHistory(any(Post.class));
		verify(auditLogService, never()).record(any(AuditLogService.AuditCommand.class));
	}

	@Test
	void updateLabAnnouncementPersistsBeforeAuditAndRecordsSafeOrderedSnapshots() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setContentType(PostContentType.LAB_ANNOUNCEMENT);
		post.setTitle("Original Announcement");
		post.setSlug("original-announcement");
		post.setSummary("Old summary");
		post.setContent("Old content");
		post.setVisibility(PostVisibility.PUBLIC);
		post.setModerationStatus(PostStatus.PUBLISHED);
		post.setPublishedAt(OffsetDateTime.parse("2026-07-20T10:15:30Z"));
		when(postRepository.findAdminPostDetail(lab.getId(), post.getId())).thenReturn(Optional.of(post));
		stubSaveAndFlush();
		when(postAttachmentRepository.findVisibleAdminPostAttachments(post)).thenReturn(List.of());
		when(postModerationLogRepository.findAdminPostModerationHistory(post)).thenReturn(List.of());

		service.updateLabAnnouncement(updateCommand(
				actor.getId(),
				post.getId(),
				"Updated Announcement",
				null,
				"Updated full content",
				PostVisibility.PRIVATE));

		InOrder inOrder = inOrder(postRepository, auditLogService, postAttachmentRepository, postModerationLogRepository);
		inOrder.verify(postRepository).saveAndFlush(post);
		ArgumentCaptor<AuditLogService.AuditCommand> auditCaptor =
				ArgumentCaptor.forClass(AuditLogService.AuditCommand.class);
		inOrder.verify(auditLogService).record(auditCaptor.capture());
		inOrder.verify(postAttachmentRepository).findVisibleAdminPostAttachments(post);
		inOrder.verify(postModerationLogRepository).findAdminPostModerationHistory(post);
		AuditLogService.AuditCommand audit = auditCaptor.getValue();
		assertEquals(actor.getId(), audit.actorId());
		assertEquals("UPDATE_LAB_ANNOUNCEMENT", audit.action());
		assertEquals("LAB_ANNOUNCEMENT", audit.entityType());
		assertEquals(post.getId(), audit.entityId());
		assertSnapshot(
				(Map<?, ?>) audit.oldValue(),
				"Original Announcement",
				"original-announcement",
				PostVisibility.PUBLIC,
				PostStatus.PUBLISHED,
				OffsetDateTime.parse("2026-07-20T10:15:30Z"),
				11,
				11);
		assertSnapshot(
				(Map<?, ?>) audit.newValue(),
				"Updated Announcement",
				"original-announcement",
				PostVisibility.PRIVATE,
				PostStatus.PUBLISHED,
				OffsetDateTime.parse("2026-07-20T10:15:30Z"),
				0,
				20);
		assertFalse(((Map<?, ?>) audit.oldValue()).containsKey("summary"));
		assertFalse(((Map<?, ?>) audit.oldValue()).containsKey("content"));
		assertFalse(((Map<?, ?>) audit.newValue()).containsKey("summary"));
		assertFalse(((Map<?, ?>) audit.newValue()).containsKey("content"));
	}

	@Test
	void updateLabAnnouncementPersistenceFailurePreventsAuditAndChildLoading() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminRolePolicy.ADMIN_ROLE_CODE));
		Post post = post(lab);
		post.setContentType(PostContentType.LAB_ANNOUNCEMENT);
		RuntimeException failure = new RuntimeException("save failed");
		when(postRepository.findAdminPostDetail(lab.getId(), post.getId())).thenReturn(Optional.of(post));
		when(postRepository.saveAndFlush(post)).thenThrow(failure);

		RuntimeException exception = assertThrows(
				RuntimeException.class,
				() -> service.updateLabAnnouncement(updateCommand(actor.getId(), post.getId())));

		assertEquals(failure, exception);
		verify(auditLogService, never()).record(any(AuditLogService.AuditCommand.class));
		verify(postAttachmentRepository, never()).findVisibleAdminPostAttachments(any(Post.class));
		verify(postModerationLogRepository, never()).findAdminPostModerationHistory(any(Post.class));
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
				"updateLabAnnouncement",
				AdminPostService.UpdateAdminLabAnnouncementCommand.class));
		assertMutationTransaction(AdminPostService.class.getMethod(
				"approvePost",
				AdminPostService.ApproveAdminPostCommand.class));
	}

	@Test
	void serviceDoesNotIntroduceNotificationDependencyForLabAnnouncementUpdate() {
		assertFalse(java.util.Arrays.stream(AdminPostService.class.getDeclaredFields())
				.anyMatch(field -> field.getType().getSimpleName().contains("Notification")));
		assertFalse(java.util.Arrays.stream(AdminPostService.class.getConstructors())
				.flatMap(constructor -> java.util.Arrays.stream(constructor.getParameterTypes()))
				.anyMatch(type -> type.getSimpleName().contains("Notification")));
	}

	private void stubActiveActor(User actor, Role role) {
		when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
		when(userRoleRepository.findByUserAndStatus(actor, UserRoleStatus.ACTIVE))
				.thenReturn(List.of(userRole(actor, role)));
	}

	private void stubSaveAndFlush() {
		when(postRepository.saveAndFlush(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private static AdminPostService.UpdateAdminLabAnnouncementCommand updateCommand(UUID actorUserId, UUID postId) {
		return updateCommand(
				actorUserId,
				postId,
				"Updated Announcement",
				"Updated summary",
				"Updated full content",
				PostVisibility.PUBLIC);
	}

	private static AdminPostService.UpdateAdminLabAnnouncementCommand updateCommand(
			UUID actorUserId,
			UUID postId,
			String title,
			String summary,
			String content,
			PostVisibility visibility) {
		return new AdminPostService.UpdateAdminLabAnnouncementCommand(
				actorUserId,
				postId,
				title,
				summary,
				content,
				visibility);
	}

	private static void assertSnapshot(
			Map<?, ?> snapshot,
			String title,
			String slug,
			PostVisibility visibility,
			PostStatus moderationStatus,
			OffsetDateTime publishedAt,
			int summaryLength,
			int contentLength) {
		assertEquals(List.of(
				"title",
				"slug",
				"visibility",
				"moderationStatus",
				"publishedAt",
				"summaryLength",
				"contentLength"), List.copyOf(snapshot.keySet()));
		assertEquals(title, snapshot.get("title"));
		assertEquals(slug, snapshot.get("slug"));
		assertEquals(visibility, snapshot.get("visibility"));
		assertEquals(moderationStatus, snapshot.get("moderationStatus"));
		assertEquals(publishedAt, snapshot.get("publishedAt"));
		assertEquals(summaryLength, snapshot.get("summaryLength"));
		assertEquals(contentLength, snapshot.get("contentLength"));
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

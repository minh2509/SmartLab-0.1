package com.smartlab.controller.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.smartlab.dto.response.admin.AdminPostDetailResponse;
import com.smartlab.dto.response.admin.AdminPostModerationActionResponse;
import com.smartlab.dto.response.admin.AdminPostPageResponse;
import com.smartlab.dto.response.admin.AdminPostSummaryResponse;
import com.smartlab.enums.PostContentType;
import com.smartlab.enums.PostModerationAction;
import com.smartlab.enums.PostStatus;
import com.smartlab.enums.PostVisibility;
import com.smartlab.exception.ApiExceptionHandler;
import com.smartlab.exception.ConflictingAdminOperationException;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminPostService;

class AdminPostControllerTests {

	private final AdminPostService adminPostService = mock(AdminPostService.class);
	private final AuthenticatedActorResolver actorResolver = mock(AuthenticatedActorResolver.class);
	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(
					new AdminPostController(adminPostService, actorResolver),
					new AdminLabAnnouncementController(adminPostService, actorResolver))
			.setControllerAdvice(new ApiExceptionHandler())
			.setValidator(validator())
			.build();

	@Test
	void listPostsWithoutFiltersReturnsPaginationContractAndOmitsSensitiveFields() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminPostService.listPosts(any(AdminPostService.ListAdminPostsQuery.class)))
				.thenReturn(new AdminPostPageResponse(
						List.of(summary(postId)),
						0,
						20,
						1,
						1,
						true,
						true));

		mockMvc.perform(get("/api/admin/posts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(postId.toString()))
				.andExpect(jsonPath("$.content[0].title").value("Post Title"))
				.andExpect(jsonPath("$.content[0].content").doesNotExist())
				.andExpect(jsonPath("$.content[0].authorEmail").doesNotExist())
				.andExpect(jsonPath("$.content[0].reviewNote").doesNotExist())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1))
				.andExpect(jsonPath("$.first").value(true))
				.andExpect(jsonPath("$.last").value(true))
				.andExpect(content().string(not(containsString("Full private content"))))
				.andExpect(content().string(not(containsString("author@example.edu"))));

		ArgumentCaptor<AdminPostService.ListAdminPostsQuery> captor =
				ArgumentCaptor.forClass(AdminPostService.ListAdminPostsQuery.class);
		verify(adminPostService).listPosts(captor.capture());
		assertEquals(actorUserId, captor.getValue().actorUserId());
		assertEquals(null, captor.getValue().page());
		assertEquals(null, captor.getValue().size());
	}

	@Test
	void listPostsBindsAllFilters() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminPostService.listPosts(any(AdminPostService.ListAdminPostsQuery.class)))
				.thenReturn(new AdminPostPageResponse(List.of(), 2, 50, 0, 0, true, true));

		mockMvc.perform(get("/api/admin/posts")
						.param("page", "2")
						.param("size", "50")
						.param("keyword", "  robotics  ")
						.param("status", "PENDING_REVIEW")
						.param("type", "NEWS")
						.param("authorId", authorId.toString())
						.param("projectId", projectId.toString())
						.param("visibility", "LAB_INTERNAL"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.page").value(2))
				.andExpect(jsonPath("$.size").value(50));

		ArgumentCaptor<AdminPostService.ListAdminPostsQuery> captor =
				ArgumentCaptor.forClass(AdminPostService.ListAdminPostsQuery.class);
		verify(adminPostService).listPosts(captor.capture());
		assertEquals("  robotics  ", captor.getValue().keyword());
		assertEquals(PostStatus.PENDING_REVIEW, captor.getValue().status());
		assertEquals(PostContentType.NEWS, captor.getValue().contentType());
		assertEquals(authorId, captor.getValue().authorId());
		assertEquals(projectId, captor.getValue().projectId());
		assertEquals(PostVisibility.LAB_INTERNAL, captor.getValue().visibility());
	}

	@Test
	void invalidEnumAndUuidReturnBadRequestThroughExistingExceptionHandler() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());

		mockMvc.perform(get("/api/admin/posts").param("status", "not-a-status"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed."));
		mockMvc.perform(get("/api/admin/posts").param("type", "not-a-type"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed."));
		mockMvc.perform(get("/api/admin/posts").param("visibility", "not-a-visibility"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed."));
		mockMvc.perform(get("/api/admin/posts").param("authorId", "not-a-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed."));
	}

	@Test
	void invalidPageAndSizeReturnBadRequestThroughExistingExceptionHandler() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());
		when(adminPostService.listPosts(any(AdminPostService.ListAdminPostsQuery.class)))
				.thenThrow(new InvalidAdminServiceInputException("Page or size is invalid."));

		mockMvc.perform(get("/api/admin/posts").param("page", "-1"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/admin/posts").param("size", "0"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/admin/posts").param("size", "101"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void listPendingPostsReturnsPaginationContractAndOmitsSensitiveFields() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminPostService.listPendingPosts(any(AdminPostService.ListPendingAdminPostsQuery.class)))
				.thenReturn(new AdminPostPageResponse(
						List.of(summary(postId)),
						1,
						10,
						11,
						2,
						false,
						true));

		mockMvc.perform(get("/api/admin/posts/pending")
						.param("page", "1")
						.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(postId.toString()))
				.andExpect(jsonPath("$.content[0].content").doesNotExist())
				.andExpect(jsonPath("$.content[0].authorEmail").doesNotExist())
				.andExpect(jsonPath("$.content[0].reviewNote").doesNotExist())
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.totalElements").value(11))
				.andExpect(jsonPath("$.totalPages").value(2))
				.andExpect(jsonPath("$.first").value(false))
				.andExpect(jsonPath("$.last").value(true))
				.andExpect(content().string(not(containsString("Full private content"))))
				.andExpect(content().string(not(containsString("author@example.edu"))));

		ArgumentCaptor<AdminPostService.ListPendingAdminPostsQuery> captor =
				ArgumentCaptor.forClass(AdminPostService.ListPendingAdminPostsQuery.class);
		verify(adminPostService).listPendingPosts(captor.capture());
		assertEquals(actorUserId, captor.getValue().actorUserId());
		assertEquals(1, captor.getValue().page());
		assertEquals(10, captor.getValue().size());
	}

	@Test
	void listPendingPostsSupportsDefaultPagination() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminPostService.listPendingPosts(any(AdminPostService.ListPendingAdminPostsQuery.class)))
				.thenReturn(new AdminPostPageResponse(List.of(), 0, 20, 0, 0, true, true));

		mockMvc.perform(get("/api/admin/posts/pending"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20));

		ArgumentCaptor<AdminPostService.ListPendingAdminPostsQuery> captor =
				ArgumentCaptor.forClass(AdminPostService.ListPendingAdminPostsQuery.class);
		verify(adminPostService).listPendingPosts(captor.capture());
		assertEquals(actorUserId, captor.getValue().actorUserId());
		assertEquals(null, captor.getValue().page());
		assertEquals(null, captor.getValue().size());
	}

	@Test
	void invalidPendingPageAndSizeReturnBadRequestThroughExistingExceptionHandler() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());
		when(adminPostService.listPendingPosts(any(AdminPostService.ListPendingAdminPostsQuery.class)))
				.thenThrow(new InvalidAdminServiceInputException("Page or size is invalid."));

		mockMvc.perform(get("/api/admin/posts/pending").param("page", "-1"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/admin/posts/pending").param("size", "0"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/admin/posts/pending").param("size", "101"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void requestWithoutAuthenticatedActorIsUnauthorized() throws Exception {
		when(actorResolver.requireActorUserId())
				.thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required."));

		mockMvc.perform(get("/api/admin/posts"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void pendingRequestWithoutAuthenticatedActorIsUnauthorized() throws Exception {
		when(actorResolver.requireActorUserId())
				.thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required."));

		mockMvc.perform(get("/api/admin/posts/pending"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void listLabAnnouncementsReturnsPaginationContractAndOmitsSensitiveFields() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminPostService.listLabAnnouncements(any(AdminPostService.ListLabAnnouncementsQuery.class)))
				.thenReturn(new AdminPostPageResponse(
						List.of(summary(postId, PostContentType.LAB_ANNOUNCEMENT)),
						0,
						20,
						1,
						1,
						true,
						true));

		mockMvc.perform(get("/api/admin/lab-announcements"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(postId.toString()))
				.andExpect(jsonPath("$.content[0].contentType").value("LAB_ANNOUNCEMENT"))
				.andExpect(jsonPath("$.content[0].content").doesNotExist())
				.andExpect(jsonPath("$.content[0].authorEmail").doesNotExist())
				.andExpect(jsonPath("$.content[0].reviewNote").doesNotExist())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1))
				.andExpect(jsonPath("$.first").value(true))
				.andExpect(jsonPath("$.last").value(true))
				.andExpect(content().string(not(containsString("Full private content"))))
				.andExpect(content().string(not(containsString("author@example.edu"))));

		ArgumentCaptor<AdminPostService.ListLabAnnouncementsQuery> captor =
				ArgumentCaptor.forClass(AdminPostService.ListLabAnnouncementsQuery.class);
		verify(adminPostService).listLabAnnouncements(captor.capture());
		assertEquals(actorUserId, captor.getValue().actorUserId());
		assertEquals(null, captor.getValue().page());
		assertEquals(null, captor.getValue().size());
	}

	@Test
	void listLabAnnouncementsSupportsExplicitPaginationAndEmptyPage() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminPostService.listLabAnnouncements(any(AdminPostService.ListLabAnnouncementsQuery.class)))
				.thenReturn(new AdminPostPageResponse(List.of(), 2, 50, 0, 0, false, true));

		mockMvc.perform(get("/api/admin/lab-announcements")
						.param("page", "2")
						.param("size", "50"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.content").isEmpty())
				.andExpect(jsonPath("$.page").value(2))
				.andExpect(jsonPath("$.size").value(50))
				.andExpect(jsonPath("$.totalElements").value(0));

		ArgumentCaptor<AdminPostService.ListLabAnnouncementsQuery> captor =
				ArgumentCaptor.forClass(AdminPostService.ListLabAnnouncementsQuery.class);
		verify(adminPostService).listLabAnnouncements(captor.capture());
		assertEquals(actorUserId, captor.getValue().actorUserId());
		assertEquals(2, captor.getValue().page());
		assertEquals(50, captor.getValue().size());
	}

	@Test
	void invalidLabAnnouncementPageAndSizeReturnBadRequestThroughExistingExceptionHandler() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());
		when(adminPostService.listLabAnnouncements(any(AdminPostService.ListLabAnnouncementsQuery.class)))
				.thenThrow(new InvalidAdminServiceInputException("Page or size is invalid."));

		mockMvc.perform(get("/api/admin/lab-announcements").param("page", "-1"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/admin/lab-announcements").param("size", "0"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/admin/lab-announcements").param("size", "101"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void labAnnouncementRequestWithoutAuthenticatedActorIsUnauthorized() throws Exception {
		when(actorResolver.requireActorUserId())
				.thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required."));

		mockMvc.perform(get("/api/admin/lab-announcements"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void getLabAnnouncementDetailReturnsFullDetailContractAndOmitsSensitiveFields() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminPostService.getLabAnnouncementDetail(
				any(AdminPostService.GetAdminLabAnnouncementDetailQuery.class)))
						.thenReturn(detail(postId, false, PostContentType.LAB_ANNOUNCEMENT));

		mockMvc.perform(get("/api/admin/lab-announcements/{postId}", postId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(postId.toString()))
				.andExpect(jsonPath("$.content").value("Full private content"))
				.andExpect(jsonPath("$.contentType").value("LAB_ANNOUNCEMENT"))
				.andExpect(jsonPath("$.author.fullName").value("Author Name"))
				.andExpect(jsonPath("$.attachments[0].originalName").value("attachment.pdf"))
				.andExpect(jsonPath("$.moderationHistory[0].action").value("SUBMIT"))
				.andExpect(jsonPath("$.authorEmail").doesNotExist())
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.reviewNote").doesNotExist())
				.andExpect(jsonPath("$.storedName").doesNotExist())
				.andExpect(jsonPath("$.storagePath").doesNotExist())
				.andExpect(jsonPath("$.deletedAt").doesNotExist())
				.andExpect(content().string(not(containsString("\"authorEmail\""))))
				.andExpect(content().string(not(containsString("\"email\""))))
				.andExpect(content().string(not(containsString("\"passwordHash\""))))
				.andExpect(content().string(not(containsString("\"reviewNote\""))))
				.andExpect(content().string(not(containsString("\"storedName\""))))
				.andExpect(content().string(not(containsString("\"storagePath\""))))
				.andExpect(content().string(not(containsString("\"deletedAt\""))))
				.andExpect(content().string(not(containsString("author@example.edu"))))
				.andExpect(content().string(not(containsString("stored-name"))))
				.andExpect(content().string(not(containsString("/private/storage"))));

		ArgumentCaptor<AdminPostService.GetAdminLabAnnouncementDetailQuery> captor =
				ArgumentCaptor.forClass(AdminPostService.GetAdminLabAnnouncementDetailQuery.class);
		verify(adminPostService).getLabAnnouncementDetail(captor.capture());
		assertEquals(actorUserId, captor.getValue().actorUserId());
		assertEquals(postId, captor.getValue().postId());
	}

	@Test
	void labAnnouncementDetailRequestWithoutAuthenticatedActorIsUnauthorized() throws Exception {
		when(actorResolver.requireActorUserId())
				.thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required."));

		mockMvc.perform(get("/api/admin/lab-announcements/{postId}", UUID.randomUUID()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").value("Invalid email or password."));
	}

	@Test
	void malformedLabAnnouncementDetailPostIdReturnsBadRequest() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());

		mockMvc.perform(get("/api/admin/lab-announcements/not-a-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed."));
	}

	@Test
	void labAnnouncementDetailMissingResourceUsesGenericNotFoundMessage() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());
		when(adminPostService.getLabAnnouncementDetail(
				any(AdminPostService.GetAdminLabAnnouncementDetailQuery.class)))
						.thenThrow(new ResourceNotFoundException("Post was not found."));

		mockMvc.perform(get("/api/admin/lab-announcements/{postId}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Post was not found."));
	}

	@Test
	void getPostDetailReturnsFullDetailContractAndOmitsSensitiveFields() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminPostService.getPostDetail(any(AdminPostService.GetAdminPostDetailQuery.class)))
				.thenReturn(detail(postId, false));

		mockMvc.perform(get("/api/admin/posts/{postId}", postId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(postId.toString()))
				.andExpect(jsonPath("$.content").value("Full private content"))
				.andExpect(jsonPath("$.author.id").exists())
				.andExpect(jsonPath("$.author.fullName").value("Author Name"))
				.andExpect(jsonPath("$.project.name").value("Project Name"))
				.andExpect(jsonPath("$.category.name").value("Category Name"))
				.andExpect(jsonPath("$.coverFile.originalName").value("cover.png"))
				.andExpect(jsonPath("$.attachments[0].originalName").value("attachment.pdf"))
				.andExpect(jsonPath("$.attachments[0].uploadedByName").value("Uploader Name"))
				.andExpect(jsonPath("$.moderationHistory[0].action").value("SUBMIT"))
				.andExpect(jsonPath("$.moderationHistory[0].actorName").value("Moderator Name"))
				.andExpect(jsonPath("$.authorEmail").doesNotExist())
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.reviewNote").doesNotExist())
				.andExpect(jsonPath("$.storedName").doesNotExist())
				.andExpect(jsonPath("$.storagePath").doesNotExist())
				.andExpect(jsonPath("$.deletedAt").doesNotExist())
				.andExpect(content().string(not(containsString("author@example.edu"))))
				.andExpect(content().string(not(containsString("stored-name"))))
				.andExpect(content().string(not(containsString("/private/storage"))));

		ArgumentCaptor<AdminPostService.GetAdminPostDetailQuery> captor =
				ArgumentCaptor.forClass(AdminPostService.GetAdminPostDetailQuery.class);
		verify(adminPostService).getPostDetail(captor.capture());
		assertEquals(actorUserId, captor.getValue().actorUserId());
		assertEquals(postId, captor.getValue().postId());
	}

	@Test
	void getPostDetailSerializesNullableOptionalRelationships() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminPostService.getPostDetail(any(AdminPostService.GetAdminPostDetailQuery.class)))
				.thenReturn(detail(postId, true));

		mockMvc.perform(get("/api/admin/posts/{postId}", postId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.project").doesNotExist())
				.andExpect(jsonPath("$.category").doesNotExist())
				.andExpect(jsonPath("$.coverFile").doesNotExist());
	}

	@Test
	void detailRequestWithoutAuthenticatedActorIsUnauthorized() throws Exception {
		when(actorResolver.requireActorUserId())
				.thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required."));

		mockMvc.perform(get("/api/admin/posts/{postId}", UUID.randomUUID()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void malformedDetailPostIdReturnsBadRequest() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());

		mockMvc.perform(get("/api/admin/posts/not-a-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed."));
	}

	@Test
	void approvePostReturnsModerationActionContractAndPassesActorAndPostId() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();
		UUID reviewedById = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminPostService.approvePost(any(AdminPostService.ApproveAdminPostCommand.class)))
				.thenReturn(moderationAction(postId, reviewedById));

		mockMvc.perform(post("/api/admin/posts/{postId}/approve", postId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postId").value(postId.toString()))
				.andExpect(jsonPath("$.action").value("APPROVE"))
				.andExpect(jsonPath("$.fromStatus").value("PENDING_REVIEW"))
				.andExpect(jsonPath("$.toStatus").value("APPROVED"))
				.andExpect(jsonPath("$.moderationStatus").value("APPROVED"))
				.andExpect(jsonPath("$.reviewedById").value(reviewedById.toString()))
				.andExpect(jsonPath("$.reviewedByName").value("Admin Reviewer"))
				.andExpect(jsonPath("$.reviewedAt").value("2026-07-23T08:15:30Z"))
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.username").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.reviewNote").doesNotExist())
				.andExpect(jsonPath("$.storagePath").doesNotExist())
				.andExpect(jsonPath("$.deletedAt").doesNotExist())
				.andExpect(content().string(not(containsString("admin@example.edu"))))
				.andExpect(content().string(not(containsString("stale note"))));

		ArgumentCaptor<AdminPostService.ApproveAdminPostCommand> captor =
				ArgumentCaptor.forClass(AdminPostService.ApproveAdminPostCommand.class);
		verify(adminPostService).approvePost(captor.capture());
		assertEquals(actorUserId, captor.getValue().actorUserId());
		assertEquals(postId, captor.getValue().postId());
	}

	@Test
	void malformedApprovePostIdReturnsBadRequest() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());

		mockMvc.perform(post("/api/admin/posts/not-a-uuid/approve"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed."));
	}

	@Test
	void approvePostConflictUsesExistingApiErrorFormat() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());
		when(adminPostService.approvePost(any(AdminPostService.ApproveAdminPostCommand.class)))
				.thenThrow(new ConflictingAdminOperationException("Post transition is not allowed."));

		mockMvc.perform(post("/api/admin/posts/{postId}/approve", UUID.randomUUID()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.error").value("Conflict"))
				.andExpect(jsonPath("$.message").value("Post transition is not allowed."));
	}

	@Test
	void approvePostMissingResourceUsesGenericNotFoundMessage() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());
		when(adminPostService.approvePost(any(AdminPostService.ApproveAdminPostCommand.class)))
				.thenThrow(new ResourceNotFoundException("Post was not found."));

		mockMvc.perform(post("/api/admin/posts/{postId}/approve", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Post was not found."));
	}

	@Test
	void publishPostReturnsModerationActionContractAndPassesActorAndPostId() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();
		UUID reviewedById = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminPostService.publishPost(any(AdminPostService.PublishAdminPostCommand.class)))
				.thenReturn(publishAction(postId, reviewedById));

		mockMvc.perform(patch("/api/admin/posts/{postId}/publish", postId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postId").value(postId.toString()))
				.andExpect(jsonPath("$.action").value("PUBLISH"))
				.andExpect(jsonPath("$.fromStatus").value("APPROVED"))
				.andExpect(jsonPath("$.toStatus").value("PUBLISHED"))
				.andExpect(jsonPath("$.moderationStatus").value("PUBLISHED"))
				.andExpect(jsonPath("$.reviewedById").value(reviewedById.toString()))
				.andExpect(jsonPath("$.reviewedByName").value("Admin Reviewer"))
				.andExpect(jsonPath("$.reviewedAt").value("2026-07-23T08:15:30Z"))
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.username").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.reviewNote").doesNotExist())
				.andExpect(jsonPath("$.storagePath").doesNotExist())
				.andExpect(jsonPath("$.deletedAt").doesNotExist())
				.andExpect(content().string(not(containsString("admin@example.edu"))))
				.andExpect(content().string(not(containsString("Full private content"))));

		ArgumentCaptor<AdminPostService.PublishAdminPostCommand> captor =
				ArgumentCaptor.forClass(AdminPostService.PublishAdminPostCommand.class);
		verify(adminPostService).publishPost(captor.capture());
		assertEquals(actorUserId, captor.getValue().actorUserId());
		assertEquals(postId, captor.getValue().postId());
	}

	@Test
	void publishPostRequestWithoutAuthenticatedActorIsUnauthorized() throws Exception {
		when(actorResolver.requireActorUserId())
				.thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required."));

		mockMvc.perform(patch("/api/admin/posts/{postId}/publish", UUID.randomUUID()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void malformedPublishPostIdReturnsBadRequest() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());

		mockMvc.perform(patch("/api/admin/posts/not-a-uuid/publish"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed."));
	}

	@Test
	void publishPostConflictUsesExistingApiErrorFormat() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());
		when(adminPostService.publishPost(any(AdminPostService.PublishAdminPostCommand.class)))
				.thenThrow(new ConflictingAdminOperationException("Only approved posts can be published."));

		mockMvc.perform(patch("/api/admin/posts/{postId}/publish", UUID.randomUUID()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.error").value("Conflict"))
				.andExpect(jsonPath("$.message").value("Only approved posts can be published."));
	}

	@Test
	void publishPostMissingResourceUsesGenericNotFoundMessage() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());
		when(adminPostService.publishPost(any(AdminPostService.PublishAdminPostCommand.class)))
				.thenThrow(new ResourceNotFoundException("Post was not found."));

		mockMvc.perform(patch("/api/admin/posts/{postId}/publish", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Post was not found."));
	}

	@Test
	void unpublishPostReturnsModerationActionContractAndPassesActorAndPostId() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();
		UUID reviewedById = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminPostService.unpublishPost(any(AdminPostService.UnpublishAdminPostCommand.class)))
				.thenReturn(unpublishAction(postId, reviewedById));

		mockMvc.perform(patch("/api/admin/posts/{postId}/unpublish", postId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postId").value(postId.toString()))
				.andExpect(jsonPath("$.action").value("UNPUBLISH"))
				.andExpect(jsonPath("$.fromStatus").value("PUBLISHED"))
				.andExpect(jsonPath("$.toStatus").value("APPROVED"))
				.andExpect(jsonPath("$.moderationStatus").value("APPROVED"))
				.andExpect(jsonPath("$.reviewedById").value(reviewedById.toString()))
				.andExpect(jsonPath("$.reviewedByName").value("Admin Reviewer"))
				.andExpect(jsonPath("$.reviewedAt").value("2026-07-23T08:15:30Z"))
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.username").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.reviewNote").doesNotExist())
				.andExpect(jsonPath("$.storagePath").doesNotExist())
				.andExpect(jsonPath("$.deletedAt").doesNotExist())
				.andExpect(content().string(not(containsString("admin@example.edu"))))
				.andExpect(content().string(not(containsString("Full private content"))));

		ArgumentCaptor<AdminPostService.UnpublishAdminPostCommand> captor =
				ArgumentCaptor.forClass(AdminPostService.UnpublishAdminPostCommand.class);
		verify(adminPostService).unpublishPost(captor.capture());
		assertEquals(actorUserId, captor.getValue().actorUserId());
		assertEquals(postId, captor.getValue().postId());
	}

	@Test
	void unpublishPostRequestWithoutAuthenticatedActorIsUnauthorized() throws Exception {
		when(actorResolver.requireActorUserId())
				.thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required."));

		mockMvc.perform(patch("/api/admin/posts/{postId}/unpublish", UUID.randomUUID()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void malformedUnpublishPostIdReturnsBadRequest() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());

		mockMvc.perform(patch("/api/admin/posts/not-a-uuid/unpublish"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed."));
	}

	@Test
	void unpublishPostConflictUsesExistingApiErrorFormat() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());
		when(adminPostService.unpublishPost(any(AdminPostService.UnpublishAdminPostCommand.class)))
				.thenThrow(new ConflictingAdminOperationException("Only published posts can be unpublished."));

		mockMvc.perform(patch("/api/admin/posts/{postId}/unpublish", UUID.randomUUID()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.error").value("Conflict"))
				.andExpect(jsonPath("$.message").value("Only published posts can be unpublished."));
	}

	@Test
	void unpublishPostMissingResourceUsesGenericNotFoundMessage() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());
		when(adminPostService.unpublishPost(any(AdminPostService.UnpublishAdminPostCommand.class)))
				.thenThrow(new ResourceNotFoundException("Post was not found."));

		mockMvc.perform(patch("/api/admin/posts/{postId}/unpublish", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Post was not found."));
	}

	private static LocalValidatorFactoryBean validator() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		return validator;
	}

	private static AdminPostSummaryResponse summary(UUID postId) {
		return summary(postId, PostContentType.NEWS);
	}

	private static AdminPostSummaryResponse summary(UUID postId, PostContentType contentType) {
		return new AdminPostSummaryResponse(
				postId,
				"Post Title",
				"post-title",
				"Short summary",
				contentType,
				PostVisibility.PUBLIC,
				PostStatus.PUBLISHED,
				UUID.randomUUID(),
				"Author Name",
				UUID.randomUUID(),
				"Project Name",
				UUID.randomUUID(),
				"Category Name",
				UUID.randomUUID(),
				OffsetDateTime.parse("2026-07-20T10:15:30Z"),
				OffsetDateTime.parse("2026-07-19T10:15:30Z"),
				OffsetDateTime.parse("2026-07-21T10:15:30Z"));
	}

	private static AdminPostDetailResponse detail(UUID postId, boolean nullOptionalRelations) {
		return detail(postId, nullOptionalRelations, PostContentType.NEWS);
	}

	private static AdminPostDetailResponse detail(
			UUID postId,
			boolean nullOptionalRelations,
			PostContentType contentType) {
		return new AdminPostDetailResponse(
				postId,
				"Post Title",
				"post-title",
				"Short summary",
				"Full private content",
				contentType,
				PostVisibility.PUBLIC,
				PostStatus.PENDING_REVIEW,
				new AdminPostDetailResponse.AuthorResponse(UUID.randomUUID(), "Author Name"),
				nullOptionalRelations
						? null
						: new AdminPostDetailResponse.ProjectResponse(UUID.randomUUID(), "Project Name"),
				nullOptionalRelations
						? null
						: new AdminPostDetailResponse.CategoryResponse(UUID.randomUUID(), "Category Name"),
				nullOptionalRelations
						? null
						: new AdminPostDetailResponse.FileResponse(
								UUID.randomUUID(),
								"cover.png",
								"image/png",
								2048L,
								"png",
								OffsetDateTime.parse("2026-07-20T10:15:30Z")),
				List.of(new AdminPostDetailResponse.AttachmentResponse(
						UUID.randomUUID(),
						UUID.randomUUID(),
						"attachment.pdf",
						"application/pdf",
						1024L,
						"pdf",
						UUID.randomUUID(),
						"Uploader Name",
						OffsetDateTime.parse("2026-07-20T10:15:30Z"))),
				List.of(new AdminPostDetailResponse.ModerationHistoryResponse(
						UUID.randomUUID(),
						PostModerationAction.SUBMIT,
						PostStatus.DRAFT,
						PostStatus.PENDING_REVIEW,
						UUID.randomUUID(),
						"Moderator Name",
						"Submitted for review",
						OffsetDateTime.parse("2026-07-20T10:15:30Z"))),
				OffsetDateTime.parse("2026-07-20T10:15:30Z"),
				OffsetDateTime.parse("2026-07-19T10:15:30Z"),
				OffsetDateTime.parse("2026-07-21T10:15:30Z"));
	}

	private static AdminPostModerationActionResponse moderationAction(UUID postId, UUID reviewedById) {
		return new AdminPostModerationActionResponse(
				postId,
				PostModerationAction.APPROVE,
				PostStatus.PENDING_REVIEW,
				PostStatus.APPROVED,
				PostStatus.APPROVED,
				reviewedById,
				"Admin Reviewer",
				OffsetDateTime.parse("2026-07-23T08:15:30Z"));
	}

	private static AdminPostModerationActionResponse publishAction(UUID postId, UUID reviewedById) {
		return new AdminPostModerationActionResponse(
				postId,
				PostModerationAction.PUBLISH,
				PostStatus.APPROVED,
				PostStatus.PUBLISHED,
				PostStatus.PUBLISHED,
				reviewedById,
				"Admin Reviewer",
				OffsetDateTime.parse("2026-07-23T08:15:30Z"));
	}

	private static AdminPostModerationActionResponse unpublishAction(UUID postId, UUID reviewedById) {
		return new AdminPostModerationActionResponse(
				postId,
				PostModerationAction.UNPUBLISH,
				PostStatus.PUBLISHED,
				PostStatus.APPROVED,
				PostStatus.APPROVED,
				reviewedById,
				"Admin Reviewer",
				OffsetDateTime.parse("2026-07-23T08:15:30Z"));
	}
}

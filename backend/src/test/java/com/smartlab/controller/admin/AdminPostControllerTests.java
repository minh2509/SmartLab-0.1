package com.smartlab.controller.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.smartlab.dto.response.admin.AdminPostPageResponse;
import com.smartlab.dto.response.admin.AdminPostSummaryResponse;
import com.smartlab.enums.PostContentType;
import com.smartlab.enums.PostStatus;
import com.smartlab.enums.PostVisibility;
import com.smartlab.exception.ApiExceptionHandler;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminPostService;

class AdminPostControllerTests {

	private final AdminPostService adminPostService = mock(AdminPostService.class);
	private final AuthenticatedActorResolver actorResolver = mock(AuthenticatedActorResolver.class);
	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new AdminPostController(adminPostService, actorResolver))
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

	private static LocalValidatorFactoryBean validator() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		return validator;
	}

	private static AdminPostSummaryResponse summary(UUID postId) {
		return new AdminPostSummaryResponse(
				postId,
				"Post Title",
				"post-title",
				"Short summary",
				PostContentType.NEWS,
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
}

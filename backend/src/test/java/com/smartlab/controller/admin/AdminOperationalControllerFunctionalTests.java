package com.smartlab.controller.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smartlab.enums.FileVisibility;
import com.smartlab.enums.NotificationTargetType;
import com.smartlab.enums.ProjectJoinRequestStatus;
import com.smartlab.exception.AdminFeatureDisabledException;
import com.smartlab.exception.ApiExceptionHandler;
import com.smartlab.exception.InvalidAdminWorkflowStateException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.service.admin.AdminDashboardService;
import com.smartlab.service.admin.AdminJoinRequestService;
import com.smartlab.service.admin.AdminNotificationService;

class AdminOperationalControllerFunctionalTests {

	private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID LAB_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
	private static final UUID PROJECT_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
	private static final UUID REQUEST_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
	private static final UUID REQUESTER_ID = UUID.fromString("50000000-0000-0000-0000-000000000005");
	private static final UUID NOTIFICATION_ID = UUID.fromString("60000000-0000-0000-0000-000000000006");
	private static final UUID FILE_ID = UUID.fromString("70000000-0000-0000-0000-000000000007");
	private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-07-23T01:02:03Z");
	private static final OffsetDateTime FILTER_FROM = OffsetDateTime.parse("2026-07-01T00:00:00Z");
	private static final OffsetDateTime FILTER_TO = OffsetDateTime.parse("2026-07-31T23:59:59Z");

	private AdminDashboardService dashboardService;
	private AdminJoinRequestService joinRequestService;
	private AdminNotificationService notificationService;
	private LocalValidatorFactoryBean validator;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		dashboardService = mock(AdminDashboardService.class);
		joinRequestService = mock(AdminJoinRequestService.class);
		notificationService = mock(AdminNotificationService.class);
		validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		Jwt jwt = Jwt.withTokenValue("admin-test-token")
				.header("alg", "none")
				.subject(ADMIN_ID.toString())
				.claim("lab_id", LAB_ID.toString())
				.issuedAt(Instant.parse("2026-07-23T00:00:00Z"))
				.expiresAt(Instant.parse("2026-07-23T01:00:00Z"))
				.build();

		mockMvc = MockMvcBuilders.standaloneSetup(
					new AdminDashboardController(dashboardService),
					new AdminJoinRequestController(joinRequestService),
					new AdminNotificationController(notificationService))
				.setControllerAdvice(new ApiExceptionHandler())
				.setCustomArgumentResolvers(new FixedJwtArgumentResolver(jwt))
				.setValidator(validator)
				.build();
	}

	@AfterEach
	void tearDown() {
		validator.close();
	}

	@Test
	void dashboardUsesJwtLabClaimAndReturnsSummaryJson() throws Exception {
		UUID activityId = UUID.fromString("80000000-0000-0000-0000-000000000008");
		when(dashboardService.getDashboardSummary(LAB_ID, 7)).thenReturn(
				new AdminDashboardService.DashboardSummary(
						12,
						3,
						5,
						4,
						9,
						List.of(new AdminDashboardService.RecentActivity(
								activityId,
								"CREATE_NOTIFICATION",
								"NOTIFICATION",
								NOTIFICATION_ID,
								ADMIN_ID,
								"Admin User",
								CREATED_AT))));

		mockMvc.perform(get("/api/admin/dashboard").param("recentLimit", "7"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.users").value(12))
				.andExpect(jsonPath("$.projects").value(3))
				.andExpect(jsonPath("$.posts").value(5))
				.andExpect(jsonPath("$.joinRequests").value(4))
				.andExpect(jsonPath("$.tasks").value(9))
				.andExpect(jsonPath("$.recentActivities[0].id").value(activityId.toString()))
				.andExpect(jsonPath("$.recentActivities[0].actorId").value(ADMIN_ID.toString()));

		verify(dashboardService).getDashboardSummary(LAB_ID, 7);
	}

	@Test
	void joinRequestListPassesJwtLabAndFiltersAndReturnsStablePageShape() throws Exception {
		AdminJoinRequestService.JoinRequestSummary summary = joinRequestSummary(ProjectJoinRequestStatus.PENDING);
		when(joinRequestService.getJoinRequests(
				eq(LAB_ID),
				any(AdminJoinRequestService.JoinRequestFilter.class),
				eq(2),
				eq(5)))
				.thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(2, 5), 11));

		mockMvc.perform(get("/api/admin/project-join-requests")
					.param("projectId", PROJECT_ID.toString())
					.param("status", "PENDING")
					.param("requesterId", REQUESTER_ID.toString())
					.param("createdFrom", FILTER_FROM.toString())
					.param("createdTo", FILTER_TO.toString())
					.param("page", "2")
					.param("size", "5"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.content[0].id").value(REQUEST_ID.toString()))
				.andExpect(jsonPath("$.content[0].project.id").value(PROJECT_ID.toString()))
				.andExpect(jsonPath("$.content[0].requester.id").value(REQUESTER_ID.toString()))
				.andExpect(jsonPath("$.content[0].cvFile.originalName").value("research-cv.pdf"))
				.andExpect(jsonPath("$.content[0].status").value("PENDING"))
				.andExpect(jsonPath("$.page").value(2))
				.andExpect(jsonPath("$.size").value(5))
				.andExpect(jsonPath("$.totalElements").value(11))
				.andExpect(jsonPath("$.totalPages").value(3));

		ArgumentCaptor<AdminJoinRequestService.JoinRequestFilter> filterCaptor =
				ArgumentCaptor.forClass(AdminJoinRequestService.JoinRequestFilter.class);
		verify(joinRequestService).getJoinRequests(eq(LAB_ID), filterCaptor.capture(), eq(2), eq(5));
		AdminJoinRequestService.JoinRequestFilter filter = filterCaptor.getValue();
		assertEquals(PROJECT_ID, filter.projectId());
		assertEquals(ProjectJoinRequestStatus.PENDING, filter.status());
		assertEquals(REQUESTER_ID, filter.requesterId());
		assertEquals(FILTER_FROM, filter.createdFrom());
		assertEquals(FILTER_TO, filter.createdTo());
	}

	@Test
	void joinRequestReviewActionsUseJwtLabAndAdminClaims() throws Exception {
		when(joinRequestService.adminApprove(LAB_ID, REQUEST_ID, ADMIN_ID))
				.thenReturn(joinRequestSummary(ProjectJoinRequestStatus.APPROVED));
		when(joinRequestService.adminReject(LAB_ID, REQUEST_ID, "Insufficient project experience", ADMIN_ID))
				.thenReturn(joinRequestSummary(ProjectJoinRequestStatus.REJECTED));

		mockMvc.perform(patch("/api/admin/project-join-requests/{requestId}/approve", REQUEST_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(REQUEST_ID.toString()))
				.andExpect(jsonPath("$.status").value("APPROVED"));

		mockMvc.perform(patch("/api/admin/project-join-requests/{requestId}/reject", REQUEST_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"reason":"Insufficient project experience"}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(REQUEST_ID.toString()))
				.andExpect(jsonPath("$.status").value("REJECTED"));

		verify(joinRequestService).adminApprove(LAB_ID, REQUEST_ID, ADMIN_ID);
		verify(joinRequestService).adminReject(
				LAB_ID, REQUEST_ID, "Insufficient project experience", ADMIN_ID);
	}

	@Test
	void blankJoinRejectionReasonReturnsValidationErrorWithoutCallingService() throws Exception {
		mockMvc.perform(patch("/api/admin/project-join-requests/{requestId}/reject", REQUEST_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"reason":"   "}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Request validation failed."))
				.andExpect(jsonPath("$.path")
						.value("/api/admin/project-join-requests/" + REQUEST_ID + "/reject"));

		verify(joinRequestService, never()).adminReject(any(), any(), any(), any());
	}

	@Test
	void joinRequestFailuresUseSharedApiExceptionMappings() throws Exception {
		when(joinRequestService.getJoinRequestDetail(LAB_ID, REQUEST_ID))
				.thenThrow(new ResourceNotFoundException("Project join request was not found."));
		when(joinRequestService.adminApprove(LAB_ID, REQUEST_ID, ADMIN_ID))
				.thenThrow(new AdminFeatureDisabledException("Admin join-request override is disabled."));
		when(joinRequestService.adminReject(LAB_ID, REQUEST_ID, "Already handled", ADMIN_ID))
				.thenThrow(new InvalidAdminWorkflowStateException("Only pending join requests can be reviewed."));

		mockMvc.perform(get("/api/admin/project-join-requests/{requestId}", REQUEST_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Project join request was not found."));

		mockMvc.perform(patch("/api/admin/project-join-requests/{requestId}/approve", REQUEST_ID))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.message").value("Admin join-request override is disabled."));

		mockMvc.perform(patch("/api/admin/project-join-requests/{requestId}/reject", REQUEST_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"reason":"Already handled"}
							"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("Only pending join requests can be reviewed."));
	}

	@Test
	void notificationListPassesJwtLabAndFiltersAndReturnsStablePageShape() throws Exception {
		AdminNotificationService.NotificationSummary summary = notificationSummary();
		when(notificationService.getNotifications(
				eq(LAB_ID),
				any(AdminNotificationService.NotificationFilter.class),
				eq(1),
				eq(10)))
				.thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(1, 10), 12));

		mockMvc.perform(get("/api/admin/notifications")
					.param("notificationType", "SYSTEM_NOTICE")
					.param("creatorId", ADMIN_ID.toString())
					.param("relatedType", "PROJECT")
					.param("createdFrom", FILTER_FROM.toString())
					.param("createdTo", FILTER_TO.toString())
					.param("page", "1")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.content[0].id").value(NOTIFICATION_ID.toString()))
				.andExpect(jsonPath("$.content[0].createdBy.id").value(ADMIN_ID.toString()))
				.andExpect(jsonPath("$.content[0].recipientCount").value(3))
				.andExpect(jsonPath("$.content[0].readCount").value(1))
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.totalElements").value(11))
				.andExpect(jsonPath("$.totalPages").value(2));

		ArgumentCaptor<AdminNotificationService.NotificationFilter> filterCaptor =
				ArgumentCaptor.forClass(AdminNotificationService.NotificationFilter.class);
		verify(notificationService).getNotifications(eq(LAB_ID), filterCaptor.capture(), eq(1), eq(10));
		AdminNotificationService.NotificationFilter filter = filterCaptor.getValue();
		assertEquals("SYSTEM_NOTICE", filter.notificationType());
		assertEquals(ADMIN_ID, filter.creatorId());
		assertEquals("PROJECT", filter.relatedType());
		assertEquals(FILTER_FROM, filter.createdFrom());
		assertEquals(FILTER_TO, filter.createdTo());
	}

	@Test
	void notificationDetailReturnsRecipientSummary() throws Exception {
		when(notificationService.getNotificationDetail(LAB_ID, NOTIFICATION_ID))
				.thenReturn(notificationDetail());

		mockMvc.perform(get("/api/admin/notifications/{notificationId}", NOTIFICATION_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(NOTIFICATION_ID.toString()))
				.andExpect(jsonPath("$.recipientCount").value(3))
				.andExpect(jsonPath("$.readCount").value(1))
				.andExpect(jsonPath("$.recipients[0].userId").value(REQUESTER_ID.toString()))
				.andExpect(jsonPath("$.recipients[0].fullName").value("Research Member"));

		verify(notificationService).getNotificationDetail(LAB_ID, NOTIFICATION_ID);
	}

	@Test
	void createNotificationUsesJwtClaimsAndReturnsCreatedLocationAndJson() throws Exception {
		when(notificationService.createNotification(
				eq(LAB_ID),
				eq(ADMIN_ID),
				any(AdminNotificationService.CreateNotificationCommand.class)))
				.thenReturn(notificationDetail());

		mockMvc.perform(post("/api/admin/notifications")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "title":"System maintenance",
							  "message":"The lab system will restart tonight.",
							  "notificationType":"SYSTEM_NOTICE",
							  "targetType":"USER",
							  "userIds":["%s"],
							  "relatedType":"PROJECT",
							  "relatedId":"%s",
							  "linkUrl":"/app/projects/smart-lab"
							}
							""".formatted(REQUESTER_ID, PROJECT_ID)))
				.andExpect(status().isCreated())
				.andExpect(header().string(
						HttpHeaders.LOCATION,
						"/api/admin/notifications/" + NOTIFICATION_ID))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(NOTIFICATION_ID.toString()))
				.andExpect(jsonPath("$.title").value("System maintenance"))
				.andExpect(jsonPath("$.recipients[0].userId").value(REQUESTER_ID.toString()));

		ArgumentCaptor<AdminNotificationService.CreateNotificationCommand> commandCaptor =
				ArgumentCaptor.forClass(AdminNotificationService.CreateNotificationCommand.class);
		verify(notificationService).createNotification(eq(LAB_ID), eq(ADMIN_ID), commandCaptor.capture());
		AdminNotificationService.CreateNotificationCommand command = commandCaptor.getValue();
		assertEquals("System maintenance", command.title());
		assertEquals(NotificationTargetType.USER, command.targetType());
		assertEquals(List.of(REQUESTER_ID), command.userIds());
		assertEquals("PROJECT", command.relatedType());
		assertEquals(PROJECT_ID, command.relatedId());
		assertEquals("/app/projects/smart-lab", command.linkUrl());
	}

	@Test
	void invalidNotificationRequestReturnsValidationErrorWithoutCallingService() throws Exception {
		mockMvc.perform(post("/api/admin/notifications")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "title":"   ",
							  "notificationType":"",
							  "targetType":null
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Request validation failed."))
				.andExpect(jsonPath("$.path").value("/api/admin/notifications"));

		verify(notificationService, never()).createNotification(any(), any(), any());
	}

	@Test
	void hideNotificationUsesJwtClaimsAndReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/admin/notifications/{notificationId}", NOTIFICATION_ID))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		verify(notificationService).hideNotification(LAB_ID, NOTIFICATION_ID, ADMIN_ID);
	}

	private static AdminJoinRequestService.JoinRequestSummary joinRequestSummary(
			ProjectJoinRequestStatus status) {
		return new AdminJoinRequestService.JoinRequestSummary(
				REQUEST_ID,
				new AdminJoinRequestService.ProjectSummary(
						PROJECT_ID, "SL-001", "Smart Lab", "smart-lab"),
				new AdminJoinRequestService.UserSummary(
						REQUESTER_ID, "Research Member", "member@smart.lab"),
				"Backend researcher",
				"I want to contribute to the project.",
				"Java, PostgreSQL",
				"Two years of backend development",
				"Research-focused backend engineer",
				new AdminJoinRequestService.CvFileSummary(
						FILE_ID,
						"research-cv.pdf",
						"application/pdf",
						1024L,
						FileVisibility.PRIVATE),
				status,
				status == ProjectJoinRequestStatus.PENDING
						? null
						: new AdminJoinRequestService.UserSummary(
								ADMIN_ID, "Admin User", "admin@smart.lab"),
				status == ProjectJoinRequestStatus.PENDING ? null : CREATED_AT.plusHours(1),
				status == ProjectJoinRequestStatus.REJECTED ? "Insufficient project experience" : null,
				CREATED_AT,
				CREATED_AT.plusHours(1));
	}

	private static AdminNotificationService.NotificationSummary notificationSummary() {
		return new AdminNotificationService.NotificationSummary(
				NOTIFICATION_ID,
				"System maintenance",
				"The lab system will restart tonight.",
				"SYSTEM_NOTICE",
				"PROJECT",
				PROJECT_ID,
				"/app/projects/smart-lab",
				new AdminNotificationService.UserSummary(ADMIN_ID, "Admin User"),
				3,
				1,
				CREATED_AT);
	}

	private static AdminNotificationService.NotificationDetail notificationDetail() {
		return new AdminNotificationService.NotificationDetail(
				notificationSummary(),
				List.of(new AdminNotificationService.RecipientSummary(
						REQUESTER_ID,
						"Research Member",
						null,
						null,
						CREATED_AT)));
	}

	private static final class FixedJwtArgumentResolver implements HandlerMethodArgumentResolver {

		private final Jwt jwt;

		private FixedJwtArgumentResolver(Jwt jwt) {
			this.jwt = jwt;
		}

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return Jwt.class.equals(parameter.getParameterType())
					&& parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
		}

		@Override
		public Object resolveArgument(
				MethodParameter parameter,
				ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest,
				WebDataBinderFactory binderFactory) {
			assertNotNull(parameter.getParameterAnnotation(AuthenticationPrincipal.class));
			return jwt;
		}
	}
}

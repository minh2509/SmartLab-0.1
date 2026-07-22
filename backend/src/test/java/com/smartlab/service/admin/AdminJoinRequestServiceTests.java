package com.smartlab.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.smartlab.entity.Lab;
import com.smartlab.entity.Project;
import com.smartlab.entity.ProjectJoinRequest;
import com.smartlab.entity.ProjectMember;
import com.smartlab.entity.User;
import com.smartlab.enums.ProjectJoinRequestStatus;
import com.smartlab.exception.AdminFeatureDisabledException;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.InvalidAdminWorkflowStateException;
import com.smartlab.repository.ProjectJoinRequestRepository;
import com.smartlab.repository.ProjectMemberRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.admin.AdminJoinRequestReadRepository;
import com.smartlab.service.common.AuditLogService;
import com.smartlab.service.common.NotificationService;

class AdminJoinRequestServiceTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-23T01:00:00Z"), ZoneOffset.UTC);

	private final ProjectJoinRequestRepository joinRequestRepository = mock(ProjectJoinRequestRepository.class);
	private final ProjectMemberRepository projectMemberRepository = mock(ProjectMemberRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final AdminJoinRequestReadRepository readRepository = mock(AdminJoinRequestReadRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final NotificationService notificationService = mock(NotificationService.class);
	private final AdminJoinRequestService service = service(true);

	@Test
	void approvePendingRequestCreatesOneMembershipAndEmitsAuditAndNotification() {
		Fixture fixture = fixture(ProjectJoinRequestStatus.PENDING);
		when(joinRequestRepository.findByIdForUpdate(fixture.request().getId())).thenReturn(Optional.of(fixture.request()));
		when(userRepository.findById(fixture.admin().getId())).thenReturn(Optional.of(fixture.admin()));
		when(projectMemberRepository.existsByProjectAndUser(fixture.project(), fixture.requester())).thenReturn(false);
		when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(joinRequestRepository.save(fixture.request())).thenReturn(fixture.request());

		AdminJoinRequestService.JoinRequestSummary result = service.adminApprove(
				fixture.lab().getId(), fixture.request().getId(), fixture.admin().getId());

		assertEquals(ProjectJoinRequestStatus.APPROVED, result.status());
		assertEquals(fixture.admin().getId(), result.reviewedBy().id());
		assertEquals(OffsetDateTime.now(CLOCK), result.reviewedAt());
		ArgumentCaptor<ProjectMember> memberCaptor = ArgumentCaptor.forClass(ProjectMember.class);
		verify(projectMemberRepository).save(memberCaptor.capture());
		assertEquals(fixture.project(), memberCaptor.getValue().getProject());
		assertEquals(fixture.requester(), memberCaptor.getValue().getUser());
		verify(auditLogService).record(any(AuditLogService.AuditCommand.class));
		verify(notificationService).sendToUsers(any(NotificationService.NotificationCommand.class),
				org.mockito.ArgumentMatchers.eq(List.of(fixture.requester().getId())));
	}

	@Test
	void approveRejectsNonPendingOrExistingMembership() {
		Fixture nonPending = fixture(ProjectJoinRequestStatus.APPROVED);
		when(joinRequestRepository.findByIdForUpdate(nonPending.request().getId())).thenReturn(Optional.of(nonPending.request()));
		when(userRepository.findById(nonPending.admin().getId())).thenReturn(Optional.of(nonPending.admin()));
		assertThrows(
				InvalidAdminWorkflowStateException.class,
				() -> service.adminApprove(
						nonPending.lab().getId(), nonPending.request().getId(), nonPending.admin().getId()));

		Fixture existing = fixture(ProjectJoinRequestStatus.PENDING);
		when(joinRequestRepository.findByIdForUpdate(existing.request().getId())).thenReturn(Optional.of(existing.request()));
		when(userRepository.findById(existing.admin().getId())).thenReturn(Optional.of(existing.admin()));
		when(projectMemberRepository.existsByProjectAndUser(existing.project(), existing.requester())).thenReturn(true);
		assertThrows(
				InvalidAdminWorkflowStateException.class,
				() -> service.adminApprove(
						existing.lab().getId(), existing.request().getId(), existing.admin().getId()));
		verify(projectMemberRepository, never()).delete(any());
	}

	@Test
	void rejectRequiresReasonAndRecordsReviewerMetadata() {
		Fixture fixture = fixture(ProjectJoinRequestStatus.PENDING);
		when(joinRequestRepository.findByIdForUpdate(fixture.request().getId())).thenReturn(Optional.of(fixture.request()));
		when(userRepository.findById(fixture.admin().getId())).thenReturn(Optional.of(fixture.admin()));

		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.adminReject(
						fixture.lab().getId(), fixture.request().getId(), " ", fixture.admin().getId()));
		AdminJoinRequestService.JoinRequestSummary result = service.adminReject(
				fixture.lab().getId(), fixture.request().getId(), " Not enough capacity ", fixture.admin().getId());

		assertEquals(ProjectJoinRequestStatus.REJECTED, result.status());
		assertEquals("Not enough capacity", result.rejectionReason());
		assertNotNull(result.reviewedAt());
		verify(auditLogService).record(any(AuditLogService.AuditCommand.class));
	}

	@Test
	void overrideIsDeniedWhenFeatureFlagIsOff() {
		Fixture fixture = fixture(ProjectJoinRequestStatus.PENDING);
		AdminJoinRequestService disabled = service(false);

		assertThrows(
				AdminFeatureDisabledException.class,
				() -> disabled.adminApprove(
						fixture.lab().getId(), fixture.request().getId(), fixture.admin().getId()));
		verify(joinRequestRepository, never()).findByIdForUpdate(any());
	}

	@Test
	void listValidationRejectsInvalidPaginationAndDateRange() {
		UUID labId = UUID.randomUUID();
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.getJoinRequests(labId, AdminJoinRequestService.JoinRequestFilter.empty(), -1, 20));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.getJoinRequests(labId, AdminJoinRequestService.JoinRequestFilter.empty(), 0, 101));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.getJoinRequests(
						labId,
						new AdminJoinRequestService.JoinRequestFilter(
								null,
								null,
								null,
								OffsetDateTime.parse("2026-07-24T00:00:00Z"),
								OffsetDateTime.parse("2026-07-23T00:00:00Z")),
						0,
						20));
	}

	private AdminJoinRequestService service(boolean enabled) {
		return new AdminJoinRequestService(
				joinRequestRepository,
				projectMemberRepository,
				userRepository,
				readRepository,
				auditLogService,
				notificationService,
				CLOCK,
				enabled);
	}

	private static Fixture fixture(ProjectJoinRequestStatus status) {
		Lab lab = new Lab();
		lab.setId(UUID.randomUUID());
		lab.setCode("LAB");
		Project project = new Project();
		project.setId(UUID.randomUUID());
		project.setLab(lab);
		project.setCode("PRJ");
		project.setName("Research Project");
		project.setSlug("research-project");
		User requester = user(UUID.randomUUID(), lab, "Member User", "member@example.edu");
		User admin = user(UUID.randomUUID(), lab, "Admin User", "admin@example.edu");
		ProjectJoinRequest request = new ProjectJoinRequest();
		request.setId(UUID.randomUUID());
		request.setProject(project);
		request.setRequester(requester);
		request.setStatus(status);
		return new Fixture(lab, project, requester, admin, request);
	}

	private static User user(UUID id, Lab lab, String fullName, String email) {
		User user = new User();
		user.setId(id);
		user.setLab(lab);
		user.setFullName(fullName);
		user.setEmail(email);
		return user;
	}

	private record Fixture(
			Lab lab,
			Project project,
			User requester,
			User admin,
			ProjectJoinRequest request) {
	}
}

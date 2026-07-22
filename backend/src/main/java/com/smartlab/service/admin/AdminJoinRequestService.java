package com.smartlab.service.admin;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.File;
import com.smartlab.entity.Project;
import com.smartlab.entity.ProjectJoinRequest;
import com.smartlab.entity.ProjectMember;
import com.smartlab.entity.User;
import com.smartlab.enums.FileVisibility;
import com.smartlab.enums.ProjectJoinRequestStatus;
import com.smartlab.enums.ProjectMemberRole;
import com.smartlab.enums.ProjectMemberStatus;
import com.smartlab.exception.AdminFeatureDisabledException;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.InvalidAdminWorkflowStateException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.ProjectJoinRequestRepository;
import com.smartlab.repository.ProjectMemberRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.admin.AdminJoinRequestReadRepository;
import com.smartlab.service.common.AuditLogService;
import com.smartlab.service.common.NotificationService;

@Service
@Profile("!nodb")
public class AdminJoinRequestService {

	private static final int MAX_PAGE_SIZE = 100;
	private static final String ENTITY_TYPE = "PROJECT_JOIN_REQUEST";

	private final ProjectJoinRequestRepository joinRequestRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final UserRepository userRepository;
	private final AdminJoinRequestReadRepository joinRequestReadRepository;
	private final AuditLogService auditLogService;
	private final NotificationService notificationService;
	private final Clock clock;
	private final boolean overrideEnabled;

	public AdminJoinRequestService(
			ProjectJoinRequestRepository joinRequestRepository,
			ProjectMemberRepository projectMemberRepository,
			UserRepository userRepository,
			AdminJoinRequestReadRepository joinRequestReadRepository,
			AuditLogService auditLogService,
			NotificationService notificationService,
			Clock clock,
			@Value("${smartlab.admin.join-request-override-enabled:false}") boolean overrideEnabled) {
		this.joinRequestRepository = joinRequestRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.userRepository = userRepository;
		this.joinRequestReadRepository = joinRequestReadRepository;
		this.auditLogService = auditLogService;
		this.notificationService = notificationService;
		this.clock = clock;
		this.overrideEnabled = overrideEnabled;
	}

	@Transactional(readOnly = true)
	public Page<JoinRequestSummary> getJoinRequests(
			UUID labId,
			JoinRequestFilter filter,
			int page,
			int size) {
		validateLabAndPagination(labId, page, size);
		JoinRequestFilter safeFilter = filter == null ? JoinRequestFilter.empty() : filter;
		validateDateRange(safeFilter.createdFrom(), safeFilter.createdTo());
		return joinRequestReadRepository.findPage(
				labId,
				new AdminJoinRequestReadRepository.JoinRequestCriteria(
						safeFilter.projectId(),
						safeFilter.status(),
						safeFilter.requesterId(),
						safeFilter.createdFrom(),
						safeFilter.createdTo()),
				page,
				size)
				.map(AdminJoinRequestService::toSummary);
	}

	@Transactional(readOnly = true)
	public JoinRequestSummary getJoinRequestDetail(UUID labId, UUID requestId) {
		return toSummary(findScopedRequest(labId, requestId));
	}

	@Transactional
	public JoinRequestSummary adminApprove(UUID labId, UUID requestId, UUID adminId) {
		requireOverrideEnabled();
		ProjectJoinRequest request = findScopedRequestForUpdate(labId, requestId);
		User admin = findScopedAdmin(labId, adminId);
		requirePending(request);
		if (projectMemberRepository.existsByProjectAndUser(request.getProject(), request.getRequester())) {
			throw new InvalidAdminWorkflowStateException("Requester already has a project membership record.");
		}

		ProjectMember membership = new ProjectMember();
		membership.setProject(request.getProject());
		membership.setUser(request.getRequester());
		membership.setProjectRole(ProjectMemberRole.PROJECT_MEMBER);
		membership.setMemberStatus(ProjectMemberStatus.ACTIVE);
		membership.setAddedBy(admin);
		try {
			projectMemberRepository.save(membership);
		} catch (DataIntegrityViolationException exception) {
			throw new InvalidAdminWorkflowStateException("Requester already has a project membership record.");
		}

		ProjectJoinRequestStatus previousStatus = request.getStatus();
		request.setStatus(ProjectJoinRequestStatus.APPROVED);
		request.setReviewedBy(admin);
		request.setReviewedAt(OffsetDateTime.now(clock));
		request.setRejectionReason(null);
		joinRequestRepository.save(request);
		recordDecisionAudit(adminId, request, "ADMIN_APPROVE_JOIN_REQUEST", previousStatus, null);
		sendDecisionNotification(request, adminId, true, null);
		return toSummary(request);
	}

	@Transactional
	public JoinRequestSummary adminReject(UUID labId, UUID requestId, String reason, UUID adminId) {
		requireOverrideEnabled();
		String normalizedReason = requireReason(reason);
		ProjectJoinRequest request = findScopedRequestForUpdate(labId, requestId);
		User admin = findScopedAdmin(labId, adminId);
		requirePending(request);

		ProjectJoinRequestStatus previousStatus = request.getStatus();
		request.setStatus(ProjectJoinRequestStatus.REJECTED);
		request.setReviewedBy(admin);
		request.setReviewedAt(OffsetDateTime.now(clock));
		request.setRejectionReason(normalizedReason);
		joinRequestRepository.save(request);
		recordDecisionAudit(adminId, request, "ADMIN_REJECT_JOIN_REQUEST", previousStatus, normalizedReason);
		sendDecisionNotification(request, adminId, false, normalizedReason);
		return toSummary(request);
	}

	private void recordDecisionAudit(
			UUID adminId,
			ProjectJoinRequest request,
			String action,
			ProjectJoinRequestStatus oldStatus,
			String reason) {
		Map<String, Object> newValue = new LinkedHashMap<>();
		newValue.put("status", request.getStatus().name());
		newValue.put("reviewedBy", adminId);
		newValue.put("rejectionReason", reason);
		auditLogService.record(new AuditLogService.AuditCommand(
				adminId,
				action,
				ENTITY_TYPE,
				request.getId(),
				Map.of("status", oldStatus.name()),
				newValue));
	}

	private void sendDecisionNotification(
			ProjectJoinRequest request,
			UUID adminId,
			boolean approved,
			String rejectionReason) {
		Project project = request.getProject();
		String title = approved ? "Project join request approved" : "Project join request rejected";
		String message = approved
				? "Your request to join %s has been approved.".formatted(project.getName())
				: "Your request to join %s was rejected: %s".formatted(project.getName(), rejectionReason);
		notificationService.sendToUsers(
				new NotificationService.NotificationCommand(
						project.getLab().getId(),
						adminId,
						title,
						message,
						approved ? "JOIN_REQUEST_APPROVED" : "JOIN_REQUEST_REJECTED",
						ENTITY_TYPE,
						request.getId(),
						"/app/projects/" + project.getSlug()),
				List.of(request.getRequester().getId()));
	}

	private ProjectJoinRequest findScopedRequest(UUID labId, UUID requestId) {
		if (labId == null || requestId == null) {
			throw new InvalidAdminServiceInputException("Lab ID and join request ID must not be null.");
		}
		ProjectJoinRequest request = joinRequestRepository.findById(requestId)
				.orElseThrow(() -> new ResourceNotFoundException("Project join request was not found."));
		if (!labId.equals(request.getProject().getLab().getId()) || request.getProject().getDeletedAt() != null) {
			throw new ResourceNotFoundException("Project join request was not found.");
		}
		return request;
	}

	private ProjectJoinRequest findScopedRequestForUpdate(UUID labId, UUID requestId) {
		if (labId == null || requestId == null) {
			throw new InvalidAdminServiceInputException("Lab ID and join request ID must not be null.");
		}
		ProjectJoinRequest request = joinRequestRepository.findByIdForUpdate(requestId)
				.orElseThrow(() -> new ResourceNotFoundException("Project join request was not found."));
		if (!labId.equals(request.getProject().getLab().getId()) || request.getProject().getDeletedAt() != null) {
			throw new ResourceNotFoundException("Project join request was not found.");
		}
		return request;
	}

	private User findScopedAdmin(UUID labId, UUID adminId) {
		if (adminId == null) {
			throw new InvalidAdminServiceInputException("Admin user ID must not be null.");
		}
		User admin = userRepository.findById(adminId)
				.orElseThrow(() -> new ResourceNotFoundException("Admin user was not found."));
		if (admin.getLab() == null || !labId.equals(admin.getLab().getId()) || admin.getDeletedAt() != null) {
			throw new ResourceNotFoundException("Admin user was not found.");
		}
		return admin;
	}

	private void requireOverrideEnabled() {
		if (!overrideEnabled) {
			throw new AdminFeatureDisabledException("Admin join-request override is disabled.");
		}
	}

	private static void requirePending(ProjectJoinRequest request) {
		if (request.getStatus() != ProjectJoinRequestStatus.PENDING) {
			throw new InvalidAdminWorkflowStateException("Only pending join requests can be reviewed.");
		}
	}

	private static String requireReason(String reason) {
		if (reason == null || reason.trim().isBlank()) {
			throw new InvalidAdminServiceInputException("Rejection reason must not be blank.");
		}
		String normalized = reason.trim();
		if (normalized.length() > 2000) {
			throw new InvalidAdminServiceInputException("Rejection reason must not exceed 2000 characters.");
		}
		return normalized;
	}

	private static void validateLabAndPagination(UUID labId, int page, int size) {
		if (labId == null) {
			throw new InvalidAdminServiceInputException("Lab ID must not be null.");
		}
		if (page < 0) {
			throw new InvalidAdminServiceInputException("Page index must not be negative.");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new InvalidAdminServiceInputException("Page size must be between 1 and 100.");
		}
	}

	private static void validateDateRange(OffsetDateTime from, OffsetDateTime to) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new InvalidAdminServiceInputException("Created-from must not be after created-to.");
		}
	}

	private static JoinRequestSummary toSummary(ProjectJoinRequest request) {
		return new JoinRequestSummary(
				request.getId(),
				toProject(request.getProject()),
				toUser(request.getRequester()),
				request.getDesiredPosition(),
				request.getReason(),
				request.getSkills(),
				request.getExperience(),
				request.getIntroduction(),
				toCvFile(request.getCvFile()),
				request.getStatus(),
				toUser(request.getReviewedBy()),
				request.getReviewedAt(),
				request.getRejectionReason(),
				request.getCreatedAt(),
				request.getUpdatedAt());
	}

	private static ProjectSummary toProject(Project project) {
		return new ProjectSummary(project.getId(), project.getCode(), project.getName(), project.getSlug());
	}

	private static UserSummary toUser(User user) {
		return user == null ? null : new UserSummary(user.getId(), user.getFullName(), user.getEmail());
	}

	private static CvFileSummary toCvFile(File file) {
		return file == null
				? null
				: new CvFileSummary(
						file.getId(),
						file.getOriginalName(),
						file.getMimeType(),
						file.getFileSize(),
						file.getVisibility());
	}

	public record JoinRequestFilter(
			UUID projectId,
			ProjectJoinRequestStatus status,
			UUID requesterId,
			OffsetDateTime createdFrom,
			OffsetDateTime createdTo) {

		public static JoinRequestFilter empty() {
			return new JoinRequestFilter(null, null, null, null, null);
		}
	}

	public record JoinRequestSummary(
			UUID id,
			ProjectSummary project,
			UserSummary requester,
			String desiredPosition,
			String reason,
			String skills,
			String experience,
			String introduction,
			CvFileSummary cvFile,
			ProjectJoinRequestStatus status,
			UserSummary reviewedBy,
			OffsetDateTime reviewedAt,
			String rejectionReason,
			OffsetDateTime createdAt,
			OffsetDateTime updatedAt) {
	}

	public record ProjectSummary(UUID id, String code, String name, String slug) {
	}

	public record UserSummary(UUID id, String fullName, String email) {
	}

	public record CvFileSummary(
			UUID id,
			String originalName,
			String mimeType,
			Long fileSize,
			FileVisibility visibility) {
	}
}

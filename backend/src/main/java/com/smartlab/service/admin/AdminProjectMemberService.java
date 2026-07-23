package com.smartlab.service.admin;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.Project;
import com.smartlab.entity.ProjectMember;
import com.smartlab.entity.User;
import com.smartlab.enums.ProjectMemberRole;
import com.smartlab.enums.ProjectMemberStatus;
import com.smartlab.enums.TaskAssigneeStatus;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.exception.ConflictingAdminOperationException;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.ProjectMemberRepository;
import com.smartlab.repository.ProjectRepository;
import com.smartlab.repository.TaskAssigneeRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.service.common.BusinessValidationService;

@Service
@Profile("!nodb")
public class AdminProjectMemberService {

	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final UserRepository userRepository;
	private final TaskAssigneeRepository taskAssigneeRepository;
	private final AdminRolePolicy rolePolicy;
	private final BusinessValidationService validationService;

	public AdminProjectMemberService(
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			UserRepository userRepository,
			TaskAssigneeRepository taskAssigneeRepository,
			AdminRolePolicy rolePolicy,
			BusinessValidationService validationService) {
		this.projectRepository = projectRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.userRepository = userRepository;
		this.taskAssigneeRepository = taskAssigneeRepository;
		this.rolePolicy = rolePolicy;
		this.validationService = validationService;
	}

	@Transactional(readOnly = true)
	public List<ProjectMemberSummary> getProjectMembers(
			UUID actorUserId,
			UUID projectId,
			ProjectMemberRole role,
			ProjectMemberStatus status) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		Project project = requireProject(actor, projectId);
		return projectMemberRepository.findByProject(project).stream()
				.filter(member -> role == null || member.getProjectRole() == role)
				.filter(member -> status == null || member.getMemberStatus() == status)
				.map(ProjectMemberSummary::from)
				.sorted(Comparator.comparing(ProjectMemberSummary::fullName, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(ProjectMemberSummary::userId))
				.toList();
	}

	@Transactional
	public ProjectMemberSummary addMember(
			UUID actorUserId,
			UUID projectId,
			UUID userId,
			ProjectMemberRole role) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		Project project = requireProject(actor, projectId);
		User user = requireActiveUser(actor, userId);
		ProjectMemberRole requiredRole = requireRole(role);
		assertLeaderEligible(user, requiredRole);

		OffsetDateTime now = OffsetDateTime.now();
		ProjectMember membership = projectMemberRepository.findByProjectAndUser(project, user)
				.map(existing -> reactivate(existing, requiredRole, actor.actor(), now))
				.orElseGet(() -> newMembership(project, user, requiredRole, actor.actor(), now));
		return ProjectMemberSummary.from(projectMemberRepository.save(membership));
	}

	@Transactional
	public ProjectMemberSummary updateProjectRole(
			UUID actorUserId,
			UUID projectId,
			UUID userId,
			ProjectMemberRole role) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		Project project = requireProject(actor, projectId);
		User user = requireUser(actor, userId);
		ProjectMember membership = requireActiveMembership(project, user);
		ProjectMemberRole requiredRole = requireRole(role);
		if (membership.getProjectRole() == requiredRole) {
			return ProjectMemberSummary.from(membership);
		}
		assertLeaderEligible(user, requiredRole);
		if (membership.getProjectRole() == ProjectMemberRole.PROJECT_LEADER) {
			assertNotLastLeader(project);
		}
		membership.setProjectRole(requiredRole);
		return ProjectMemberSummary.from(projectMemberRepository.save(membership));
	}

	@Transactional
	public ProjectMemberSummary removeMember(UUID actorUserId, UUID projectId, UUID userId) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		Project project = requireProject(actor, projectId);
		User user = requireUser(actor, userId);
		ProjectMember membership = requireActiveMembership(project, user);
		if (membership.getProjectRole() == ProjectMemberRole.PROJECT_LEADER) {
			assertNotLastLeader(project);
		}
		if (taskAssigneeRepository.existsByTaskProjectAndUserAndStatus(
				project,
				user,
				TaskAssigneeStatus.ASSIGNED)) {
			throw new ConflictingAdminOperationException(
					"Project members with active task assignments cannot be removed.");
		}
		membership.setMemberStatus(ProjectMemberStatus.REMOVED);
		membership.setLeftAt(OffsetDateTime.now());
		return ProjectMemberSummary.from(projectMemberRepository.save(membership));
	}

	private Project requireProject(AdminRolePolicy.ActorContext actor, UUID projectId) {
		validationService.requireId(projectId, "Project ID");
		return projectRepository.findByIdAndLabAndDeletedAtIsNull(projectId, actor.lab())
				.orElseThrow(() -> new ResourceNotFoundException("Project was not found."));
	}

	private User requireActiveUser(AdminRolePolicy.ActorContext actor, UUID userId) {
		User user = requireUser(actor, userId);
		if (user.getAccountStatus() != UserAccountStatus.ACTIVE) {
			throw new ConflictingAdminOperationException("Only ACTIVE users can join a project.");
		}
		return user;
	}

	private User requireUser(AdminRolePolicy.ActorContext actor, UUID userId) {
		validationService.requireId(userId, "User ID");
		return userRepository.findByIdAndLab(userId, actor.lab())
				.orElseThrow(() -> new ResourceNotFoundException("User was not found."));
	}

	private ProjectMember requireActiveMembership(Project project, User user) {
		ProjectMember membership = projectMemberRepository.findByProjectAndUser(project, user)
				.orElseThrow(() -> new ResourceNotFoundException("Project membership was not found."));
		if (membership.getMemberStatus() != ProjectMemberStatus.ACTIVE) {
			throw new ConflictingAdminOperationException("Project membership is not active.");
		}
		return membership;
	}

	private ProjectMember reactivate(
			ProjectMember membership,
			ProjectMemberRole role,
			User actor,
			OffsetDateTime now) {
		if (membership.getMemberStatus() == ProjectMemberStatus.ACTIVE) {
			throw new ConflictingAdminOperationException("User is already an active project member.");
		}
		membership.setProjectRole(role);
		membership.setMemberStatus(ProjectMemberStatus.ACTIVE);
		membership.setJoinedAt(now);
		membership.setLeftAt(null);
		membership.setAddedBy(actor);
		return membership;
	}

	private static ProjectMember newMembership(
			Project project,
			User user,
			ProjectMemberRole role,
			User actor,
			OffsetDateTime now) {
		ProjectMember membership = new ProjectMember();
		membership.setProject(project);
		membership.setUser(user);
		membership.setProjectRole(role);
		membership.setMemberStatus(ProjectMemberStatus.ACTIVE);
		membership.setJoinedAt(now);
		membership.setAddedBy(actor);
		return membership;
	}

	private void assertLeaderEligible(User user, ProjectMemberRole role) {
		if (role != ProjectMemberRole.PROJECT_LEADER) {
			return;
		}
		List<String> roles = rolePolicy.activeRoleCodes(user);
		if (!roles.contains(AdminRolePolicy.LEADER_ROLE_CODE)
				&& !roles.contains(AdminRolePolicy.ADMIN_ROLE_CODE)) {
			throw new ConflictingAdminOperationException(
					"Project leaders must have the LEADER or ADMIN system role.");
		}
	}

	private void assertNotLastLeader(Project project) {
		long leaderCount = projectMemberRepository.countByProjectAndProjectRoleAndMemberStatus(
				project,
				ProjectMemberRole.PROJECT_LEADER,
				ProjectMemberStatus.ACTIVE);
		if (leaderCount <= 1) {
			throw new ConflictingAdminOperationException("A project must retain at least one active leader.");
		}
	}

	private static ProjectMemberRole requireRole(ProjectMemberRole role) {
		if (role == null) {
			throw new InvalidAdminServiceInputException("Project member role is required.");
		}
		return role;
	}

	public record ProjectMemberSummary(
			UUID membershipId,
			UUID userId,
			String fullName,
			String email,
			ProjectMemberRole projectRole,
			ProjectMemberStatus memberStatus,
			OffsetDateTime joinedAt,
			OffsetDateTime leftAt) {

		static ProjectMemberSummary from(ProjectMember member) {
			User user = member.getUser();
			return new ProjectMemberSummary(
					member.getId(),
					user.getId(),
					user.getFullName(),
					user.getEmail(),
					member.getProjectRole(),
					member.getMemberStatus(),
					member.getJoinedAt(),
					member.getLeftAt());
		}
	}
}

package com.smartlab.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.smartlab.entity.Lab;
import com.smartlab.entity.Project;
import com.smartlab.entity.ProjectMember;
import com.smartlab.entity.User;
import com.smartlab.enums.ProjectMemberRole;
import com.smartlab.enums.ProjectMemberStatus;
import com.smartlab.enums.TaskAssigneeStatus;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.exception.ConflictingAdminOperationException;
import com.smartlab.repository.ProjectMemberRepository;
import com.smartlab.repository.ProjectRepository;
import com.smartlab.repository.TaskAssigneeRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.service.common.BusinessValidationService;

class AdminProjectMemberServiceTests {

	private final ProjectRepository projectRepository = mock(ProjectRepository.class);
	private final ProjectMemberRepository memberRepository = mock(ProjectMemberRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final TaskAssigneeRepository taskAssigneeRepository = mock(TaskAssigneeRepository.class);
	private final AdminRolePolicy rolePolicy = mock(AdminRolePolicy.class);
	private final AdminProjectMemberService service = new AdminProjectMemberService(
			projectRepository,
			memberRepository,
			userRepository,
			taskAssigneeRepository,
			rolePolicy,
			new BusinessValidationService());
	private final UUID actorId = UUID.randomUUID();
	private final Lab lab = lab();
	private final User actor = user(lab, UUID.randomUUID(), UserAccountStatus.ACTIVE);
	private final Project project = project(lab);

	@BeforeEach
	void setUp() {
		when(rolePolicy.requireAdminActor(actorId))
				.thenReturn(new AdminRolePolicy.ActorContext(actor, Set.of(AdminRolePolicy.ADMIN_ROLE_CODE)));
		when(projectRepository.findByIdAndLabAndDeletedAtIsNull(project.getId(), lab))
				.thenReturn(Optional.of(project));
		when(memberRepository.save(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void listsMembersWithRoleAndStatusFiltersWithoutSensitiveFields() {
		ProjectMember leader = membership(user(lab, UUID.randomUUID(), UserAccountStatus.ACTIVE),
				ProjectMemberRole.PROJECT_LEADER, ProjectMemberStatus.ACTIVE);
		ProjectMember removed = membership(user(lab, UUID.randomUUID(), UserAccountStatus.ACTIVE),
				ProjectMemberRole.PROJECT_MEMBER, ProjectMemberStatus.REMOVED);
		when(memberRepository.findByProject(project)).thenReturn(List.of(removed, leader));

		List<AdminProjectMemberService.ProjectMemberSummary> result = service.getProjectMembers(
				actorId, project.getId(), ProjectMemberRole.PROJECT_LEADER, ProjectMemberStatus.ACTIVE);

		assertEquals(1, result.size());
		assertEquals(leader.getUser().getId(), result.getFirst().userId());
		assertEquals(ProjectMemberRole.PROJECT_LEADER, result.getFirst().projectRole());
	}

	@Test
	void rejectsDuplicateActiveMembership() {
		User target = target();
		when(userRepository.findByIdAndLab(target.getId(), lab)).thenReturn(Optional.of(target));
		when(memberRepository.findByProjectAndUser(project, target))
				.thenReturn(Optional.of(membership(target, ProjectMemberRole.PROJECT_MEMBER, ProjectMemberStatus.ACTIVE)));

		assertThrows(ConflictingAdminOperationException.class,
				() -> service.addMember(actorId, project.getId(), target.getId(), ProjectMemberRole.PROJECT_MEMBER));
	}

	@Test
	void reactivatesRemovedMembershipAndClearsLeftAt() {
		User target = target();
		ProjectMember removed = membership(target, ProjectMemberRole.PROJECT_MEMBER, ProjectMemberStatus.REMOVED);
		removed.setLeftAt(OffsetDateTime.now().minusDays(1));
		when(userRepository.findByIdAndLab(target.getId(), lab)).thenReturn(Optional.of(target));
		when(memberRepository.findByProjectAndUser(project, target)).thenReturn(Optional.of(removed));

		AdminProjectMemberService.ProjectMemberSummary result = service.addMember(
				actorId, project.getId(), target.getId(), ProjectMemberRole.PROJECT_MEMBER);

		assertEquals(ProjectMemberStatus.ACTIVE, result.memberStatus());
		assertNull(result.leftAt());
		assertEquals(actor, removed.getAddedBy());
	}

	@Test
	void requiresLeaderSystemRoleWhenPromoting() {
		User target = target();
		ProjectMember member = membership(target, ProjectMemberRole.PROJECT_MEMBER, ProjectMemberStatus.ACTIVE);
		when(userRepository.findByIdAndLab(target.getId(), lab)).thenReturn(Optional.of(target));
		when(memberRepository.findByProjectAndUser(project, target)).thenReturn(Optional.of(member));
		when(rolePolicy.activeRoleCodes(target)).thenReturn(List.of(AdminRolePolicy.MEMBER_ROLE_CODE));

		assertThrows(ConflictingAdminOperationException.class,
				() -> service.updateProjectRole(actorId, project.getId(), target.getId(),
						ProjectMemberRole.PROJECT_LEADER));
	}

	@Test
	void sameRoleUpdateIsIdempotent() {
		User target = target();
		ProjectMember member = membership(target, ProjectMemberRole.PROJECT_MEMBER, ProjectMemberStatus.ACTIVE);
		when(userRepository.findByIdAndLab(target.getId(), lab)).thenReturn(Optional.of(target));
		when(memberRepository.findByProjectAndUser(project, target)).thenReturn(Optional.of(member));

		AdminProjectMemberService.ProjectMemberSummary result = service.updateProjectRole(
				actorId, project.getId(), target.getId(), ProjectMemberRole.PROJECT_MEMBER);

		assertEquals(ProjectMemberRole.PROJECT_MEMBER, result.projectRole());
	}

	@Test
	void refusesToDowngradeLastActiveLeader() {
		User target = target();
		ProjectMember leader = membership(target, ProjectMemberRole.PROJECT_LEADER, ProjectMemberStatus.ACTIVE);
		when(userRepository.findByIdAndLab(target.getId(), lab)).thenReturn(Optional.of(target));
		when(memberRepository.findByProjectAndUser(project, target)).thenReturn(Optional.of(leader));
		when(memberRepository.countByProjectAndProjectRoleAndMemberStatus(
				project, ProjectMemberRole.PROJECT_LEADER, ProjectMemberStatus.ACTIVE)).thenReturn(1L);

		assertThrows(ConflictingAdminOperationException.class,
				() -> service.updateProjectRole(actorId, project.getId(), target.getId(),
						ProjectMemberRole.PROJECT_MEMBER));
	}

	@Test
	void refusesToRemoveMemberWithActiveTaskAssignment() {
		User target = target();
		ProjectMember member = membership(target, ProjectMemberRole.PROJECT_MEMBER, ProjectMemberStatus.ACTIVE);
		when(userRepository.findByIdAndLab(target.getId(), lab)).thenReturn(Optional.of(target));
		when(memberRepository.findByProjectAndUser(project, target)).thenReturn(Optional.of(member));
		when(taskAssigneeRepository.existsByTaskProjectAndUserAndStatus(
				project, target, TaskAssigneeStatus.ASSIGNED)).thenReturn(true);

		assertThrows(ConflictingAdminOperationException.class,
				() -> service.removeMember(actorId, project.getId(), target.getId()));
	}

	@Test
	void removesMemberWithoutDeletingMembership() {
		User target = target();
		ProjectMember member = membership(target, ProjectMemberRole.PROJECT_MEMBER, ProjectMemberStatus.ACTIVE);
		when(userRepository.findByIdAndLab(target.getId(), lab)).thenReturn(Optional.of(target));
		when(memberRepository.findByProjectAndUser(project, target)).thenReturn(Optional.of(member));

		AdminProjectMemberService.ProjectMemberSummary result = service.removeMember(
				actorId, project.getId(), target.getId());

		assertEquals(ProjectMemberStatus.REMOVED, result.memberStatus());
		verify(memberRepository).save(member);
	}

	private User target() {
		return user(lab, UUID.randomUUID(), UserAccountStatus.ACTIVE);
	}

	private ProjectMember membership(User user, ProjectMemberRole role, ProjectMemberStatus status) {
		ProjectMember membership = new ProjectMember();
		membership.setId(UUID.randomUUID());
		membership.setProject(project);
		membership.setUser(user);
		membership.setProjectRole(role);
		membership.setMemberStatus(status);
		membership.setJoinedAt(OffsetDateTime.now().minusDays(2));
		return membership;
	}

	private static Lab lab() {
		Lab lab = new Lab();
		lab.setId(UUID.randomUUID());
		return lab;
	}

	private static Project project(Lab lab) {
		Project project = new Project();
		project.setId(UUID.randomUUID());
		project.setLab(lab);
		return project;
	}

	private static User user(Lab lab, UUID id, UserAccountStatus status) {
		User user = new User();
		user.setId(id);
		user.setLab(lab);
		user.setFullName("Test User " + id.toString().substring(0, 4));
		user.setEmail(id + "@smart.lab");
		user.setAccountStatus(status);
		return user;
	}
}

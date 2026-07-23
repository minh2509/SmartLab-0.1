package com.smartlab.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.smartlab.dto.response.admin.AdminMemberDetailResponse;
import com.smartlab.dto.response.admin.AdminMemberEvaluationResponse;
import com.smartlab.dto.response.admin.AdminMemberProjectResponse;
import com.smartlab.dto.response.admin.AdminMemberSummaryResponse;
import com.smartlab.entity.Lab;
import com.smartlab.entity.MemberEvaluation;
import com.smartlab.entity.MemberProfile;
import com.smartlab.entity.Project;
import com.smartlab.entity.ProjectMember;
import com.smartlab.entity.ResearchField;
import com.smartlab.entity.Role;
import com.smartlab.entity.User;
import com.smartlab.entity.UserRole;
import com.smartlab.enums.MemberProfileActivityStatus;
import com.smartlab.enums.ProjectMemberRole;
import com.smartlab.enums.ProjectMemberStatus;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.mapper.AdminMemberApiMapper;
import com.smartlab.mapper.AdminResearchFieldApiMapper;
import com.smartlab.repository.MemberEvaluationRepository;
import com.smartlab.repository.MemberProfileRepository;
import com.smartlab.repository.MemberResearchFieldRepository;
import com.smartlab.repository.ProjectMemberRepository;
import com.smartlab.repository.ResearchFieldRepository;
import com.smartlab.repository.RoleRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.UserRoleRepository;

class AdminMemberServiceTests {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final MemberProfileRepository memberProfileRepository = mock(MemberProfileRepository.class);
	private final MemberResearchFieldRepository memberResearchFieldRepository = mock(MemberResearchFieldRepository.class);
	private final ResearchFieldRepository researchFieldRepository = mock(ResearchFieldRepository.class);
	private final ProjectMemberRepository projectMemberRepository = mock(ProjectMemberRepository.class);
	private final MemberEvaluationRepository memberEvaluationRepository = mock(MemberEvaluationRepository.class);
	private final RoleRepository roleRepository = mock(RoleRepository.class);
	private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);

	private final AdminRolePolicy rolePolicy = new AdminRolePolicy(userRepository, roleRepository, userRoleRepository);
	private final AdminMemberService service = new AdminMemberService(
			userRepository,
			memberProfileRepository,
			memberResearchFieldRepository,
			researchFieldRepository,
			projectMemberRepository,
			memberEvaluationRepository,
			rolePolicy,
			new AdminMemberApiMapper(new AdminResearchFieldApiMapper()));

	private final UUID actorUserId = UUID.randomUUID();
	private final Lab lab = new Lab();

	@BeforeEach
	void setUp() {
		lab.setId(UUID.randomUUID());

		User actor = new User();
		actor.setId(actorUserId);
		actor.setLab(lab);
		actor.setAccountStatus(UserAccountStatus.ACTIVE);

		Role adminRole = new Role();
		adminRole.setCode("ADMIN");

		UserRole userRole = new UserRole();
		userRole.setUser(actor);
		userRole.setRole(adminRole);
		userRole.setStatus(UserRoleStatus.ACTIVE);

		when(userRepository.findById(actorUserId)).thenReturn(Optional.of(actor));
		when(userRoleRepository.findByUserAndStatus(actor, UserRoleStatus.ACTIVE)).thenReturn(List.of(userRole));
	}

	@Test
	void listMembers_Success() {
		User targetUser = new User();
		targetUser.setId(UUID.randomUUID());
		targetUser.setLab(lab);
		targetUser.setUsername("user1");
		targetUser.setFullName("User One");
		targetUser.setEmail("user1@lab.com");

		PageRequest pageable = PageRequest.of(0, 20);
		when(userRepository.findByLabAndDeletedAtIsNull(lab, pageable)).thenReturn(new PageImpl<>(List.of(targetUser)));

		Page<AdminMemberSummaryResponse> page = service.listMembers(
				new AdminMemberService.ListMembersQuery(actorUserId, 0, 20, null));

		assertNotNull(page);
		assertEquals(1, page.getTotalElements());
		assertEquals("user1", page.getContent().get(0).username());
	}

	@Test
	void getMemberDetail_Success() {
		UUID targetUserId = UUID.randomUUID();
		User targetUser = new User();
		targetUser.setId(targetUserId);
		targetUser.setLab(lab);
		targetUser.setUsername("target");
		targetUser.setFullName("Target User");
		targetUser.setEmail("target@lab.com");

		MemberProfile profile = new MemberProfile();
		profile.setStudentCode("ST123");
		profile.setActivityStatus(MemberProfileActivityStatus.ACTIVE);

		when(userRepository.findByIdAndLab(targetUserId, lab)).thenReturn(Optional.of(targetUser));
		when(memberProfileRepository.findByUser(targetUser)).thenReturn(Optional.of(profile));
		when(projectMemberRepository.findByUser(targetUser)).thenReturn(List.of());
		when(memberEvaluationRepository.findByMember(targetUser)).thenReturn(List.of());

		AdminMemberDetailResponse detail = service.getMemberDetail(actorUserId, targetUserId);

		assertNotNull(detail);
		assertEquals("target", detail.username());
		assertNotNull(detail.profile());
		assertEquals("ST123", detail.profile().studentCode());
	}

	@Test
	void getMemberDetail_NotFound_ThrowsException() {
		UUID invalidUserId = UUID.randomUUID();
		when(userRepository.findByIdAndLab(invalidUserId, lab)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.getMemberDetail(actorUserId, invalidUserId));
	}

	@Test
	void updateMemberActivityStatus_Success() {
		UUID targetUserId = UUID.randomUUID();
		User targetUser = new User();
		targetUser.setId(targetUserId);
		targetUser.setLab(lab);

		MemberProfile profile = new MemberProfile();
		profile.setActivityStatus(MemberProfileActivityStatus.ACTIVE);

		when(userRepository.findByIdAndLab(targetUserId, lab)).thenReturn(Optional.of(targetUser));
		when(memberProfileRepository.findByUser(targetUser)).thenReturn(Optional.of(profile));
		when(memberProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		AdminMemberDetailResponse detail = service.updateMemberActivityStatus(
				new AdminMemberService.UpdateMemberActivityStatusCommand(actorUserId, targetUserId, MemberProfileActivityStatus.INACTIVE));

		assertNotNull(detail);
		assertEquals(MemberProfileActivityStatus.INACTIVE, detail.profile().activityStatus());
	}

	@Test
	void getMemberProjects_Success() {
		UUID targetUserId = UUID.randomUUID();
		User targetUser = new User();
		targetUser.setId(targetUserId);
		targetUser.setLab(lab);

		Project project = new Project();
		project.setId(UUID.randomUUID());
		project.setName("Project Alpha");
		project.setCode("ALPHA");

		ProjectMember pm = new ProjectMember();
		pm.setProject(project);
		pm.setUser(targetUser);
		pm.setProjectRole(ProjectMemberRole.PROJECT_MEMBER);
		pm.setMemberStatus(ProjectMemberStatus.ACTIVE);

		when(userRepository.findByIdAndLab(targetUserId, lab)).thenReturn(Optional.of(targetUser));
		when(projectMemberRepository.findByUser(targetUser)).thenReturn(List.of(pm));

		List<AdminMemberProjectResponse> projects = service.getMemberProjects(actorUserId, targetUserId);

		assertNotNull(projects);
		assertEquals(1, projects.size());
		assertEquals("Project Alpha", projects.get(0).projectName());
	}

	@Test
	void getMemberEvaluations_Success() {
		UUID targetUserId = UUID.randomUUID();
		User targetUser = new User();
		targetUser.setId(targetUserId);
		targetUser.setLab(lab);

		Project project = new Project();
		project.setId(UUID.randomUUID());
		project.setName("Project Beta");

		MemberEvaluation eval = new MemberEvaluation();
		eval.setId(UUID.randomUUID());
		eval.setProject(project);
		eval.setMember(targetUser);
		eval.setEvaluationPeriod("Q1 2026");

		when(userRepository.findByIdAndLab(targetUserId, lab)).thenReturn(Optional.of(targetUser));
		when(memberEvaluationRepository.findByMember(targetUser)).thenReturn(List.of(eval));

		List<AdminMemberEvaluationResponse> evals = service.getMemberEvaluations(actorUserId, targetUserId);

		assertNotNull(evals);
		assertEquals(1, evals.size());
		assertEquals("Q1 2026", evals.get(0).evaluationPeriod());
	}
}

package com.smartlab.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.smartlab.entity.Lab;
import com.smartlab.entity.Project;
import com.smartlab.entity.ProjectMember;
import com.smartlab.entity.ProjectResearchField;
import com.smartlab.entity.ResearchField;
import com.smartlab.entity.User;
import com.smartlab.enums.CatalogStatus;
import com.smartlab.enums.ProjectMemberRole;
import com.smartlab.enums.ProjectMemberStatus;
import com.smartlab.enums.ProjectStatus;
import com.smartlab.enums.ProjectType;
import com.smartlab.enums.ProjectVisibility;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.ProjectMemberRepository;
import com.smartlab.repository.ProjectRepository;
import com.smartlab.repository.ProjectResearchFieldRepository;
import com.smartlab.repository.ResearchFieldRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.service.common.BusinessValidationService;
import com.smartlab.service.common.SlugService;

class AdminProjectServiceTests {

	private final ProjectRepository projectRepository = mock(ProjectRepository.class);
	private final ProjectResearchFieldRepository projectResearchFieldRepository =
			mock(ProjectResearchFieldRepository.class);
	private final ProjectMemberRepository projectMemberRepository = mock(ProjectMemberRepository.class);
	private final ResearchFieldRepository researchFieldRepository = mock(ResearchFieldRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final AdminRolePolicy rolePolicy = mock(AdminRolePolicy.class);
	private final AdminProjectService service = new AdminProjectService(
			projectRepository,
			projectResearchFieldRepository,
			projectMemberRepository,
			researchFieldRepository,
			userRepository,
			rolePolicy,
			new BusinessValidationService(),
			new SlugService());

	@Test
	void getProjectsScopesQueryToActorLabAndBulkLoadsFrontendAssociations() {
		UUID actorUserId = UUID.randomUUID();
		Lab lab = lab();
		Project project = project(lab);
		ProjectResearchField projectField = projectField(project, "AI");
		UUID leaderId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		ProjectMember leader = member(project, user(lab, leaderId, UserAccountStatus.ACTIVE), ProjectMemberRole.PROJECT_LEADER);
		ProjectMember member = member(project, user(lab, memberId, UserAccountStatus.LOCKED), ProjectMemberRole.PROJECT_MEMBER);
		User deletedUser = user(lab, UUID.randomUUID(), UserAccountStatus.DELETED);
		deletedUser.setDeletedAt(OffsetDateTime.now());
		ProjectMember deletedMembership = member(project, deletedUser, ProjectMemberRole.PROJECT_MEMBER);
		AdminProjectService.ProjectFilter filter = filter("AI");
		Pageable requestedPageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("progress")));
		when(rolePolicy.requireAdminActor(actorUserId)).thenReturn(actor(lab));
		when(projectRepository.findAdminProjects(
				eq(lab),
				eq(filter.statuses()),
				eq(filter.types()),
				eq(filter.visibilities()),
				eq("AI"),
				any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(project), PageRequest.of(0, 20), 1));
		when(projectResearchFieldRepository.findByProjectIn(List.of(project))).thenReturn(List.of(projectField));
		when(projectMemberRepository.findByProjectInAndMemberStatus(List.of(project), ProjectMemberStatus.ACTIVE))
				.thenReturn(List.of(leader, member, deletedMembership));

		Page<AdminProjectService.ProjectSummary> result = service.getProjects(
				actorUserId,
				filter,
				requestedPageable);

		assertEquals(1, result.getTotalElements());
		assertEquals(List.of("AI"), result.getContent().getFirst().fieldCodes());
		assertEquals(List.of(leaderId), result.getContent().getFirst().leaderIds());
		assertEquals(List.of(memberId), result.getContent().getFirst().memberIds());
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(projectRepository).findAdminProjects(
				eq(lab),
				eq(filter.statuses()),
				eq(filter.types()),
				eq(filter.visibilities()),
				eq("AI"),
				pageableCaptor.capture());
		assertEquals("progressPercent: DESC", pageableCaptor.getValue().getSort().toString());
	}

	@Test
	void getProjectsDoesNotIssueAssociationQueriesForEmptyPage() {
		UUID actorUserId = UUID.randomUUID();
		Lab lab = lab();
		AdminProjectService.ProjectFilter filter = filter(null);
		when(rolePolicy.requireAdminActor(actorUserId)).thenReturn(actor(lab));
		when(projectRepository.findAdminProjects(any(), any(), any(), any(), any(), any()))
				.thenReturn(Page.empty(PageRequest.of(0, 20)));

		Page<AdminProjectService.ProjectSummary> result = service.getProjects(
				actorUserId,
				filter,
				PageRequest.of(0, 20));

		assertEquals(0, result.getTotalElements());
		verify(projectResearchFieldRepository, never()).findByProjectIn(any());
		verify(projectMemberRepository, never()).findByProjectInAndMemberStatus(any(), any());
	}

	@Test
	void getProjectDetailReturnsSameFrontendSourceShape() {
		UUID actorUserId = UUID.randomUUID();
		Lab lab = lab();
		Project project = project(lab);
		when(rolePolicy.requireAdminActor(actorUserId)).thenReturn(actor(lab));
		when(projectRepository.findByIdAndLabAndDeletedAtIsNull(project.getId(), lab)).thenReturn(Optional.of(project));
		when(projectResearchFieldRepository.findByProjectIn(List.of(project))).thenReturn(List.of());
		when(projectMemberRepository.findByProjectInAndMemberStatus(List.of(project), ProjectMemberStatus.ACTIVE))
				.thenReturn(List.of());

		AdminProjectService.ProjectSummary result = service.getProjectDetail(actorUserId, project.getId());

		assertEquals(project.getId(), result.id());
		assertEquals("Short description", result.description());
		assertEquals(ProjectStatus.IN_PROGRESS, result.status());
	}

	@Test
	void getProjectDetailHidesMissingDeletedAndCrossLabProjectsBehindNotFound() {
		UUID actorUserId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		Lab lab = lab();
		when(rolePolicy.requireAdminActor(actorUserId)).thenReturn(actor(lab));
		when(projectRepository.findByIdAndLabAndDeletedAtIsNull(projectId, lab)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.getProjectDetail(actorUserId, projectId));
	}

	@Test
	void getProjectsRejectsUnsupportedPaginationBeforeQueryingProjects() {
		UUID actorUserId = UUID.randomUUID();
		Lab lab = lab();
		when(rolePolicy.requireAdminActor(actorUserId)).thenReturn(actor(lab));

		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.getProjects(actorUserId, filter(null), PageRequest.of(0, 101)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.getProjects(
						actorUserId,
						filter(null),
						PageRequest.of(0, 20, Sort.by("unknown"))));
		verify(projectRepository, never()).findAdminProjects(any(), any(), any(), any(), any(), any());
	}

	@Test
	void updateProgressPersistsValidatedValueAndReturnsUpdatedSummary() {
		UUID actorUserId = UUID.randomUUID();
		Lab lab = lab();
		Project project = project(lab);
		when(rolePolicy.requireAdminActor(actorUserId)).thenReturn(actor(lab));
		when(projectRepository.findByIdAndLabAndDeletedAtIsNull(project.getId(), lab))
				.thenReturn(Optional.of(project));
		when(projectRepository.save(project)).thenReturn(project);
		when(projectResearchFieldRepository.findByProjectIn(List.of(project))).thenReturn(List.of());
		when(projectMemberRepository.findByProjectInAndMemberStatus(List.of(project), ProjectMemberStatus.ACTIVE))
				.thenReturn(List.of());

		AdminProjectService.ProjectSummary result = service.updateProgress(actorUserId, project.getId(), 75);

		assertEquals(75, project.getProgressPercent());
		assertEquals(75, result.progress());
		verify(projectRepository).save(project);
	}

	@Test
	void deleteProjectSoftDeletesWithoutRemovingTheRecord() {
		UUID actorUserId = UUID.randomUUID();
		Lab lab = lab();
		Project project = project(lab);
		when(rolePolicy.requireAdminActor(actorUserId)).thenReturn(actor(lab));
		when(projectRepository.findByIdAndLabAndDeletedAtIsNull(project.getId(), lab))
				.thenReturn(Optional.of(project));

		service.deleteProject(actorUserId, project.getId());

		verify(projectRepository).save(project);
		org.junit.jupiter.api.Assertions.assertNotNull(project.getDeletedAt());
	}

	@Test
	void createProjectRejectsMissingRequiredDatesBeforeWritingAnything() {
		UUID actorUserId = UUID.randomUUID();
		Lab lab = lab();
		when(rolePolicy.requireAdminActor(actorUserId)).thenReturn(actor(lab));
		AdminProjectService.ProjectCommand command = new AdminProjectService.ProjectCommand(
				"PRJ-001",
				"Project",
				"Description",
				"Objective",
				ProjectType.RESEARCH,
				List.of("AI"),
				List.of(UUID.randomUUID()),
				null,
				LocalDate.of(2027, 1, 1),
				ProjectStatus.PROPOSED,
				0,
				ProjectVisibility.PUBLIC);

		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.createProject(actorUserId, command));
		verify(projectRepository, never()).save(any());
	}

	@Test
	void createProjectSetsJoinedAtForNewLeaderMembership() {
		UUID actorUserId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		Lab lab = lab();
		User actorUser = user(lab, actorUserId, UserAccountStatus.ACTIVE);
		User leader = user(lab, leaderId, UserAccountStatus.ACTIVE);
		ResearchField field = new ResearchField();
		field.setId(UUID.randomUUID());
		field.setCode("AI");
		field.setStatus(CatalogStatus.ACTIVE);
		when(rolePolicy.requireAdminActor(actorUserId))
				.thenReturn(new AdminRolePolicy.ActorContext(actorUser, Set.of("ADMIN")));
		when(researchFieldRepository.findByCodeIn(List.of("AI"))).thenReturn(List.of(field));
		when(userRepository.findByIdInAndLabAndAccountStatus(
				List.of(leaderId), lab, UserAccountStatus.ACTIVE))
				.thenReturn(List.of(leader));
		when(rolePolicy.activeRoleCodesByUserId(List.of(leader)))
				.thenReturn(Map.of(leaderId, List.of("LEADER")));
		when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
			Project project = invocation.getArgument(0);
			project.setId(UUID.randomUUID());
			return project;
		});

		service.createProject(actorUserId, new AdminProjectService.ProjectCommand(
				"PRJ-001",
				"Project",
				"Description",
				"Objective",
				ProjectType.RESEARCH,
				List.of("AI"),
				List.of(leaderId),
				LocalDate.of(2026, 1, 1),
				LocalDate.of(2027, 1, 1),
				ProjectStatus.PROPOSED,
				0,
				ProjectVisibility.PUBLIC));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<ProjectMember>> memberships =
				ArgumentCaptor.forClass((Class<List<ProjectMember>>) (Class<?>) List.class);
		verify(projectMemberRepository).saveAll(memberships.capture());
		assertEquals(1, memberships.getValue().size());
		assertNotNull(memberships.getValue().getFirst().getJoinedAt());
	}

	private static AdminProjectService.ProjectFilter filter(String fieldCode) {
		return new AdminProjectService.ProjectFilter(
				EnumSet.allOf(ProjectStatus.class),
				EnumSet.allOf(ProjectType.class),
				EnumSet.allOf(ProjectVisibility.class),
				fieldCode);
	}

	private static AdminRolePolicy.ActorContext actor(Lab lab) {
		User actor = user(lab, UUID.randomUUID(), UserAccountStatus.ACTIVE);
		return new AdminRolePolicy.ActorContext(actor, Set.of("ADMIN"));
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
		project.setSlug("atlas-perception");
		project.setCode("NL-24-07");
		project.setName("Atlas");
		project.setShortDescription("Short description");
		project.setObjective("Objective");
		project.setProjectType(ProjectType.RESEARCH);
		project.setVisibility(ProjectVisibility.PUBLIC);
		project.setStatus(ProjectStatus.IN_PROGRESS);
		project.setProgressPercent(62);
		project.setStartDate(LocalDate.of(2024, 3, 4));
		project.setExpectedEndDate(LocalDate.of(2026, 12, 15));
		return project;
	}

	private static ProjectResearchField projectField(Project project, String code) {
		ResearchField field = new ResearchField();
		field.setId(UUID.randomUUID());
		field.setCode(code);
		ProjectResearchField projectField = new ProjectResearchField();
		projectField.setProject(project);
		projectField.setResearchField(field);
		return projectField;
	}

	private static User user(Lab lab, UUID id, UserAccountStatus status) {
		User user = new User();
		user.setId(id);
		user.setLab(lab);
		user.setAccountStatus(status);
		return user;
	}

	private static ProjectMember member(Project project, User user, ProjectMemberRole role) {
		ProjectMember member = new ProjectMember();
		member.setProject(project);
		member.setUser(user);
		member.setProjectRole(role);
		member.setMemberStatus(ProjectMemberStatus.ACTIVE);
		return member;
	}
}

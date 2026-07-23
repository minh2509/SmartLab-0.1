package com.smartlab.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartlab.entity.Lab;
import com.smartlab.entity.Project;
import com.smartlab.entity.User;
import com.smartlab.enums.ProjectStatus;
import com.smartlab.repository.ProjectMemberRepository;
import com.smartlab.repository.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class AdminProjectLookupServiceTests {

	@Mock
	private AdminRolePolicy rolePolicy;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private ProjectMemberRepository projectMemberRepository;

	@InjectMocks
	private AdminProjectLookupService service;

	@Test
	void listsOnlyRepositoryScopedProjectsAsSmallOptions() {
		UUID actorId = UUID.randomUUID();
		Lab lab = new Lab();
		lab.setId(UUID.randomUUID());
		User actor = new User();
		actor.setId(actorId);
		actor.setLab(lab);
		AdminRolePolicy.ActorContext actorContext =
				new AdminRolePolicy.ActorContext(actor, Set.of(AdminRolePolicy.ADMIN_ROLE_CODE));

		Project project = new Project();
		project.setId(UUID.randomUUID());
		project.setLab(lab);
		project.setCode("AI-24-01");
		project.setName("Responsible AI Observatory");
		project.setSlug("responsible-ai-observatory");
		project.setStatus(ProjectStatus.IN_PROGRESS);
		ProjectMemberRepository.ProjectRecipientCount recipientCount =
				mock(ProjectMemberRepository.ProjectRecipientCount.class);

		when(rolePolicy.requireAdminActor(actorId)).thenReturn(actorContext);
		when(projectRepository.findByLabAndDeletedAtIsNullOrderByNameAscCodeAsc(lab))
				.thenReturn(List.of(project));
		when(recipientCount.getProjectId()).thenReturn(project.getId());
		when(recipientCount.getRecipientCount()).thenReturn(3L);
		when(projectMemberRepository.countEligibleNotificationRecipients(List.of(project)))
				.thenReturn(List.of(recipientCount));

		List<AdminProjectLookupService.ProjectOption> result = service.listProjectOptions(actorId);

		assertEquals(1, result.size());
		assertEquals(project.getId(), result.getFirst().id());
		assertEquals(project.getCode(), result.getFirst().code());
		assertEquals(project.getName(), result.getFirst().name());
		assertEquals(project.getSlug(), result.getFirst().slug());
		assertEquals(ProjectStatus.IN_PROGRESS, result.getFirst().status());
		assertEquals(3L, result.getFirst().activeRecipientCount());
		verify(projectRepository).findByLabAndDeletedAtIsNullOrderByNameAscCodeAsc(lab);
		verify(projectMemberRepository).countEligibleNotificationRecipients(List.of(project));
	}

	@Test
	void defaultsActiveRecipientCountToZeroWhenAggregateHasNoProjectRow() {
		UUID actorId = UUID.randomUUID();
		Lab lab = new Lab();
		lab.setId(UUID.randomUUID());
		User actor = new User();
		actor.setId(actorId);
		actor.setLab(lab);
		AdminRolePolicy.ActorContext actorContext =
				new AdminRolePolicy.ActorContext(actor, Set.of(AdminRolePolicy.ADMIN_ROLE_CODE));

		Project project = new Project();
		project.setId(UUID.randomUUID());
		project.setLab(lab);
		project.setCode("SE-24-02");
		project.setName("Software Reliability Observatory");
		project.setSlug("software-reliability-observatory");
		project.setStatus(ProjectStatus.PROPOSED);

		when(rolePolicy.requireAdminActor(actorId)).thenReturn(actorContext);
		when(projectRepository.findByLabAndDeletedAtIsNullOrderByNameAscCodeAsc(lab))
				.thenReturn(List.of(project));
		when(projectMemberRepository.countEligibleNotificationRecipients(List.of(project)))
				.thenReturn(List.of());

		List<AdminProjectLookupService.ProjectOption> result = service.listProjectOptions(actorId);

		assertEquals(1, result.size());
		assertEquals(0L, result.getFirst().activeRecipientCount());
		verify(projectMemberRepository).countEligibleNotificationRecipients(List.of(project));
	}
}

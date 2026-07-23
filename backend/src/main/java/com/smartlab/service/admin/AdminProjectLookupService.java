package com.smartlab.service.admin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.enums.ProjectStatus;
import com.smartlab.repository.ProjectMemberRepository;
import com.smartlab.repository.ProjectRepository;

@Service
@Profile("!nodb")
public class AdminProjectLookupService {

	private final AdminRolePolicy rolePolicy;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;

	public AdminProjectLookupService(
			AdminRolePolicy rolePolicy,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository) {
		this.rolePolicy = rolePolicy;
		this.projectRepository = projectRepository;
		this.projectMemberRepository = projectMemberRepository;
	}

	@Transactional(readOnly = true)
	public List<ProjectOption> listProjectOptions(UUID actorUserId) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		var projects = projectRepository.findByLabAndDeletedAtIsNullOrderByNameAscCodeAsc(actor.lab());
		Map<UUID, Long> recipientCounts = projects.isEmpty()
				? Map.of()
				: projectMemberRepository.countEligibleNotificationRecipients(projects)
						.stream()
						.collect(Collectors.toMap(
								ProjectMemberRepository.ProjectRecipientCount::getProjectId,
								ProjectMemberRepository.ProjectRecipientCount::getRecipientCount));
		return projects
				.stream()
				.map(project -> new ProjectOption(
						project.getId(),
						project.getCode(),
						project.getName(),
						project.getSlug(),
						project.getStatus(),
						recipientCounts.getOrDefault(project.getId(), 0L)))
				.toList();
	}

	public record ProjectOption(
			UUID id,
			String code,
			String name,
			String slug,
			ProjectStatus status,
			long activeRecipientCount) {
	}
}

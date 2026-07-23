package com.smartlab.service.admin;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.Lab;
import com.smartlab.entity.Project;
import com.smartlab.entity.ProjectMember;
import com.smartlab.entity.ProjectResearchField;
import com.smartlab.entity.User;
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
import com.smartlab.service.common.BusinessValidationService;

@Service
@Profile("!nodb")
public class AdminProjectService {

	private static final int MAX_PAGE_SIZE = 100;
	private static final Map<String, String> SORT_PROPERTIES = Map.of(
			"code", "code",
			"createdAt", "createdAt",
			"expectedEnd", "expectedEndDate",
			"name", "name",
			"progress", "progressPercent",
			"startDate", "startDate",
			"status", "status",
			"type", "projectType",
			"updatedAt", "updatedAt");

	private final ProjectRepository projectRepository;
	private final ProjectResearchFieldRepository projectResearchFieldRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final AdminRolePolicy rolePolicy;
	private final BusinessValidationService validationService;

	public AdminProjectService(
			ProjectRepository projectRepository,
			ProjectResearchFieldRepository projectResearchFieldRepository,
			ProjectMemberRepository projectMemberRepository,
			AdminRolePolicy rolePolicy,
			BusinessValidationService validationService) {
		this.projectRepository = projectRepository;
		this.projectResearchFieldRepository = projectResearchFieldRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.rolePolicy = rolePolicy;
		this.validationService = validationService;
	}

	@Transactional(readOnly = true)
	public Page<ProjectSummary> getProjects(UUID actorUserId, ProjectFilter filter, Pageable pageable) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		ProjectFilter requiredFilter = requireFilter(filter);
		Pageable normalizedPageable = normalizePageable(pageable);
		Page<Project> projects = projectRepository.findAdminProjects(
				actor.lab(),
				requiredFilter.statuses(),
				requiredFilter.types(),
				requiredFilter.visibilities(),
				requiredFilter.researchFieldCode(),
				normalizedPageable);
		return enrich(projects);
	}

	@Transactional(readOnly = true)
	public ProjectSummary getProjectDetail(UUID actorUserId, UUID projectId) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		validationService.requireId(projectId, "Project ID");
		Project project = projectRepository.findByIdAndLabAndDeletedAtIsNull(projectId, actor.lab())
				.orElseThrow(() -> new ResourceNotFoundException("Project was not found."));
		return summaries(List.of(project)).get(project.getId());
	}

	private Page<ProjectSummary> enrich(Page<Project> projects) {
		if (projects.isEmpty()) {
			return projects.map(project -> ProjectSummary.from(project, List.of(), List.of()));
		}
		Map<UUID, ProjectSummary> summaries = summaries(projects.getContent());
		return projects.map(project -> summaries.get(project.getId()));
	}

	private Map<UUID, ProjectSummary> summaries(List<Project> projects) {
		Map<UUID, List<String>> fieldCodesByProject = projectResearchFieldRepository.findByProjectIn(projects)
				.stream()
				.filter(projectField -> projectField.getProject() != null)
				.filter(projectField -> projectField.getResearchField() != null)
				.collect(Collectors.groupingBy(
						projectField -> projectField.getProject().getId(),
						Collectors.mapping(
								projectField -> projectField.getResearchField().getCode(),
								Collectors.collectingAndThen(Collectors.toSet(), codes -> codes.stream().sorted().toList()))));
		Map<UUID, List<ProjectMember>> membersByProject = projectMemberRepository
				.findByProjectInAndMemberStatus(projects, ProjectMemberStatus.ACTIVE)
				.stream()
				.filter(AdminProjectService::hasVisibleUser)
				.collect(Collectors.groupingBy(member -> member.getProject().getId()));

		Map<UUID, ProjectSummary> result = new LinkedHashMap<>();
		for (Project project : projects) {
			result.put(
					project.getId(),
					ProjectSummary.from(
							project,
							fieldCodesByProject.getOrDefault(project.getId(), List.of()),
							membersByProject.getOrDefault(project.getId(), List.of())));
		}
		return Map.copyOf(result);
	}

	private static boolean hasVisibleUser(ProjectMember member) {
		if (member == null || member.getProject() == null || member.getUser() == null) {
			return false;
		}
		User user = member.getUser();
		return user.getDeletedAt() == null && user.getAccountStatus() != UserAccountStatus.DELETED;
	}

	private static ProjectFilter requireFilter(ProjectFilter filter) {
		if (filter == null) {
			throw new InvalidAdminServiceInputException("Project filter must not be null.");
		}
		if (filter.statuses() == null || filter.statuses().isEmpty()) {
			throw new InvalidAdminServiceInputException("Project status filter must not be empty.");
		}
		if (filter.types() == null || filter.types().isEmpty()) {
			throw new InvalidAdminServiceInputException("Project type filter must not be empty.");
		}
		if (filter.visibilities() == null || filter.visibilities().isEmpty()) {
			throw new InvalidAdminServiceInputException("Project visibility filter must not be empty.");
		}
		return filter;
	}

	private static Pageable normalizePageable(Pageable pageable) {
		if (pageable == null || pageable.isUnpaged()) {
			throw new InvalidAdminServiceInputException("Project pagination is required.");
		}
		if (pageable.getPageSize() < 1 || pageable.getPageSize() > MAX_PAGE_SIZE) {
			throw new InvalidAdminServiceInputException("Project page size must be between 1 and 100.");
		}
		List<Sort.Order> orders = pageable.getSort()
				.stream()
				.map(AdminProjectService::normalizeSortOrder)
				.toList();
		Sort sort = orders.isEmpty() ? Sort.by(Sort.Order.asc("name")) : Sort.by(orders);
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
	}

	private static Sort.Order normalizeSortOrder(Sort.Order order) {
		String property = SORT_PROPERTIES.get(order.getProperty());
		if (property == null) {
			throw new InvalidAdminServiceInputException("Unsupported project sort property.");
		}
		return new Sort.Order(order.getDirection(), property, order.getNullHandling());
	}

	public record ProjectFilter(
			Set<ProjectStatus> statuses,
			Set<ProjectType> types,
			Set<ProjectVisibility> visibilities,
			String researchFieldCode) {

		public ProjectFilter {
			statuses = immutableEnumSet(statuses);
			types = immutableEnumSet(types);
			visibilities = immutableEnumSet(visibilities);
		}

		private static <E extends Enum<E>> Set<E> immutableEnumSet(Collection<E> values) {
			if (values == null || values.isEmpty()) {
				return Set.of();
			}
			return Set.copyOf(EnumSet.copyOf(values));
		}
	}

	public record ProjectSummary(
			UUID id,
			String slug,
			String code,
			String name,
			String description,
			String objective,
			ProjectType type,
			List<String> fieldCodes,
			List<UUID> leaderIds,
			List<UUID> memberIds,
			LocalDate startDate,
			LocalDate expectedEnd,
			ProjectStatus status,
			int progress,
			ProjectVisibility visibility,
			LocalDate actualEndDate,
			OffsetDateTime createdAt,
			OffsetDateTime updatedAt) {

		static ProjectSummary from(Project project, List<String> fieldCodes, List<ProjectMember> members) {
			List<UUID> leaderIds = memberIds(members, ProjectMemberRole.PROJECT_LEADER);
			List<UUID> memberIds = memberIds(members, ProjectMemberRole.PROJECT_MEMBER);
			String description = project.getDescription();
			if (description == null || description.isBlank()) {
				description = project.getShortDescription();
			}
			return new ProjectSummary(
					project.getId(),
					project.getSlug(),
					project.getCode(),
					project.getName(),
					description,
					project.getObjective(),
					project.getProjectType(),
					List.copyOf(fieldCodes),
					leaderIds,
					memberIds,
					project.getStartDate(),
					project.getExpectedEndDate(),
					project.getStatus(),
					project.getProgressPercent(),
					project.getVisibility(),
					project.getActualEndDate(),
					project.getCreatedAt(),
					project.getUpdatedAt());
		}

		private static List<UUID> memberIds(List<ProjectMember> members, ProjectMemberRole role) {
			return members.stream()
					.filter(member -> member.getProjectRole() == role)
					.map(ProjectMember::getUser)
					.map(User::getId)
					.distinct()
					.sorted()
					.toList();
		}
	}
}

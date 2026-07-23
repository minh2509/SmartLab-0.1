package com.smartlab.service.admin;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
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
import com.smartlab.entity.ResearchField;
import com.smartlab.entity.User;
import com.smartlab.enums.CatalogStatus;
import com.smartlab.enums.ProjectMemberRole;
import com.smartlab.enums.ProjectMemberStatus;
import com.smartlab.enums.ProjectStatus;
import com.smartlab.enums.ProjectType;
import com.smartlab.enums.ProjectVisibility;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.exception.ConflictingAdminOperationException;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.ProjectMemberRepository;
import com.smartlab.repository.ProjectRepository;
import com.smartlab.repository.ProjectResearchFieldRepository;
import com.smartlab.repository.ResearchFieldRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.service.common.BusinessValidationService;
import com.smartlab.service.common.SlugService;

@Service
@Profile("!nodb")
public class AdminProjectService {

	private static final int MAX_PAGE_SIZE = 100;
	private static final Map<ProjectStatus, Set<ProjectStatus>> STATUS_TRANSITIONS = Map.of(
			ProjectStatus.PROPOSED, Set.of(ProjectStatus.PREPARING, ProjectStatus.IN_PROGRESS),
			ProjectStatus.PREPARING, Set.of(ProjectStatus.IN_PROGRESS),
			ProjectStatus.IN_PROGRESS, Set.of(ProjectStatus.PAUSED, ProjectStatus.COMPLETED),
			ProjectStatus.PAUSED, Set.of(ProjectStatus.IN_PROGRESS),
			ProjectStatus.COMPLETED, Set.of(ProjectStatus.CLOSED),
			ProjectStatus.CLOSED, Set.of());
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
	private final ResearchFieldRepository researchFieldRepository;
	private final UserRepository userRepository;
	private final AdminRolePolicy rolePolicy;
	private final BusinessValidationService validationService;
	private final SlugService slugService;

	public AdminProjectService(
			ProjectRepository projectRepository,
			ProjectResearchFieldRepository projectResearchFieldRepository,
			ProjectMemberRepository projectMemberRepository,
			ResearchFieldRepository researchFieldRepository,
			UserRepository userRepository,
			AdminRolePolicy rolePolicy,
			BusinessValidationService validationService,
			SlugService slugService) {
		this.projectRepository = projectRepository;
		this.projectResearchFieldRepository = projectResearchFieldRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.researchFieldRepository = researchFieldRepository;
		this.userRepository = userRepository;
		this.rolePolicy = rolePolicy;
		this.validationService = validationService;
		this.slugService = slugService;
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

	@Transactional
	public ProjectSummary createProject(UUID actorUserId, ProjectCommand command) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		ProjectCommand required = validateCommand(command);
		String code = required.code().trim();
		if (projectRepository.existsByLabAndCode(actor.lab(), code)) {
			throw new ConflictingAdminOperationException("Project code already exists in this lab.");
		}
		List<ResearchField> fields = resolveFields(required.fieldCodes());
		List<User> leaders = resolveLeaders(actor, required.leaderIds());

		Project project = new Project();
		project.setLab(actor.lab());
		project.setCode(code);
		project.setName(required.name().trim());
		project.setSlug(slugService.generateUniqueSlug(
			required.name(),
			slug -> projectRepository.existsByLabAndSlug(actor.lab(), slug)));
		applyProjectValues(project, required, true);
		project.setCreatedBy(actor.actor());
		Project saved = projectRepository.save(project);
		List<ProjectResearchField> fieldMappings = saveFields(saved, fields);
		List<ProjectMember> memberships = saveLeaders(saved, leaders, actor.actor(), new ArrayList<>());
		return ProjectSummary.from(
			saved,
			fieldMappings.stream().map(mapping -> mapping.getResearchField().getCode()).sorted().toList(),
			memberships);
	}

	@Transactional
	public ProjectSummary updateProject(UUID actorUserId, UUID projectId, ProjectCommand command) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		Project project = requireMutableProject(actor, projectId);
		ProjectCommand required = validateCommand(command);
		String code = required.code().trim();
		if (projectRepository.existsByLabAndCodeAndIdNot(actor.lab(), code, project.getId())) {
			throw new ConflictingAdminOperationException("Project code already exists in this lab.");
		}
		List<ResearchField> fields = resolveFields(required.fieldCodes());
		List<User> leaders = resolveLeaders(actor, required.leaderIds());

		project.setCode(code);
		project.setName(required.name().trim());
		project.setSlug(slugService.generateUniqueSlug(
			required.name(),
			slug -> projectRepository.existsByLabAndSlugAndIdNot(actor.lab(), slug, project.getId())));
		ProjectStatus targetStatus = project.getStatus() == ProjectStatus.PREPARING
				&& required.status() == ProjectStatus.PROPOSED
						? ProjectStatus.PREPARING
						: required.status();
		if (project.getStatus() != targetStatus) {
			validationService.validateStateTransition(
					project.getStatus(),
					targetStatus,
					STATUS_TRANSITIONS.getOrDefault(project.getStatus(), Set.of()));
		}
		applyProjectValues(project, required, false);
		project.setStatus(targetStatus);
		Project saved = projectRepository.save(project);
		List<ProjectResearchField> fieldMappings = replaceFieldsInternal(saved, fields);
		List<ProjectMember> memberships = replaceLeadersInternal(saved, leaders, actor.actor());
		return ProjectSummary.from(
			saved,
			fieldMappings.stream().map(mapping -> mapping.getResearchField().getCode()).sorted().toList(),
			memberships.stream().filter(member -> member.getMemberStatus() == ProjectMemberStatus.ACTIVE).toList());
	}

	@Transactional
	public ProjectSummary updateStatus(UUID actorUserId, UUID projectId, ProjectStatus targetStatus) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		Project project = requireMutableProject(actor, projectId);
		validationService.validateStateTransition(
				project.getStatus(),
				targetStatus,
				STATUS_TRANSITIONS.getOrDefault(project.getStatus(), Set.of()));
		project.setStatus(targetStatus);
		projectRepository.save(project);
		return summary(project);
	}

	@Transactional
	public ProjectSummary updateProgress(UUID actorUserId, UUID projectId, Integer progress) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		Project project = requireMutableProject(actor, projectId);
		project.setProgressPercent(validationService.validateProgress(progress));
		projectRepository.save(project);
		return summary(project);
	}

	@Transactional
	public ProjectSummary replaceResearchFields(UUID actorUserId, UUID projectId, Collection<String> fieldCodes) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		Project project = requireMutableProject(actor, projectId);
		List<ProjectResearchField> mappings = replaceFieldsInternal(project, resolveFields(fieldCodes));
		return ProjectSummary.from(
			project,
			mappings.stream().map(mapping -> mapping.getResearchField().getCode()).sorted().toList(),
			projectMemberRepository.findByProjectAndMemberStatus(project, ProjectMemberStatus.ACTIVE));
	}

	@Transactional
	public ProjectSummary replaceLeaders(UUID actorUserId, UUID projectId, Collection<UUID> leaderIds) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		Project project = requireMutableProject(actor, projectId);
		List<ProjectMember> memberships = replaceLeadersInternal(
			project,
			resolveLeaders(actor, leaderIds),
			actor.actor());
		return ProjectSummary.from(
			project,
			projectResearchFieldRepository.findByProject(project).stream()
					.map(mapping -> mapping.getResearchField().getCode()).sorted().toList(),
			memberships.stream().filter(member -> member.getMemberStatus() == ProjectMemberStatus.ACTIVE).toList());
	}

	@Transactional
	public void deleteProject(UUID actorUserId, UUID projectId) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		validationService.requireId(projectId, "Project ID");
		Project project = projectRepository.findByIdAndLabAndDeletedAtIsNull(projectId, actor.lab())
				.orElseThrow(() -> new ResourceNotFoundException("Project was not found."));
		project.setDeletedAt(OffsetDateTime.now());
		projectRepository.save(project);
	}

	private Project requireMutableProject(AdminRolePolicy.ActorContext actor, UUID projectId) {
		validationService.requireId(projectId, "Project ID");
		Project project = projectRepository.findByIdAndLabAndDeletedAtIsNull(projectId, actor.lab())
				.orElseThrow(() -> new ResourceNotFoundException("Project was not found."));
		if (project.getStatus() == ProjectStatus.CLOSED) {
			throw new ConflictingAdminOperationException("Closed projects cannot be modified.");
		}
		return project;
	}

	private ProjectCommand validateCommand(ProjectCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Project command must not be null.");
		}
		if (isBlank(command.code()) || isBlank(command.name()) || isBlank(command.description())
				|| isBlank(command.objective())) {
			throw new InvalidAdminServiceInputException("Project text fields must not be blank.");
		}
		if (command.type() == null || command.status() == null || command.visibility() == null) {
			throw new InvalidAdminServiceInputException("Project type, status, and visibility are required.");
		}
		if (command.startDate() == null || command.expectedEnd() == null) {
			throw new InvalidAdminServiceInputException("Project start date and expected end date are required.");
		}
		validationService.validateProgress(command.progress());
		validationService.validateDateRange(command.startDate(), command.expectedEnd(), null);
		if (normalizeFieldCodes(command.fieldCodes()).isEmpty()) {
			throw new InvalidAdminServiceInputException("At least one research field is required.");
		}
		if (normalizeIds(command.leaderIds()).isEmpty()) {
			throw new InvalidAdminServiceInputException("At least one project leader is required.");
		}
		return command;
	}

	private void applyProjectValues(Project project, ProjectCommand command, boolean includeStatus) {
		project.setDescription(command.description().trim());
		project.setShortDescription(command.description().trim());
		project.setObjective(command.objective().trim());
		project.setProjectType(command.type());
		project.setVisibility(command.visibility());
		if (includeStatus) {
			project.setStatus(command.status());
		}
		project.setProgressPercent(validationService.validateProgress(command.progress()));
		project.setStartDate(command.startDate());
		project.setExpectedEndDate(command.expectedEnd());
	}

	private List<ResearchField> resolveFields(Collection<String> fieldCodes) {
		List<String> normalized = normalizeFieldCodes(fieldCodes);
		if (normalized.isEmpty()) {
			throw new InvalidAdminServiceInputException("At least one research field is required.");
		}
		Map<String, ResearchField> byCode = researchFieldRepository.findByCodeIn(normalized).stream()
				.collect(Collectors.toMap(field -> field.getCode().toUpperCase(), field -> field));
		if (byCode.size() != normalized.size()) {
			throw new ResourceNotFoundException("Research field was not found.");
		}
		List<ResearchField> fields = normalized.stream().map(byCode::get).toList();
		if (fields.stream().anyMatch(field -> field.getStatus() != CatalogStatus.ACTIVE)) {
			throw new ConflictingAdminOperationException("Inactive research fields cannot be assigned.");
		}
		return fields;
	}

	private List<User> resolveLeaders(AdminRolePolicy.ActorContext actor, Collection<UUID> leaderIds) {
		List<UUID> normalized = normalizeIds(leaderIds);
		if (normalized.isEmpty()) {
			throw new InvalidAdminServiceInputException("At least one project leader is required.");
		}
		List<User> users = userRepository.findByIdInAndLabAndAccountStatus(
			normalized,
			actor.lab(),
			UserAccountStatus.ACTIVE);
		if (users.size() != normalized.size()) {
			throw new ResourceNotFoundException("Project leader was not found.");
		}
		Map<UUID, List<String>> roles = rolePolicy.activeRoleCodesByUserId(users);
		for (User user : users) {
			List<String> codes = roles.getOrDefault(user.getId(), List.of());
			if (!codes.contains(AdminRolePolicy.LEADER_ROLE_CODE)
					&& !codes.contains(AdminRolePolicy.ADMIN_ROLE_CODE)) {
				throw new ConflictingAdminOperationException(
						"Project leaders must have the LEADER or ADMIN system role.");
			}
		}
		Map<UUID, User> byId = users.stream().collect(Collectors.toMap(User::getId, user -> user));
		return normalized.stream().map(byId::get).toList();
	}

	private List<ProjectResearchField> saveFields(Project project, List<ResearchField> fields) {
		List<ProjectResearchField> mappings = fields.stream().map(field -> {
			ProjectResearchField mapping = new ProjectResearchField();
			mapping.setProject(project);
			mapping.setResearchField(field);
			return mapping;
		}).toList();
		return projectResearchFieldRepository.saveAll(mappings);
	}

	private List<ProjectResearchField> replaceFieldsInternal(Project project, List<ResearchField> fields) {
		List<ProjectResearchField> existing = projectResearchFieldRepository.findByProject(project);
		projectResearchFieldRepository.deleteAll(existing);
		projectResearchFieldRepository.flush();
		return saveFields(project, fields);
	}

	private List<ProjectMember> saveLeaders(
			Project project,
			List<User> leaders,
			User actor,
			List<ProjectMember> existing) {
		Map<UUID, ProjectMember> byUserId = existing.stream()
				.collect(Collectors.toMap(member -> member.getUser().getId(), member -> member));
		Set<UUID> desired = leaders.stream().map(User::getId).collect(Collectors.toSet());
		for (ProjectMember member : existing) {
			if (member.getProjectRole() == ProjectMemberRole.PROJECT_LEADER
					&& !desired.contains(member.getUser().getId())) {
				member.setProjectRole(ProjectMemberRole.PROJECT_MEMBER);
			}
		}
		for (User leader : leaders) {
			ProjectMember member = byUserId.computeIfAbsent(leader.getId(), ignored -> {
				ProjectMember created = new ProjectMember();
				created.setProject(project);
				created.setUser(leader);
				created.setJoinedAt(OffsetDateTime.now());
				existing.add(created);
				return created;
			});
			member.setProjectRole(ProjectMemberRole.PROJECT_LEADER);
			member.setMemberStatus(ProjectMemberStatus.ACTIVE);
			member.setLeftAt(null);
			member.setAddedBy(actor);
		}
		return projectMemberRepository.saveAll(existing);
	}

	private List<ProjectMember> replaceLeadersInternal(Project project, List<User> leaders, User actor) {
		return saveLeaders(
			project,
			leaders,
			actor,
			new ArrayList<>(projectMemberRepository.findByProject(project)));
	}

	private ProjectSummary summary(Project project) {
		return summaries(List.of(project)).get(project.getId());
	}

	private static List<String> normalizeFieldCodes(Collection<String> fieldCodes) {
		if (fieldCodes == null) {
			return List.of();
		}
		return fieldCodes.stream()
				.filter(code -> code != null && !code.isBlank())
				.map(code -> code.trim().toUpperCase())
				.collect(Collectors.collectingAndThen(
						Collectors.toCollection(LinkedHashSet::new),
						List::copyOf));
	}

	private static List<UUID> normalizeIds(Collection<UUID> ids) {
		if (ids == null) {
			return List.of();
		}
		return List.copyOf(ids.stream()
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new)));
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
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

	public record ProjectCommand(
			String code,
			String name,
			String description,
			String objective,
			ProjectType type,
			List<String> fieldCodes,
			List<UUID> leaderIds,
			LocalDate startDate,
			LocalDate expectedEnd,
			ProjectStatus status,
			Integer progress,
			ProjectVisibility visibility) {
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

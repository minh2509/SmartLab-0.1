package com.smartlab.mapper;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.smartlab.dto.request.admin.SaveAdminProjectRequest;
import com.smartlab.dto.response.admin.AdminProjectResponse;
import com.smartlab.dto.response.admin.PageResponse;
import com.smartlab.enums.ProjectStatus;
import com.smartlab.enums.ProjectType;
import com.smartlab.enums.ProjectVisibility;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.service.admin.AdminProjectService;

@Component
public class AdminProjectApiMapper {

	private static final Set<String> FRONTEND_FIELD_CODES = Set.of("AI", "ROBOTICS", "SE");

	public AdminProjectService.ProjectFilter toFilter(
			String status,
			String type,
			String visibility,
			String field) {
		return new AdminProjectService.ProjectFilter(
				toStatuses(status),
				toTypes(type),
				toVisibilities(visibility),
				toResearchFieldCode(field));
	}

	public AdminProjectService.ProjectCommand toCommand(SaveAdminProjectRequest request) {
		if (request == null) {
			throw new InvalidAdminServiceInputException("Project request must not be null.");
		}
		return new AdminProjectService.ProjectCommand(
				request.code(),
				request.name(),
				request.description(),
				request.objective(),
				toProjectType(request.type()),
				request.fields(),
				request.leaderIds(),
				request.startDate(),
				request.expectedEnd(),
				toCreateOrUpdateStatus(request.status()),
				request.progress(),
				toProjectVisibility(request.visibility()));
	}

	public ProjectStatus toTargetStatus(String value) {
		return switch (normalizeRequired(value, "Project status")) {
			case "proposed" -> ProjectStatus.PROPOSED;
			case "planning", "preparing" -> ProjectStatus.PREPARING;
			case "active", "in progress" -> ProjectStatus.IN_PROGRESS;
			case "on hold", "paused" -> ProjectStatus.PAUSED;
			case "publishing" -> ProjectStatus.COMPLETED;
			case "completed", "closed" -> ProjectStatus.CLOSED;
			default -> throw new InvalidAdminServiceInputException("Unsupported project status.");
		};
	}

	public PageResponse<AdminProjectResponse> toPageResponse(Page<AdminProjectService.ProjectSummary> page) {
		List<AdminProjectResponse> items = page.getContent().stream().map(this::toResponse).toList();
		return new PageResponse<>(
				items,
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast());
	}

	public AdminProjectResponse toResponse(AdminProjectService.ProjectSummary summary) {
		return new AdminProjectResponse(
				summary.id(),
				summary.slug(),
				summary.code(),
				summary.name(),
				summary.description(),
				summary.objective(),
				toFrontendType(summary.type()),
				summary.fieldCodes().stream().map(AdminProjectApiMapper::toFrontendField).toList(),
				summary.leaderIds(),
				summary.memberIds(),
				summary.startDate(),
				summary.expectedEnd(),
				toFrontendStatus(summary.status()),
				summary.progress(),
				toFrontendVisibility(summary.visibility()),
				summary.actualEndDate(),
				summary.createdAt(),
				summary.updatedAt());
	}

	private static Set<ProjectStatus> toStatuses(String value) {
		if (value == null || value.isBlank()) {
			return EnumSet.allOf(ProjectStatus.class);
		}
		return switch (normalize(value)) {
			case "planning" -> EnumSet.of(ProjectStatus.PROPOSED, ProjectStatus.PREPARING);
			case "active" -> EnumSet.of(ProjectStatus.IN_PROGRESS);
			case "publishing" -> EnumSet.of(ProjectStatus.COMPLETED);
			case "completed" -> EnumSet.of(ProjectStatus.CLOSED);
			case "on hold" -> EnumSet.of(ProjectStatus.PAUSED);
			default -> throw new InvalidAdminServiceInputException("Unsupported project status filter.");
		};
	}

	private static Set<ProjectType> toTypes(String value) {
		if (value == null || value.isBlank()) {
			return EnumSet.allOf(ProjectType.class);
		}
		return switch (normalize(value)) {
			case "research" -> EnumSet.of(ProjectType.RESEARCH);
			case "production" -> EnumSet.of(ProjectType.PRODUCTION);
			default -> throw new InvalidAdminServiceInputException("Unsupported project type filter.");
		};
	}

	private static Set<ProjectVisibility> toVisibilities(String value) {
		if (value == null || value.isBlank()) {
			return EnumSet.allOf(ProjectVisibility.class);
		}
		return switch (normalize(value)) {
			case "public" -> EnumSet.of(ProjectVisibility.PUBLIC);
			case "internal" -> EnumSet.of(
					ProjectVisibility.LAB_INTERNAL,
					ProjectVisibility.PROJECT_INTERNAL,
					ProjectVisibility.PRIVATE);
			default -> throw new InvalidAdminServiceInputException("Unsupported project visibility filter.");
		};
	}

	private static String toResearchFieldCode(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String code = value.trim().toUpperCase(Locale.ROOT);
		if (!FRONTEND_FIELD_CODES.contains(code)) {
			throw new InvalidAdminServiceInputException("Unsupported project research field filter.");
		}
		return code;
	}

	private static String toFrontendStatus(ProjectStatus status) {
		return switch (status) {
			case PROPOSED, PREPARING -> "Planning";
			case IN_PROGRESS -> "Active";
			case PAUSED -> "On hold";
			case COMPLETED -> "Publishing";
			case CLOSED -> "Completed";
		};
	}

	private static String toFrontendType(ProjectType type) {
		return switch (type) {
			case RESEARCH -> "Research";
			case PRODUCTION -> "Production";
		};
	}

	private static String toFrontendVisibility(ProjectVisibility visibility) {
		return visibility == ProjectVisibility.PUBLIC ? "public" : "internal";
	}

	private static String toFrontendField(String code) {
		return code.toLowerCase(Locale.ROOT);
	}

	private static String normalize(String value) {
		return value.trim()
				.toLowerCase(Locale.ROOT)
				.replace('_', ' ')
				.replace('-', ' ')
				.replaceAll("\\s+", " ");
	}

	private static ProjectStatus toCreateOrUpdateStatus(String value) {
		return switch (normalizeRequired(value, "Project status")) {
			case "planning", "proposed" -> ProjectStatus.PROPOSED;
			case "preparing" -> ProjectStatus.PREPARING;
			case "active", "in progress" -> ProjectStatus.IN_PROGRESS;
			case "on hold", "paused" -> ProjectStatus.PAUSED;
			case "publishing" -> ProjectStatus.COMPLETED;
			case "completed", "closed" -> ProjectStatus.CLOSED;
			default -> throw new InvalidAdminServiceInputException("Unsupported project status.");
		};
	}

	private static ProjectType toProjectType(String value) {
		return switch (normalizeRequired(value, "Project type")) {
			case "research" -> ProjectType.RESEARCH;
			case "production" -> ProjectType.PRODUCTION;
			default -> throw new InvalidAdminServiceInputException("Unsupported project type.");
		};
	}

	private static ProjectVisibility toProjectVisibility(String value) {
		return switch (normalizeRequired(value, "Project visibility")) {
			case "public" -> ProjectVisibility.PUBLIC;
			case "internal", "lab internal" -> ProjectVisibility.LAB_INTERNAL;
			case "project internal" -> ProjectVisibility.PROJECT_INTERNAL;
			case "private" -> ProjectVisibility.PRIVATE;
			default -> throw new InvalidAdminServiceInputException("Unsupported project visibility.");
		};
	}

	private static String normalizeRequired(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new InvalidAdminServiceInputException(fieldName + " must not be blank.");
		}
		return normalize(value);
	}
}

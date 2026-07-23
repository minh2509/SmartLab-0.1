package com.smartlab.mapper;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.smartlab.dto.response.admin.AdminProjectMemberResponse;
import com.smartlab.enums.ProjectMemberRole;
import com.smartlab.enums.ProjectMemberStatus;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.service.admin.AdminProjectMemberService;

@Component
public class AdminProjectMemberApiMapper {

	public ProjectMemberRole toRole(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return switch (normalize(value)) {
			case "leader", "project leader" -> ProjectMemberRole.PROJECT_LEADER;
			case "member", "project member" -> ProjectMemberRole.PROJECT_MEMBER;
			default -> throw new InvalidAdminServiceInputException("Unsupported project member role.");
		};
	}

	public ProjectMemberStatus toStatus(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return switch (normalize(value)) {
			case "active" -> ProjectMemberStatus.ACTIVE;
			case "removed" -> ProjectMemberStatus.REMOVED;
			case "left" -> ProjectMemberStatus.LEFT;
			default -> throw new InvalidAdminServiceInputException("Unsupported project member status.");
		};
	}

	public List<AdminProjectMemberResponse> toResponses(
			List<AdminProjectMemberService.ProjectMemberSummary> summaries) {
		return summaries.stream().map(this::toResponse).toList();
	}

	public AdminProjectMemberResponse toResponse(AdminProjectMemberService.ProjectMemberSummary summary) {
		return new AdminProjectMemberResponse(
				summary.membershipId(),
				summary.userId(),
				summary.fullName(),
				summary.email(),
				summary.projectRole().name(),
				summary.memberStatus().name(),
				summary.joinedAt(),
				summary.leftAt());
	}

	private static String normalize(String value) {
		return value.trim()
				.toLowerCase(Locale.ROOT)
				.replace('_', ' ')
				.replace('-', ' ')
				.replaceAll("\\s+", " ");
	}
}

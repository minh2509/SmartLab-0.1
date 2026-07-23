package com.smartlab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.smartlab.dto.response.admin.AdminProjectResponse;
import com.smartlab.enums.ProjectStatus;
import com.smartlab.enums.ProjectType;
import com.smartlab.enums.ProjectVisibility;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.service.admin.AdminProjectService;

class AdminProjectApiMapperTests {

	private final AdminProjectApiMapper mapper = new AdminProjectApiMapper();

	@Test
	void toFilterAcceptsFrontendProjectValues() {
		AdminProjectService.ProjectFilter filter = mapper.toFilter("Planning", "Research", "internal", "ai");

		assertEquals(Set.of(ProjectStatus.PROPOSED, ProjectStatus.PREPARING), filter.statuses());
		assertEquals(Set.of(ProjectType.RESEARCH), filter.types());
		assertEquals(
				Set.of(ProjectVisibility.LAB_INTERNAL, ProjectVisibility.PROJECT_INTERNAL, ProjectVisibility.PRIVATE),
				filter.visibilities());
		assertEquals("AI", filter.researchFieldCode());
	}

	@Test
	void toFilterUsesAllDomainValuesWhenFiltersAreOmitted() {
		AdminProjectService.ProjectFilter filter = mapper.toFilter(null, " ", null, null);

		assertEquals(Set.of(ProjectStatus.values()), filter.statuses());
		assertEquals(Set.of(ProjectType.values()), filter.types());
		assertEquals(Set.of(ProjectVisibility.values()), filter.visibilities());
		assertEquals(null, filter.researchFieldCode());
	}

	@Test
	void toFilterRejectsValuesNotSupportedByFrontendContract() {
		assertThrows(InvalidAdminServiceInputException.class, () -> mapper.toFilter("CLOSED", null, null, null));
		assertThrows(InvalidAdminServiceInputException.class, () -> mapper.toFilter(null, "OTHER", null, null));
		assertThrows(InvalidAdminServiceInputException.class, () -> mapper.toFilter(null, null, "PRIVATE", null));
		assertThrows(InvalidAdminServiceInputException.class, () -> mapper.toFilter(null, null, null, "medicine"));
	}

	@Test
	void toResponseUsesFrontendProjectShapeAndLabels() {
		UUID projectId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		AdminProjectService.ProjectSummary summary = new AdminProjectService.ProjectSummary(
				projectId,
				"atlas-perception",
				"NL-24-07",
				"Atlas",
				"Description",
				"Objective",
				ProjectType.RESEARCH,
				List.of("AI", "ROBOTICS"),
				List.of(leaderId),
				List.of(memberId),
				LocalDate.of(2024, 3, 4),
				LocalDate.of(2026, 12, 15),
				ProjectStatus.IN_PROGRESS,
				62,
				ProjectVisibility.PUBLIC,
				null,
				null,
				null);

		AdminProjectResponse response = mapper.toResponse(summary);

		assertEquals(projectId, response.id());
		assertEquals("Research", response.type());
		assertEquals(List.of("ai", "robotics"), response.fields());
		assertEquals(List.of(leaderId), response.leaderIds());
		assertEquals(List.of(memberId), response.memberIds());
		assertEquals(LocalDate.of(2026, 12, 15), response.expectedEnd());
		assertEquals("Active", response.status());
		assertEquals(62, response.progress());
		assertEquals("public", response.visibility());
	}

	@Test
	void toResponseMapsEveryBackendStatusToExistingFrontendStatus() {
		assertEquals("Planning", mapStatus(ProjectStatus.PROPOSED));
		assertEquals("Planning", mapStatus(ProjectStatus.PREPARING));
		assertEquals("Active", mapStatus(ProjectStatus.IN_PROGRESS));
		assertEquals("On hold", mapStatus(ProjectStatus.PAUSED));
		assertEquals("Publishing", mapStatus(ProjectStatus.COMPLETED));
		assertEquals("Completed", mapStatus(ProjectStatus.CLOSED));
	}

	private String mapStatus(ProjectStatus status) {
		return mapper.toResponse(new AdminProjectService.ProjectSummary(
				UUID.randomUUID(),
				"slug",
				"CODE",
				"Name",
				"Description",
				"Objective",
				ProjectType.RESEARCH,
				List.of(),
				List.of(),
				List.of(),
				null,
				null,
				status,
				0,
				ProjectVisibility.LAB_INTERNAL,
				null,
				null,
				null)).status();
	}
}

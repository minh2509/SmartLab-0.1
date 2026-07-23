package com.smartlab.controller.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smartlab.enums.ProjectStatus;
import com.smartlab.enums.ProjectType;
import com.smartlab.enums.ProjectVisibility;
import com.smartlab.exception.ApiExceptionHandler;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.mapper.AdminProjectApiMapper;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminProjectService;

class AdminProjectControllerTests {

	private final AdminProjectService adminProjectService = mock(AdminProjectService.class);
	private final AuthenticatedActorResolver actorResolver = mock(AuthenticatedActorResolver.class);
	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new AdminProjectController(
					adminProjectService,
					new AdminProjectApiMapper(),
					actorResolver))
			.setControllerAdvice(new ApiExceptionHandler())
			.setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
			.build();

	@Test
	void getProjectsAcceptsFrontendFiltersAndReturnsFrontendProjectShape() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		AdminProjectService.ProjectSummary summary = projectSummary(projectId, leaderId, memberId);
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminProjectService.getProjects(any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(1, 2), 5));

		mockMvc.perform(get("/api/admin/projects")
						.param("status", "Active")
						.param("type", "Research")
						.param("visibility", "public")
						.param("field", "ai")
						.param("page", "1")
						.param("size", "2")
						.param("sort", "progress,desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].id").value(projectId.toString()))
				.andExpect(jsonPath("$.items[0].type").value("Research"))
				.andExpect(jsonPath("$.items[0].fields[0]").value("ai"))
				.andExpect(jsonPath("$.items[0].fields[1]").value("robotics"))
				.andExpect(jsonPath("$.items[0].leaderIds[0]").value(leaderId.toString()))
				.andExpect(jsonPath("$.items[0].memberIds[0]").value(memberId.toString()))
				.andExpect(jsonPath("$.items[0].expectedEnd").value("2026-12-15"))
				.andExpect(jsonPath("$.items[0].status").value("Active"))
				.andExpect(jsonPath("$.items[0].progress").value(62))
				.andExpect(jsonPath("$.items[0].visibility").value("public"))
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalElements").value(5))
				.andExpect(jsonPath("$.totalPages").value(3));

		ArgumentCaptor<AdminProjectService.ProjectFilter> filterCaptor =
				ArgumentCaptor.forClass(AdminProjectService.ProjectFilter.class);
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(adminProjectService).getProjects(any(), filterCaptor.capture(), pageableCaptor.capture());
		assertEquals(List.of(ProjectStatus.IN_PROGRESS), filterCaptor.getValue().statuses().stream().toList());
		assertEquals("AI", filterCaptor.getValue().researchFieldCode());
		assertEquals(1, pageableCaptor.getValue().getPageNumber());
		assertEquals(2, pageableCaptor.getValue().getPageSize());
	}

	@Test
	void getProjectDetailReturnsNotFoundWithoutLeakingCrossLabInformation() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminProjectService.getProjectDetail(actorUserId, projectId))
				.thenThrow(new ResourceNotFoundException("Project was not found."));

		mockMvc.perform(get("/api/admin/projects/{projectId}", projectId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Project was not found."));
	}

	@Test
	void getProjectsRejectsUnsupportedFrontendFilterBeforeCallingService() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());

		mockMvc.perform(get("/api/admin/projects").param("status", "CLOSED"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Unsupported project status filter."));

		verify(adminProjectService, never()).getProjects(any(), any(), any());
	}

	@Test
	void getProjectDetailRejectsMalformedUuid() throws Exception {
		mockMvc.perform(get("/api/admin/projects/not-a-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed."));

		verify(adminProjectService, never()).getProjectDetail(any(), any());
	}

	private static AdminProjectService.ProjectSummary projectSummary(
			UUID projectId,
			UUID leaderId,
			UUID memberId) {
		return new AdminProjectService.ProjectSummary(
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
	}
}

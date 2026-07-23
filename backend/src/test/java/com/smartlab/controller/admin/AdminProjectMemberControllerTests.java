package com.smartlab.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smartlab.enums.ProjectMemberRole;
import com.smartlab.enums.ProjectMemberStatus;
import com.smartlab.exception.ApiExceptionHandler;
import com.smartlab.mapper.AdminProjectMemberApiMapper;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminProjectMemberService;

class AdminProjectMemberControllerTests {

	private final AdminProjectMemberService service = mock(AdminProjectMemberService.class);
	private final AuthenticatedActorResolver actorResolver = mock(AuthenticatedActorResolver.class);
	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new AdminProjectMemberController(
					service,
					new AdminProjectMemberApiMapper(),
					actorResolver))
			.setControllerAdvice(new ApiExceptionHandler())
			.build();

	@Test
	void exposesListAddRoleAndRemoveContracts() throws Exception {
		UUID actorId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		AdminProjectMemberService.ProjectMemberSummary summary = new AdminProjectMemberService.ProjectMemberSummary(
				UUID.randomUUID(), userId, "Linh Tran", "linh@smart.lab",
				ProjectMemberRole.PROJECT_MEMBER, ProjectMemberStatus.ACTIVE,
				OffsetDateTime.parse("2026-07-23T10:00:00Z"), null);
		when(actorResolver.requireActorUserId()).thenReturn(actorId);
		when(service.getProjectMembers(any(), any(), any(), any())).thenReturn(List.of(summary));
		when(service.addMember(any(), any(), any(), any())).thenReturn(summary);
		when(service.updateProjectRole(any(), any(), any(), any())).thenReturn(summary);
		when(service.removeMember(any(), any(), any())).thenReturn(summary);

		mockMvc.perform(get("/api/admin/projects/{projectId}/members", projectId)
					.param("projectRole", "member").param("memberStatus", "active"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].userId").value(userId.toString()))
				.andExpect(jsonPath("$[0].fullName").value("Linh Tran"))
				.andExpect(jsonPath("$[0].projectRole").value("PROJECT_MEMBER"));
		mockMvc.perform(post("/api/admin/projects/{projectId}/members", projectId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"userId\":\"%s\",\"role\":\"PROJECT_MEMBER\"}".formatted(userId)))
				.andExpect(status().isCreated());
		mockMvc.perform(patch("/api/admin/projects/{projectId}/members/{userId}/role", projectId, userId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"role\":\"PROJECT_LEADER\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(patch("/api/admin/projects/{projectId}/members/{userId}/remove", projectId, userId))
				.andExpect(status().isOk());
	}

	@Test
	void rejectsMalformedFiltersAndRequestsBeforeService() throws Exception {
		UUID projectId = UUID.randomUUID();
		mockMvc.perform(get("/api/admin/projects/{projectId}/members", projectId)
					.param("projectRole", "OWNER"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/admin/projects/{projectId}/members", projectId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"role\":\"PROJECT_MEMBER\"}"))
				.andExpect(status().isBadRequest());
		verify(service, never()).addMember(any(), any(), any(), any());
	}
}

package com.smartlab.controller.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smartlab.enums.UserRoleStatus;
import com.smartlab.exception.ApiExceptionHandler;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.mapper.AdminUserApiMapper;
import com.smartlab.service.admin.AdminUserRoleService;

class AdminUserRoleControllerTests {

	private final AdminUserRoleService adminUserRoleService = mock(AdminUserRoleService.class);
	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new AdminUserRoleController(adminUserRoleService, new AdminUserApiMapper()))
			.setControllerAdvice(new ApiExceptionHandler())
			.build();

	@Test
	void listActiveRolesForUserReturnsOnlyServiceProvidedActiveRoles() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID roleId = UUID.randomUUID();
		when(adminUserRoleService.listActiveRolesForUser(userId))
				.thenReturn(List.of(roleSummary(UUID.randomUUID(), roleId, "MEMBER", UserRoleStatus.ACTIVE)));

		mockMvc.perform(get("/api/admin/users/{userId}/roles", userId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].roleId").value(roleId.toString()))
				.andExpect(jsonPath("$[0].code").value("MEMBER"))
				.andExpect(jsonPath("$[0].status").value("ACTIVE"));
	}

	@Test
	void assignRoleUsesPostCollectionContractAndKeepsAssignedByNullUntilAuthenticationExists() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID roleId = UUID.randomUUID();
		when(adminUserRoleService.assignRoleToUser(any(AdminUserRoleService.AssignUserRoleCommand.class)))
				.thenReturn(roleSummary(UUID.randomUUID(), roleId, "ADMIN", UserRoleStatus.ACTIVE));

		mockMvc.perform(post("/api/admin/users/{userId}/roles", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"roleId\":\"" + roleId + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roleCode").value("ADMIN"))
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		ArgumentCaptor<AdminUserRoleService.AssignUserRoleCommand> captor =
				ArgumentCaptor.forClass(AdminUserRoleService.AssignUserRoleCommand.class);
		verify(adminUserRoleService).assignRoleToUser(captor.capture());
		assertEquals(userId, captor.getValue().userId());
		assertEquals(roleId, captor.getValue().roleId());
		assertNull(captor.getValue().assignedByUserId());
	}

	@Test
	void assignRoleKeepsLegacyPutCompatibility() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID roleId = UUID.randomUUID();
		when(adminUserRoleService.assignRoleToUser(any(AdminUserRoleService.AssignUserRoleCommand.class)))
				.thenReturn(roleSummary(UUID.randomUUID(), roleId, "LEADER", UserRoleStatus.ACTIVE));

		mockMvc.perform(put("/api/admin/users/{userId}/roles/{roleId}", userId, roleId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roleCode").value("LEADER"))
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		ArgumentCaptor<AdminUserRoleService.AssignUserRoleCommand> captor =
				ArgumentCaptor.forClass(AdminUserRoleService.AssignUserRoleCommand.class);
		verify(adminUserRoleService).assignRoleToUser(captor.capture());
		assertEquals(userId, captor.getValue().userId());
		assertEquals(roleId, captor.getValue().roleId());
		assertNull(captor.getValue().assignedByUserId());
	}

	@Test
	void revokeRoleUsesLifecycleServiceAndReturnsNoContent() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID roleId = UUID.randomUUID();

		mockMvc.perform(delete("/api/admin/users/{userId}/roles/{roleId}", userId, roleId))
				.andExpect(status().isNoContent());

		ArgumentCaptor<AdminUserRoleService.RevokeUserRoleCommand> captor =
				ArgumentCaptor.forClass(AdminUserRoleService.RevokeUserRoleCommand.class);
		verify(adminUserRoleService).revokeRoleFromUser(captor.capture());
		assertEquals(userId, captor.getValue().userId());
		assertEquals(roleId, captor.getValue().roleId());
	}

	@Test
	void missingAssignmentUsesGlobalExceptionHandlerNotFoundMapping() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID roleId = UUID.randomUUID();
		org.mockito.Mockito.doThrow(new ResourceNotFoundException("User role assignment was not found."))
				.when(adminUserRoleService)
				.revokeRoleFromUser(any(AdminUserRoleService.RevokeUserRoleCommand.class));

		mockMvc.perform(delete("/api/admin/users/{userId}/roles/{roleId}", userId, roleId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("User role assignment was not found."));
	}

	private static AdminUserRoleService.AssignedRoleSummary roleSummary(
			UUID assignmentId,
			UUID roleId,
			String roleCode,
			UserRoleStatus status) {
		return new AdminUserRoleService.AssignedRoleSummary(assignmentId, roleId, roleCode, roleCode, status);
	}
}

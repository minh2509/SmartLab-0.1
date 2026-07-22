package com.smartlab.controller.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;
import com.smartlab.exception.ApiExceptionHandler;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.mapper.AdminUserApiMapper;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminUserRoleService;
import com.smartlab.service.admin.AdminUserService;

class AdminUserRoleControllerTests {

	private final AdminUserRoleService adminUserRoleService = mock(AdminUserRoleService.class);
	private final AuthenticatedActorResolver actorResolver = mock(AuthenticatedActorResolver.class);
	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new AdminUserRoleController(adminUserRoleService, new AdminUserApiMapper(), actorResolver))
			.setControllerAdvice(new ApiExceptionHandler())
			.setValidator(validator())
			.build();

	@Test
	void listActiveRolesForUserUsesActorAwareService() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID roleId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminUserRoleService.listActiveRolesForUser(actorUserId, userId))
				.thenReturn(List.of(roleSummary(UUID.randomUUID(), roleId, "MEMBER", UserRoleStatus.ACTIVE)));

		mockMvc.perform(get("/api/admin/users/{userId}/roles", userId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].roleId").value(roleId.toString()))
				.andExpect(jsonPath("$[0].code").value("MEMBER"))
				.andExpect(jsonPath("$[0].status").value("ACTIVE"));
	}

	@Test
	void replaceRolesUsesPutCollectionEndpointAndReturnsUpdatedUserResponse() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID labId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminUserRoleService.replaceRolesForUser(any(AdminUserRoleService.ReplaceUserRolesCommand.class)))
				.thenReturn(new AdminUserService.ManagedUserSummary(
						userId,
						labId,
						"member",
						"member@example.edu",
						"Member User",
						null,
						UserAccountStatus.ACTIVE,
						List.of("LEADER", "MEMBER")));

		mockMvc.perform(put("/api/admin/users/{userId}/roles", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"roleCodes":[" member ","LEADER"]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roleCodes[0]").value("LEADER"))
				.andExpect(jsonPath("$.roleCodes[1]").value("MEMBER"));

		ArgumentCaptor<AdminUserRoleService.ReplaceUserRolesCommand> captor =
				ArgumentCaptor.forClass(AdminUserRoleService.ReplaceUserRolesCommand.class);
		verify(adminUserRoleService).replaceRolesForUser(captor.capture());
		assertEquals(actorUserId, captor.getValue().actorUserId());
		assertEquals(userId, captor.getValue().userId());
		assertEquals(List.of(" member ", "LEADER"), captor.getValue().roleCodes());
	}

	@Test
	void assignRoleUsesActorIdentityAndRecordsAssignerInService() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID roleId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminUserRoleService.assignRoleToUser(any(AdminUserRoleService.AssignUserRoleCommand.class)))
				.thenReturn(roleSummary(UUID.randomUUID(), roleId, "ADMIN", UserRoleStatus.ACTIVE));

		mockMvc.perform(put("/api/admin/users/{userId}/roles/{roleId}", userId, roleId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roleCode").value("ADMIN"))
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		ArgumentCaptor<AdminUserRoleService.AssignUserRoleCommand> captor =
				ArgumentCaptor.forClass(AdminUserRoleService.AssignUserRoleCommand.class);
		verify(adminUserRoleService).assignRoleToUser(captor.capture());
		assertEquals(actorUserId, captor.getValue().actorUserId());
		assertEquals(userId, captor.getValue().userId());
		assertEquals(roleId, captor.getValue().roleId());
	}

	@Test
	void revokeRoleUsesLifecycleServiceAndReturnsNoContent() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID roleId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);

		mockMvc.perform(delete("/api/admin/users/{userId}/roles/{roleId}", userId, roleId))
				.andExpect(status().isNoContent());

		ArgumentCaptor<AdminUserRoleService.RevokeUserRoleCommand> captor =
				ArgumentCaptor.forClass(AdminUserRoleService.RevokeUserRoleCommand.class);
		verify(adminUserRoleService).revokeRoleFromUser(captor.capture());
		assertEquals(actorUserId, captor.getValue().actorUserId());
		assertEquals(userId, captor.getValue().userId());
		assertEquals(roleId, captor.getValue().roleId());
	}

	@Test
	void missingAssignmentUsesGlobalExceptionHandlerNotFoundMapping() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID roleId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());
		org.mockito.Mockito.doThrow(new ResourceNotFoundException("User role assignment was not found."))
				.when(adminUserRoleService)
				.revokeRoleFromUser(any(AdminUserRoleService.RevokeUserRoleCommand.class));

		mockMvc.perform(delete("/api/admin/users/{userId}/roles/{roleId}", userId, roleId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("User role assignment was not found."));
	}

	private static LocalValidatorFactoryBean validator() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		return validator;
	}

	private static AdminUserRoleService.AssignedRoleSummary roleSummary(
			UUID assignmentId,
			UUID roleId,
			String roleCode,
			UserRoleStatus status) {
		return new AdminUserRoleService.AssignedRoleSummary(assignmentId, roleId, roleCode, roleCode, status);
	}
}

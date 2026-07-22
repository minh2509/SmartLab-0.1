package com.smartlab.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smartlab.exception.ApiExceptionHandler;
import com.smartlab.mapper.AdminUserApiMapper;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminUserRoleService;

class AdminRoleControllerTests {

	private final AdminUserRoleService adminUserRoleService = mock(AdminUserRoleService.class);
	private final AuthenticatedActorResolver actorResolver = mock(AuthenticatedActorResolver.class);
	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new AdminRoleController(adminUserRoleService, new AdminUserApiMapper(), actorResolver))
			.setControllerAdvice(new ApiExceptionHandler())
			.build();

	@Test
	void listRolesReturnsCatalogResponsesWithAssignableFlags() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID adminRoleId = UUID.randomUUID();
		UUID superAdminRoleId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminUserRoleService.listRoleCatalog(actorUserId))
				.thenReturn(List.of(
						new AdminUserRoleService.RoleCatalogSummary(adminRoleId, "ADMIN", "Admin", true),
						new AdminUserRoleService.RoleCatalogSummary(superAdminRoleId, "SUPER_ADMIN", "Super Admin", false)));

		mockMvc.perform(get("/api/admin/roles"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].roleId").value(adminRoleId.toString()))
				.andExpect(jsonPath("$[0].code").value("ADMIN"))
				.andExpect(jsonPath("$[0].assignable").value(true))
				.andExpect(jsonPath("$[1].code").value("SUPER_ADMIN"))
				.andExpect(jsonPath("$[1].assignable").value(false));

		verify(adminUserRoleService).listRoleCatalog(actorUserId);
	}
}

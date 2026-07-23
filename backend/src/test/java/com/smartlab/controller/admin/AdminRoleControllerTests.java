package com.smartlab.controller.admin;

import static org.mockito.Mockito.mock;
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
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.mapper.AdminRoleCatalogApiMapper;
import com.smartlab.service.admin.AdminRoleCatalogService;

class AdminRoleControllerTests {

	private final AdminRoleCatalogService adminRoleCatalogService = mock(AdminRoleCatalogService.class);
	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new AdminRoleController(adminRoleCatalogService, new AdminRoleCatalogApiMapper()))
			.setControllerAdvice(new ApiExceptionHandler())
			.build();

	@Test
	void listRolesReturnsSystemRoleCatalog() throws Exception {
		UUID adminId = UUID.randomUUID();
		when(adminRoleCatalogService.listRoles())
				.thenReturn(List.of(new AdminRoleCatalogService.RoleSummary(
						adminId,
						"ADMIN",
						"Admin",
						"Lab administrator role")));

		mockMvc.perform(get("/api/admin/roles"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(adminId.toString()))
				.andExpect(jsonPath("$[0].code").value("ADMIN"))
				.andExpect(jsonPath("$[0].name").value("Admin"))
				.andExpect(jsonPath("$[0].description").value("Lab administrator role"));
	}

	@Test
	void listPermissionsForRoleReturnsPermissionCatalog() throws Exception {
		UUID roleId = UUID.randomUUID();
		UUID permissionId = UUID.randomUUID();
		when(adminRoleCatalogService.listPermissionsForRole(roleId))
				.thenReturn(List.of(new AdminRoleCatalogService.PermissionSummary(
						permissionId,
						"USER_MANAGE",
						"Manage users",
						"ADMIN",
						"Create, update, lock, unlock, reset, and soft-delete user accounts")));

		mockMvc.perform(get("/api/admin/roles/{roleId}/permissions", roleId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(permissionId.toString()))
				.andExpect(jsonPath("$[0].code").value("USER_MANAGE"))
				.andExpect(jsonPath("$[0].module").value("ADMIN"));
	}

	@Test
	void missingRoleUsesGlobalExceptionHandlerNotFoundMapping() throws Exception {
		UUID roleId = UUID.randomUUID();
		when(adminRoleCatalogService.listPermissionsForRole(roleId))
				.thenThrow(new ResourceNotFoundException("Role was not found."));

		mockMvc.perform(get("/api/admin/roles/{roleId}/permissions", roleId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Role was not found."));
	}
}

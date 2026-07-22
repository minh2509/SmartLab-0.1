package com.smartlab.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.smartlab.entity.Permission;
import com.smartlab.entity.Role;
import com.smartlab.entity.RolePermission;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.RolePermissionRepository;
import com.smartlab.repository.RoleRepository;

class AdminRoleCatalogServiceTests {

	private final RoleRepository roleRepository = mock(RoleRepository.class);
	private final RolePermissionRepository rolePermissionRepository = mock(RolePermissionRepository.class);
	private final AdminRoleCatalogService service =
			new AdminRoleCatalogService(roleRepository, rolePermissionRepository);

	@Test
	void listRolesReturnsStableCodeSortedCatalog() {
		Role member = role("MEMBER", "Member");
		Role admin = role("ADMIN", "Admin");
		when(roleRepository.findAll()).thenReturn(List.of(member, admin));

		List<AdminRoleCatalogService.RoleSummary> roles = service.listRoles();

		assertEquals(List.of("ADMIN", "MEMBER"), roles.stream().map(AdminRoleCatalogService.RoleSummary::code).toList());
		assertEquals("Admin", roles.get(0).name());
	}

	@Test
	void listPermissionsForRoleReturnsStableModuleThenCodeSortedCatalog() {
		Role role = role("LEADER", "Leader");
		Permission task = permission("TASK_MANAGE_ASSIGNED_PROJECT", "TASK");
		Permission post = permission("POST_CREATE", "CONTENT");
		when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
		when(rolePermissionRepository.findByRole(role))
				.thenReturn(List.of(rolePermission(role, task), rolePermission(role, post)));

		List<AdminRoleCatalogService.PermissionSummary> permissions = service.listPermissionsForRole(role.getId());

		assertEquals(
				List.of("POST_CREATE", "TASK_MANAGE_ASSIGNED_PROJECT"),
				permissions.stream().map(AdminRoleCatalogService.PermissionSummary::code).toList());
	}

	@Test
	void listPermissionsForRoleRejectsMissingRoleIds() {
		assertThrows(InvalidAdminServiceInputException.class, () -> service.listPermissionsForRole(null));
		assertThrows(ResourceNotFoundException.class, () -> service.listPermissionsForRole(UUID.randomUUID()));
	}

	private static Role role(String code, String name) {
		Role role = new Role();
		role.setId(UUID.randomUUID());
		role.setCode(code);
		role.setName(name);
		role.setDescription(name + " description");
		return role;
	}

	private static Permission permission(String code, String module) {
		Permission permission = new Permission();
		permission.setId(UUID.randomUUID());
		permission.setCode(code);
		permission.setName(code);
		permission.setModule(module);
		permission.setDescription(code + " description");
		return permission;
	}

	private static RolePermission rolePermission(Role role, Permission permission) {
		RolePermission rolePermission = new RolePermission();
		rolePermission.setId(UUID.randomUUID());
		rolePermission.setRole(role);
		rolePermission.setPermission(permission);
		return rolePermission;
	}
}

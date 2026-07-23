package com.smartlab.service.admin;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.Permission;
import com.smartlab.entity.Role;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.RolePermissionRepository;
import com.smartlab.repository.RoleRepository;

@Service
@Profile("!nodb")
public class AdminRoleCatalogService {

	private final RoleRepository roleRepository;
	private final RolePermissionRepository rolePermissionRepository;

	public AdminRoleCatalogService(
			RoleRepository roleRepository,
			RolePermissionRepository rolePermissionRepository) {
		this.roleRepository = roleRepository;
		this.rolePermissionRepository = rolePermissionRepository;
	}

	@Transactional(readOnly = true)
	public List<RoleSummary> listRoles() {
		return roleRepository.findAll()
				.stream()
				.sorted(Comparator.comparing(Role::getCode))
				.map(RoleSummary::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<PermissionSummary> listPermissionsForRole(UUID roleId) {
		Role role = findRole(roleId);
		return rolePermissionRepository.findByRole(role)
				.stream()
				.map(rolePermission -> rolePermission.getPermission())
				.sorted(Comparator.comparing(Permission::getModule).thenComparing(Permission::getCode))
				.map(PermissionSummary::from)
				.toList();
	}

	private Role findRole(UUID roleId) {
		if (roleId == null) {
			throw new InvalidAdminServiceInputException("Role ID must not be null.");
		}
		return roleRepository.findById(roleId)
				.orElseThrow(() -> new ResourceNotFoundException("Role was not found."));
	}

	public record RoleSummary(
			UUID id,
			String code,
			String name,
			String description) {

		static RoleSummary from(Role role) {
			return new RoleSummary(role.getId(), role.getCode(), role.getName(), role.getDescription());
		}
	}

	public record PermissionSummary(
			UUID id,
			String code,
			String name,
			String module,
			String description) {

		static PermissionSummary from(Permission permission) {
			return new PermissionSummary(
					permission.getId(),
					permission.getCode(),
					permission.getName(),
					permission.getModule(),
					permission.getDescription());
		}
	}
}

package com.smartlab.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Permission;
import com.smartlab.entity.Role;
import com.smartlab.entity.RolePermission;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

	boolean existsByRoleAndPermission(Role role, Permission permission);

	List<RolePermission> findByRole(Role role);

	List<RolePermission> findByRoleIn(Collection<Role> roles);
}

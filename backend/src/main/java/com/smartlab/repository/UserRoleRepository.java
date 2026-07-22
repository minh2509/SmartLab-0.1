package com.smartlab.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Role;
import com.smartlab.entity.User;
import com.smartlab.entity.UserRole;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

	boolean existsByUserAndRole(User user, Role role);

	Optional<UserRole> findByUserAndRole(User user, Role role);

	boolean existsByUserAndRoleAndStatus(User user, Role role, UserRoleStatus status);

	List<UserRole> findByUserAndStatus(User user, UserRoleStatus status);

	@EntityGraph(attributePaths = {"user", "role"})
	List<UserRole> findByUserInAndStatus(Collection<User> users, UserRoleStatus status);

	List<UserRole> findByRoleAndStatus(Role role, UserRoleStatus status);

	long countByRoleAndStatusAndUserAccountStatus(
			Role role,
			UserRoleStatus status,
			UserAccountStatus accountStatus);
}

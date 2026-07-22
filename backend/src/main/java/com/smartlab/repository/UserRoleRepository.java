package com.smartlab.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Role;
import com.smartlab.entity.User;
import com.smartlab.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

	boolean existsByUserAndRole(User user, Role role);
}

package com.smartlab.service.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.Role;
import com.smartlab.entity.User;
import com.smartlab.entity.UserRole;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;
import com.smartlab.exception.DuplicateActiveRoleAssignmentException;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ProtectedAdministratorOperationException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.RoleRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.UserRoleRepository;

@Service
@Profile("!nodb")
public class AdminUserRoleService {

	public static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;

	public AdminUserRoleService(
			UserRepository userRepository,
			RoleRepository roleRepository,
			UserRoleRepository userRoleRepository) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userRoleRepository = userRoleRepository;
	}

	@Transactional
	public AssignedRoleSummary assignRoleToUser(AssignUserRoleCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Assign user role command must not be null.");
		}

		User user = findUser(command.userId());
		Role role = findRole(command.roleId());
		User assignedBy = command.assignedByUserId() == null ? null : findUser(command.assignedByUserId());
		return userRoleRepository.findByUserAndRole(user, role)
				.map(existing -> reactivateExistingAssignment(existing, assignedBy))
				.orElseGet(() -> createAssignment(user, role, assignedBy));
	}

	@Transactional
	public void revokeRoleFromUser(RevokeUserRoleCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Revoke user role command must not be null.");
		}

		User user = findUser(command.userId());
		Role role = findRole(command.roleId());
		UserRole assignment = userRoleRepository.findByUserAndRole(user, role)
				.orElseThrow(() -> new ResourceNotFoundException("User role assignment was not found."));
		if (assignment.getStatus() == UserRoleStatus.INACTIVE) {
			return;
		}
		protectFinalActiveSuperAdmin(role);
		assignment.setStatus(UserRoleStatus.INACTIVE);
	}

	@Transactional(readOnly = true)
	public List<AssignedRoleSummary> listActiveRolesForUser(UUID userId) {
		User user = findUser(userId);
		return userRoleRepository.findByUserAndStatus(user, UserRoleStatus.ACTIVE)
				.stream()
				.map(AssignedRoleSummary::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AssignedUserSummary> listActiveUsersForRole(UUID roleId) {
		Role role = findRole(roleId);
		return userRoleRepository.findByRoleAndStatus(role, UserRoleStatus.ACTIVE)
				.stream()
				.map(AssignedUserSummary::from)
				.toList();
	}

	private AssignedRoleSummary reactivateExistingAssignment(UserRole assignment, User assignedBy) {
		if (assignment.getStatus() == UserRoleStatus.ACTIVE) {
			throw new DuplicateActiveRoleAssignmentException("User already has an active assignment for this role.");
		}
		assignment.setStatus(UserRoleStatus.ACTIVE);
		assignment.setAssignedBy(assignedBy);
		return AssignedRoleSummary.from(assignment);
	}

	private AssignedRoleSummary createAssignment(User user, Role role, User assignedBy) {
		UserRole assignment = new UserRole();
		assignment.setUser(user);
		assignment.setRole(role);
		assignment.setAssignedBy(assignedBy);
		assignment.setStatus(UserRoleStatus.ACTIVE);
		return AssignedRoleSummary.from(userRoleRepository.save(assignment));
	}

	private void protectFinalActiveSuperAdmin(Role role) {
		if (!SUPER_ADMIN_ROLE_CODE.equals(role.getCode())) {
			return;
		}
		long activeSuperAdmins = userRoleRepository.countByRoleAndStatusAndUserAccountStatus(
				role,
				UserRoleStatus.ACTIVE,
				UserAccountStatus.ACTIVE);
		if (activeSuperAdmins <= 1) {
			throw new ProtectedAdministratorOperationException("Cannot revoke the final active SUPER_ADMIN assignment.");
		}
	}

	private User findUser(UUID userId) {
		if (userId == null) {
			throw new InvalidAdminServiceInputException("User ID must not be null.");
		}
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User was not found."));
	}

	private Role findRole(UUID roleId) {
		if (roleId == null) {
			throw new InvalidAdminServiceInputException("Role ID must not be null.");
		}
		return roleRepository.findById(roleId)
				.orElseThrow(() -> new ResourceNotFoundException("Role was not found."));
	}

	public record AssignUserRoleCommand(UUID userId, UUID roleId, UUID assignedByUserId) {
	}

	public record RevokeUserRoleCommand(UUID userId, UUID roleId) {
	}

	public record AssignedRoleSummary(
			UUID assignmentId,
			UUID roleId,
			String roleCode,
			String roleName,
			UserRoleStatus status) {

		static AssignedRoleSummary from(UserRole userRole) {
			Role role = userRole.getRole();
			return new AssignedRoleSummary(
					userRole.getId(),
					role.getId(),
					role.getCode(),
					role.getName(),
					userRole.getStatus());
		}
	}

	public record AssignedUserSummary(
			UUID assignmentId,
			UUID userId,
			UUID labId,
			String username,
			String email,
			String fullName) {

		static AssignedUserSummary from(UserRole userRole) {
			User user = userRole.getUser();
			UUID labId = user.getLab() == null ? null : user.getLab().getId();
			return new AssignedUserSummary(
					userRole.getId(),
					user.getId(),
					labId,
					user.getUsername(),
					user.getEmail(),
					user.getFullName());
		}
	}
}

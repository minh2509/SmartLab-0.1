package com.smartlab.service.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.Role;
import com.smartlab.entity.User;
import com.smartlab.entity.UserRole;
import com.smartlab.enums.UserRoleStatus;
import com.smartlab.exception.ConflictingAdminOperationException;
import com.smartlab.exception.DuplicateActiveRoleAssignmentException;
import com.smartlab.exception.ForbiddenAdminOperationException;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.RoleRepository;
import com.smartlab.repository.UserRoleRepository;

@Service
@Profile("!nodb")
public class AdminUserRoleService {

	public static final String SUPER_ADMIN_ROLE_CODE = AdminRolePolicy.SUPER_ADMIN_ROLE_CODE;
	public static final String ADMIN_ROLE_CODE = AdminRolePolicy.ADMIN_ROLE_CODE;
	public static final String LEADER_ROLE_CODE = AdminRolePolicy.LEADER_ROLE_CODE;
	public static final String MEMBER_ROLE_CODE = AdminRolePolicy.MEMBER_ROLE_CODE;

	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final AdminRolePolicy rolePolicy;

	public AdminUserRoleService(
			RoleRepository roleRepository,
			UserRoleRepository userRoleRepository,
			AdminRolePolicy rolePolicy) {
		this.roleRepository = roleRepository;
		this.userRoleRepository = userRoleRepository;
		this.rolePolicy = rolePolicy;
	}

	@Transactional
	public AssignedRoleSummary assignRoleToUser(AssignUserRoleCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Assign user role command must not be null.");
		}

		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		User user = rolePolicy.findUserInActorLab(actor, command.userId());
		rolePolicy.assertCanMutateTarget(actor, user);
		Role role = findRole(command.roleId());
		List<String> roleCodes = List.of(role.getCode());
		rolePolicy.assertAssignableRoleCodes(actor, roleCodes);
		return userRoleRepository.findByUserAndRole(user, role)
				.map(existing -> reactivateExistingAssignment(existing, actor.actor()))
				.orElseGet(() -> createAssignment(user, role, actor.actor()));
	}

	@Transactional
	public void revokeRoleFromUser(RevokeUserRoleCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Revoke user role command must not be null.");
		}

		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		User user = rolePolicy.findUserInActorLab(actor, command.userId());
		rolePolicy.assertCanMutateTarget(actor, user);
		Role role = findRole(command.roleId());
		if (SUPER_ADMIN_ROLE_CODE.equals(role.getCode())) {
			throw new ForbiddenAdminOperationException("SUPER_ADMIN cannot be revoked through this API.");
		}
		UserRole assignment = userRoleRepository.findByUserAndRole(user, role)
				.orElseThrow(() -> new ResourceNotFoundException("User role assignment was not found."));
		if (assignment.getStatus() == UserRoleStatus.INACTIVE) {
			return;
		}
		List<UserRole> activeAssignments = userRoleRepository.findByUserAndStatus(user, UserRoleStatus.ACTIVE);
		if (activeAssignments.size() <= 1) {
			throw new ConflictingAdminOperationException("A managed user must retain at least one active role.");
		}
		assignment.setStatus(UserRoleStatus.INACTIVE);
	}

	@Transactional
	public AdminUserService.ManagedUserSummary replaceRolesForUser(ReplaceUserRolesCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Replace user roles command must not be null.");
		}

		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		User user = rolePolicy.findUserInActorLab(actor, command.userId());
		rolePolicy.assertCanMutateTarget(actor, user);
		List<String> requestedRoleCodes = rolePolicy.normalizeRoleCodes(command.roleCodes());
		rolePolicy.assertAssignableRoleCodes(actor, requestedRoleCodes);
		List<Role> requestedRoles = rolePolicy.resolveRolesByCodes(requestedRoleCodes);

		List<UserRole> activeAssignments = userRoleRepository.findByUserAndStatus(user, UserRoleStatus.ACTIVE);
		for (UserRole activeAssignment : activeAssignments) {
			String currentCode = activeAssignment.getRole().getCode();
			if (!requestedRoleCodes.contains(currentCode)) {
				activeAssignment.setStatus(UserRoleStatus.INACTIVE);
			}
		}
		for (Role role : requestedRoles) {
			activateRequestedRole(user, role, actor.actor());
		}
		return AdminUserService.ManagedUserSummary.from(user, requestedRoleCodes);
	}

	@Transactional(readOnly = true)
	public List<AssignedRoleSummary> listActiveRolesForUser(UUID actorUserId, UUID userId) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		User user = rolePolicy.findUserInActorLab(actor, userId);
		return userRoleRepository.findByUserAndStatus(user, UserRoleStatus.ACTIVE)
				.stream()
				.map(AssignedRoleSummary::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<RoleCatalogSummary> listRoleCatalog(UUID actorUserId) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		return rolePolicy.sortedSystemRoles()
				.stream()
				.map(role -> new RoleCatalogSummary(
						role.getId(),
						role.getCode(),
						role.getName(),
						rolePolicy.isRoleAssignable(actor, role.getCode())))
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
		UserRole assignment = newAssignment(user, role);
		assignment.setAssignedBy(assignedBy);
		assignment.setStatus(UserRoleStatus.ACTIVE);
		return AssignedRoleSummary.from(userRoleRepository.save(assignment));
	}

	private void activateRequestedRole(User user, Role role, User assignedBy) {
		userRoleRepository.findByUserAndRole(user, role)
				.ifPresentOrElse(
						assignment -> reactivateForReplacement(assignment, assignedBy),
						() -> createReplacementAssignment(user, role, assignedBy));
	}

	private void reactivateForReplacement(UserRole assignment, User assignedBy) {
		if (assignment.getStatus() == UserRoleStatus.ACTIVE) {
			return;
		}
		assignment.setStatus(UserRoleStatus.ACTIVE);
		assignment.setAssignedBy(assignedBy);
	}

	private void createReplacementAssignment(User user, Role role, User assignedBy) {
		UserRole assignment = newAssignment(user, role);
		assignment.setAssignedBy(assignedBy);
		assignment.setStatus(UserRoleStatus.ACTIVE);
		userRoleRepository.save(assignment);
	}

	private static UserRole newAssignment(User user, Role role) {
		UserRole assignment = new UserRole();
		assignment.setUser(user);
		assignment.setRole(role);
		return assignment;
	}

	private Role findRole(UUID roleId) {
		if (roleId == null) {
			throw new InvalidAdminServiceInputException("Role ID must not be null.");
		}
		return roleRepository.findById(roleId)
				.orElseThrow(() -> new ResourceNotFoundException("Role was not found."));
	}

	public record AssignUserRoleCommand(UUID actorUserId, UUID userId, UUID roleId) {
	}

	public record RevokeUserRoleCommand(UUID actorUserId, UUID userId, UUID roleId) {
	}

	public record ReplaceUserRolesCommand(UUID actorUserId, UUID userId, List<String> roleCodes) {
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

	public record RoleCatalogSummary(
			UUID roleId,
			String code,
			String name,
			boolean assignable) {
	}
}

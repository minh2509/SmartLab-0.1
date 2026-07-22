package com.smartlab.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.Lab;
import com.smartlab.entity.Role;
import com.smartlab.entity.RolePermission;
import com.smartlab.entity.User;
import com.smartlab.entity.UserRole;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;
import com.smartlab.exception.DuplicateActiveRoleAssignmentException;
import com.smartlab.exception.ProtectedAdministratorOperationException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.RoleRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.UserRoleRepository;

class AdminUserRoleServiceTests {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final RoleRepository roleRepository = mock(RoleRepository.class);
	private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
	private final AdminUserRoleService service = new AdminUserRoleService(
			userRepository,
			roleRepository,
			userRoleRepository);

	@Test
	void assignRoleCreatesActiveAssignmentForGlobalRoleAndRecordsAssigner() {
		User user = user(UUID.randomUUID(), "member", "member@example.edu");
		User assigner = user(UUID.randomUUID(), "admin", "admin@example.edu");
		Role role = role(UUID.randomUUID(), "MEMBER");
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(userRepository.findById(assigner.getId())).thenReturn(Optional.of(assigner));
		when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
		when(userRoleRepository.findByUserAndRole(user, role)).thenReturn(Optional.empty());
		when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> {
			UserRole userRole = invocation.getArgument(0);
			userRole.setId(UUID.randomUUID());
			return userRole;
		});

		AdminUserRoleService.AssignedRoleSummary summary = service.assignRoleToUser(
				new AdminUserRoleService.AssignUserRoleCommand(user.getId(), role.getId(), assigner.getId()));

		assertEquals("MEMBER", summary.roleCode());
		ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
		verify(userRoleRepository).save(captor.capture());
		assertEquals(user, captor.getValue().getUser());
		assertEquals(role, captor.getValue().getRole());
		assertEquals(assigner, captor.getValue().getAssignedBy());
		assertEquals(UserRoleStatus.ACTIVE, captor.getValue().getStatus());
		assertFalse(Arrays.stream(Role.class.getDeclaredFields()).anyMatch(field -> field.getName().equals("lab")));
		verifyNoRolePermissionChanges();
	}

	@Test
	void assignRoleRejectsMissingUserMissingRoleAndDuplicateActiveAssignment() {
		UUID missingUserId = UUID.randomUUID();
		UUID roleId = UUID.randomUUID();
		assertThrows(
				ResourceNotFoundException.class,
				() -> service.assignRoleToUser(new AdminUserRoleService.AssignUserRoleCommand(missingUserId, roleId, null)));

		User user = user(UUID.randomUUID(), "member", "member@example.edu");
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		assertThrows(
				ResourceNotFoundException.class,
				() -> service.assignRoleToUser(new AdminUserRoleService.AssignUserRoleCommand(user.getId(), roleId, null)));

		Role role = role(roleId, "MEMBER");
		UserRole active = userRole(user, role, UserRoleStatus.ACTIVE);
		when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
		when(userRoleRepository.findByUserAndRole(user, role)).thenReturn(Optional.of(active));
		assertThrows(
				DuplicateActiveRoleAssignmentException.class,
				() -> service.assignRoleToUser(new AdminUserRoleService.AssignUserRoleCommand(user.getId(), roleId, null)));
	}

	@Test
	void assignRoleReactivatesInactiveAssignmentWithoutCreatingDuplicateHistory() {
		User user = user(UUID.randomUUID(), "member", "member@example.edu");
		Role role = role(UUID.randomUUID(), "MEMBER");
		UserRole inactive = userRole(user, role, UserRoleStatus.INACTIVE);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
		when(userRoleRepository.findByUserAndRole(user, role)).thenReturn(Optional.of(inactive));

		AdminUserRoleService.AssignedRoleSummary summary = service.assignRoleToUser(
				new AdminUserRoleService.AssignUserRoleCommand(user.getId(), role.getId(), null));

		assertEquals(UserRoleStatus.ACTIVE, inactive.getStatus());
		assertEquals(role.getId(), summary.roleId());
		verify(userRoleRepository, never()).save(any(UserRole.class));
	}

	@Test
	void revokeRoleDeactivatesActiveAssignmentAndRepeatedRevocationIsNoop() {
		User user = user(UUID.randomUUID(), "member", "member@example.edu");
		Role role = role(UUID.randomUUID(), "MEMBER");
		UserRole active = userRole(user, role, UserRoleStatus.ACTIVE);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
		when(userRoleRepository.findByUserAndRole(user, role)).thenReturn(Optional.of(active));

		service.revokeRoleFromUser(new AdminUserRoleService.RevokeUserRoleCommand(user.getId(), role.getId()));

		assertEquals(UserRoleStatus.INACTIVE, active.getStatus());
		verify(userRoleRepository, never()).delete(any(UserRole.class));
		service.revokeRoleFromUser(new AdminUserRoleService.RevokeUserRoleCommand(user.getId(), role.getId()));
		assertEquals(UserRoleStatus.INACTIVE, active.getStatus());
	}

	@Test
	void revokeRoleRejectsMissingAssignmentAndProtectsFinalActiveSuperAdmin() {
		User user = user(UUID.randomUUID(), "root", "root@example.edu");
		Role member = role(UUID.randomUUID(), "MEMBER");
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(roleRepository.findById(member.getId())).thenReturn(Optional.of(member));
		when(userRoleRepository.findByUserAndRole(user, member)).thenReturn(Optional.empty());
		assertThrows(
				ResourceNotFoundException.class,
				() -> service.revokeRoleFromUser(new AdminUserRoleService.RevokeUserRoleCommand(user.getId(), member.getId())));

		Role superAdmin = role(UUID.randomUUID(), AdminUserRoleService.SUPER_ADMIN_ROLE_CODE);
		UserRole assignment = userRole(user, superAdmin, UserRoleStatus.ACTIVE);
		when(roleRepository.findById(superAdmin.getId())).thenReturn(Optional.of(superAdmin));
		when(userRoleRepository.findByUserAndRole(user, superAdmin)).thenReturn(Optional.of(assignment));
		when(userRoleRepository.countByRoleAndStatusAndUserAccountStatus(
				superAdmin,
				UserRoleStatus.ACTIVE,
				UserAccountStatus.ACTIVE)).thenReturn(1L);

		assertThrows(
				ProtectedAdministratorOperationException.class,
				() -> service.revokeRoleFromUser(
						new AdminUserRoleService.RevokeUserRoleCommand(user.getId(), superAdmin.getId())));
		assertEquals(UserRoleStatus.ACTIVE, assignment.getStatus());
	}

	@Test
	void queriesReturnOnlyActiveRolesAndActiveUsers() {
		User user = user(UUID.randomUUID(), "member", "member@example.edu");
		Role member = role(UUID.randomUUID(), "MEMBER");
		Role admin = role(UUID.randomUUID(), "ADMIN");
		UserRole activeMember = userRole(user, member, UserRoleStatus.ACTIVE);
		UserRole activeAdmin = userRole(user, admin, UserRoleStatus.ACTIVE);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(roleRepository.findById(member.getId())).thenReturn(Optional.of(member));
		when(userRoleRepository.findByUserAndStatus(user, UserRoleStatus.ACTIVE)).thenReturn(List.of(activeMember, activeAdmin));
		when(userRoleRepository.findByRoleAndStatus(member, UserRoleStatus.ACTIVE)).thenReturn(List.of(activeMember));

		assertEquals(2, service.listActiveRolesForUser(user.getId()).size());
		assertEquals(1, service.listActiveUsersForRole(member.getId()).size());
	}

	@Test
	void serviceStructureUsesSpringTransactionsNoPlaintextPasswordAndNoRolePermissionSurface()
			throws NoSuchMethodException {
		assertNotNull(AdminUserRoleService.class.getAnnotation(Service.class));
		assertTransactional("assignRoleToUser", AdminUserRoleService.AssignUserRoleCommand.class, false);
		assertTransactional("revokeRoleFromUser", AdminUserRoleService.RevokeUserRoleCommand.class, false);
		assertTransactional("listActiveRolesForUser", UUID.class, true);
		assertTransactional("listActiveUsersForRole", UUID.class, true);
		assertFalse(Arrays.stream(AdminUserRoleService.class.getDeclaredMethods())
				.anyMatch(method -> method.getReturnType() == RolePermission.class));
		assertFalse(Arrays.stream(AdminUserRoleService.AssignUserRoleCommand.class.getRecordComponents())
				.anyMatch(component -> component.getName().toLowerCase().contains("password")));
	}

	private void verifyNoRolePermissionChanges() {
		assertFalse(Arrays.stream(AdminUserRoleService.class.getDeclaredFields())
				.anyMatch(field -> field.getType().getName().contains("RolePermission")));
	}

	private static void assertTransactional(String methodName, Class<?> argType, boolean readOnly)
			throws NoSuchMethodException {
		Method method = AdminUserRoleService.class.getMethod(methodName, argType);
		Transactional transactional = method.getAnnotation(Transactional.class);
		assertNotNull(transactional);
		assertEquals(readOnly, transactional.readOnly());
	}

	private static User user(UUID id, String username, String email) {
		Lab lab = new Lab();
		lab.setId(UUID.randomUUID());
		User user = new User();
		user.setId(id);
		user.setLab(lab);
		user.setUsername(username);
		user.setEmail(email);
		user.setPasswordHash("hash");
		user.setFullName("Full Name");
		user.setAccountStatus(UserAccountStatus.ACTIVE);
		return user;
	}

	private static Role role(UUID id, String code) {
		Role role = new Role();
		role.setId(id);
		role.setCode(code);
		role.setName(code);
		return role;
	}

	private static UserRole userRole(User user, Role role, UserRoleStatus status) {
		UserRole userRole = new UserRole();
		userRole.setId(UUID.randomUUID());
		userRole.setUser(user);
		userRole.setRole(role);
		userRole.setStatus(status);
		return userRole;
	}
}

package com.smartlab.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
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
import com.smartlab.exception.ConflictingAdminOperationException;
import com.smartlab.exception.DuplicateActiveRoleAssignmentException;
import com.smartlab.exception.ForbiddenAdminOperationException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.RoleRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.UserRoleRepository;

class AdminUserRoleServiceTests {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final RoleRepository roleRepository = mock(RoleRepository.class);
	private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
	private final AdminRolePolicy rolePolicy = new AdminRolePolicy(userRepository, roleRepository, userRoleRepository);
	private final AdminUserRoleService service = new AdminUserRoleService(
			roleRepository,
			userRoleRepository,
			rolePolicy);

	@Test
	void regularAdminAssignsMemberRoleAndRecordsActor() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, "admin", "admin@example.edu");
		User target = user(UUID.randomUUID(), lab, "member", "member@example.edu");
		Role admin = role(UUID.randomUUID(), AdminUserRoleService.ADMIN_ROLE_CODE);
		Role member = role(UUID.randomUUID(), AdminUserRoleService.MEMBER_ROLE_CODE);
		stubActorAndTarget(actor, admin, target, List.of());
		when(roleRepository.findById(member.getId())).thenReturn(Optional.of(member));
		when(userRoleRepository.findByUserAndRole(target, member)).thenReturn(Optional.empty());
		when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> {
			UserRole userRole = invocation.getArgument(0);
			userRole.setId(UUID.randomUUID());
			return userRole;
		});

		AdminUserRoleService.AssignedRoleSummary summary = service.assignRoleToUser(
				new AdminUserRoleService.AssignUserRoleCommand(actor.getId(), target.getId(), member.getId()));

		assertEquals(AdminUserRoleService.MEMBER_ROLE_CODE, summary.roleCode());
		ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
		verify(userRoleRepository).save(captor.capture());
		assertEquals(actor, captor.getValue().getAssignedBy());
		assertEquals(UserRoleStatus.ACTIVE, captor.getValue().getStatus());
		verifyNoRolePermissionChanges();
	}

	@Test
	void rolePolicyPreventsForbiddenAssignmentTargetsAndDuplicateActiveAssignments() {
		Lab lab = lab(UUID.randomUUID());
		User adminActor = user(UUID.randomUUID(), lab, "admin", "admin@example.edu");
		User target = user(UUID.randomUUID(), lab, "target", "target@example.edu");
		Role admin = role(UUID.randomUUID(), AdminUserRoleService.ADMIN_ROLE_CODE);
		Role superAdmin = role(UUID.randomUUID(), AdminUserRoleService.SUPER_ADMIN_ROLE_CODE);
		stubActorAndTarget(adminActor, admin, target, List.of());
		when(roleRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
		when(roleRepository.findById(superAdmin.getId())).thenReturn(Optional.of(superAdmin));

		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.assignRoleToUser(
						new AdminUserRoleService.AssignUserRoleCommand(adminActor.getId(), target.getId(), admin.getId())));
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.assignRoleToUser(
						new AdminUserRoleService.AssignUserRoleCommand(adminActor.getId(), target.getId(), superAdmin.getId())));

		Role member = role(UUID.randomUUID(), AdminUserRoleService.MEMBER_ROLE_CODE);
		UserRole active = userRole(target, member, UserRoleStatus.ACTIVE);
		when(roleRepository.findById(member.getId())).thenReturn(Optional.of(member));
		when(userRoleRepository.findByUserAndRole(target, member)).thenReturn(Optional.of(active));
		assertThrows(
				DuplicateActiveRoleAssignmentException.class,
				() -> service.assignRoleToUser(
						new AdminUserRoleService.AssignUserRoleCommand(adminActor.getId(), target.getId(), member.getId())));
	}

	@Test
	void superAdminCanAssignAdminButCannotModifySelfOrCrossLabTarget() {
		Lab lab = lab(UUID.randomUUID());
		User superActor = user(UUID.randomUUID(), lab, "super", "super@example.edu");
		User target = user(UUID.randomUUID(), lab, "member", "member@example.edu");
		Role superAdmin = role(UUID.randomUUID(), AdminUserRoleService.SUPER_ADMIN_ROLE_CODE);
		Role admin = role(UUID.randomUUID(), AdminUserRoleService.ADMIN_ROLE_CODE);
		stubActorAndTarget(superActor, superAdmin, target, List.of());
		when(roleRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
		when(userRoleRepository.findByUserAndRole(target, admin)).thenReturn(Optional.empty());
		when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> {
			UserRole assignment = invocation.getArgument(0);
			assignment.setId(UUID.randomUUID());
			return assignment;
		});

		assertEquals(AdminUserRoleService.ADMIN_ROLE_CODE, service.assignRoleToUser(
				new AdminUserRoleService.AssignUserRoleCommand(superActor.getId(), target.getId(), admin.getId()))
				.roleCode());
		when(userRepository.findByIdAndLab(superActor.getId(), lab)).thenReturn(Optional.of(superActor));
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.assignRoleToUser(
						new AdminUserRoleService.AssignUserRoleCommand(superActor.getId(), superActor.getId(), admin.getId())));
		assertThrows(
				ResourceNotFoundException.class,
				() -> service.assignRoleToUser(
						new AdminUserRoleService.AssignUserRoleCommand(superActor.getId(), UUID.randomUUID(), admin.getId())));
	}

	@Test
	void revokeRoleDeactivatesHistoryButRejectsSuperAdminAndLastActiveRole() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, "super", "super@example.edu");
		User target = user(UUID.randomUUID(), lab, "member", "member@example.edu");
		Role superAdmin = role(UUID.randomUUID(), AdminUserRoleService.SUPER_ADMIN_ROLE_CODE);
		Role member = role(UUID.randomUUID(), AdminUserRoleService.MEMBER_ROLE_CODE);
		Role leader = role(UUID.randomUUID(), AdminUserRoleService.LEADER_ROLE_CODE);
		UserRole memberAssignment = userRole(target, member, UserRoleStatus.ACTIVE);
		UserRole leaderAssignment = userRole(target, leader, UserRoleStatus.ACTIVE);
		stubActorAndTarget(actor, superAdmin, target, List.of(memberAssignment, leaderAssignment));
		when(roleRepository.findById(member.getId())).thenReturn(Optional.of(member));
		when(userRoleRepository.findByUserAndRole(target, member)).thenReturn(Optional.of(memberAssignment));
		doReturn(List.of(memberAssignment, leaderAssignment))
				.doReturn(List.of(memberAssignment, leaderAssignment))
				.when(userRoleRepository)
				.findByUserAndStatus(target, UserRoleStatus.ACTIVE);

		service.revokeRoleFromUser(new AdminUserRoleService.RevokeUserRoleCommand(actor.getId(), target.getId(), member.getId()));

		assertEquals(UserRoleStatus.INACTIVE, memberAssignment.getStatus());
		verify(userRoleRepository, never()).delete(any(UserRole.class));

		when(roleRepository.findById(superAdmin.getId())).thenReturn(Optional.of(superAdmin));
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.revokeRoleFromUser(
						new AdminUserRoleService.RevokeUserRoleCommand(actor.getId(), target.getId(), superAdmin.getId())));

		UserRole onlyMember = userRole(target, member, UserRoleStatus.ACTIVE);
		when(userRoleRepository.findByUserAndRole(target, member)).thenReturn(Optional.of(onlyMember));
		doReturn(List.of(onlyMember))
				.doReturn(List.of(onlyMember))
				.when(userRoleRepository)
				.findByUserAndStatus(target, UserRoleStatus.ACTIVE);
		assertThrows(
				ConflictingAdminOperationException.class,
				() -> service.revokeRoleFromUser(
						new AdminUserRoleService.RevokeUserRoleCommand(actor.getId(), target.getId(), member.getId())));
	}

	@Test
	void replaceRolesAddsReactivatesAndDeactivatesAssignmentsTransactionally() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, "admin", "admin@example.edu");
		User target = user(UUID.randomUUID(), lab, "member", "member@example.edu");
		Role admin = role(UUID.randomUUID(), AdminUserRoleService.ADMIN_ROLE_CODE);
		Role leader = role(UUID.randomUUID(), AdminUserRoleService.LEADER_ROLE_CODE);
		Role member = role(UUID.randomUUID(), AdminUserRoleService.MEMBER_ROLE_CODE);
		UserRole activeLeader = userRole(target, leader, UserRoleStatus.ACTIVE);
		UserRole inactiveMember = userRole(target, member, UserRoleStatus.INACTIVE);
		stubActorAndTarget(actor, admin, target, List.of(activeLeader));
		doReturn(List.of(activeLeader))
				.doReturn(List.of(activeLeader))
				.when(userRoleRepository)
				.findByUserAndStatus(target, UserRoleStatus.ACTIVE);
		when(roleRepository.findByCodeIn(List.of(AdminUserRoleService.MEMBER_ROLE_CODE))).thenReturn(List.of(member));
		when(userRoleRepository.findByUserAndRole(target, member)).thenReturn(Optional.of(inactiveMember));

		AdminUserService.ManagedUserSummary summary = service.replaceRolesForUser(
				new AdminUserRoleService.ReplaceUserRolesCommand(
						actor.getId(),
						target.getId(),
						List.of(" member ", "MEMBER")));

		assertEquals(List.of(AdminUserRoleService.MEMBER_ROLE_CODE), summary.roleCodes());
		assertEquals(UserRoleStatus.INACTIVE, activeLeader.getStatus());
		assertEquals(UserRoleStatus.ACTIVE, inactiveMember.getStatus());
		assertEquals(actor, inactiveMember.getAssignedBy());
		verify(userRoleRepository, never()).delete(any(UserRole.class));
		verify(userRoleRepository, never()).save(any(UserRole.class));
	}

	@Test
	void replaceRolesKeepsUnchangedActiveAssignmentAndCreatesMissingAssignment() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, "admin", "admin@example.edu");
		User originalAssigner = user(UUID.randomUUID(), lab, "original", "original@example.edu");
		User target = user(UUID.randomUUID(), lab, "member", "member@example.edu");
		Role admin = role(UUID.randomUUID(), AdminUserRoleService.ADMIN_ROLE_CODE);
		Role leader = role(UUID.randomUUID(), AdminUserRoleService.LEADER_ROLE_CODE);
		Role member = role(UUID.randomUUID(), AdminUserRoleService.MEMBER_ROLE_CODE);
		UserRole activeLeader = userRole(target, leader, UserRoleStatus.ACTIVE);
		activeLeader.setAssignedBy(originalAssigner);
		stubActorAndTarget(actor, admin, target, List.of(activeLeader));
		when(roleRepository.findByCodeIn(List.of(
				AdminUserRoleService.LEADER_ROLE_CODE,
				AdminUserRoleService.MEMBER_ROLE_CODE))).thenReturn(List.of(leader, member));
		when(userRoleRepository.findByUserAndRole(target, leader)).thenReturn(Optional.of(activeLeader));
		when(userRoleRepository.findByUserAndRole(target, member)).thenReturn(Optional.empty());
		when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> {
			UserRole assignment = invocation.getArgument(0);
			assignment.setId(UUID.randomUUID());
			return assignment;
		});

		AdminUserService.ManagedUserSummary summary = service.replaceRolesForUser(
				new AdminUserRoleService.ReplaceUserRolesCommand(
						actor.getId(),
						target.getId(),
						List.of("member", "leader")));

		assertEquals(List.of(AdminUserRoleService.LEADER_ROLE_CODE, AdminUserRoleService.MEMBER_ROLE_CODE), summary.roleCodes());
		assertEquals(UserRoleStatus.ACTIVE, activeLeader.getStatus());
		assertEquals(originalAssigner, activeLeader.getAssignedBy());
		ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
		verify(userRoleRepository).save(captor.capture());
		assertEquals(member, captor.getValue().getRole());
		assertEquals(actor, captor.getValue().getAssignedBy());
		assertEquals(UserRoleStatus.ACTIVE, captor.getValue().getStatus());
		verify(userRoleRepository, never()).delete(any(UserRole.class));
	}

	@Test
	void replaceRolesAllowsActorAndTargetWithDifferentLabObjectsHavingSameUuid() {
		UUID labId = UUID.randomUUID();
		Lab actorLab = lab(labId);
		Lab targetLab = lab(labId);
		User actor = user(UUID.randomUUID(), actorLab, "admin", "admin@example.edu");
		User target = user(UUID.randomUUID(), targetLab, "member", "member@example.edu");
		Role admin = role(UUID.randomUUID(), AdminUserRoleService.ADMIN_ROLE_CODE);
		Role leader = role(UUID.randomUUID(), AdminUserRoleService.LEADER_ROLE_CODE);
		Role member = role(UUID.randomUUID(), AdminUserRoleService.MEMBER_ROLE_CODE);
		UserRole activeLeader = userRole(target, leader, UserRoleStatus.ACTIVE);
		UserRole inactiveMember = userRole(target, member, UserRoleStatus.INACTIVE);
		stubActorAndTarget(actor, admin, target, List.of(activeLeader));
		when(roleRepository.findByCodeIn(List.of(AdminUserRoleService.MEMBER_ROLE_CODE))).thenReturn(List.of(member));
		when(userRoleRepository.findByUserAndRole(target, member)).thenReturn(Optional.of(inactiveMember));

		AdminUserService.ManagedUserSummary summary = service.replaceRolesForUser(
				new AdminUserRoleService.ReplaceUserRolesCommand(
						actor.getId(),
						target.getId(),
						List.of("MEMBER")));

		assertEquals(List.of(AdminUserRoleService.MEMBER_ROLE_CODE), summary.roleCodes());
		assertEquals(UserRoleStatus.INACTIVE, activeLeader.getStatus());
		assertEquals(UserRoleStatus.ACTIVE, inactiveMember.getStatus());
		assertEquals(actor, inactiveMember.getAssignedBy());
		verify(userRoleRepository, never()).delete(any(UserRole.class));
		verify(userRoleRepository, never()).save(any(UserRole.class));
	}

	@Test
	void roleCatalogReturnsDeterministicAssignableFlags() {
		Lab lab = lab(UUID.randomUUID());
		User superActor = user(UUID.randomUUID(), lab, "super", "super@example.edu");
		Role superAdmin = role(UUID.randomUUID(), AdminUserRoleService.SUPER_ADMIN_ROLE_CODE);
		Role admin = role(UUID.randomUUID(), AdminUserRoleService.ADMIN_ROLE_CODE);
		Role leader = role(UUID.randomUUID(), AdminUserRoleService.LEADER_ROLE_CODE);
		Role member = role(UUID.randomUUID(), AdminUserRoleService.MEMBER_ROLE_CODE);
		stubActiveActor(superActor, superAdmin);
		when(roleRepository.findAll()).thenReturn(List.of(member, superAdmin, leader, admin));

		List<AdminUserRoleService.RoleCatalogSummary> catalog = service.listRoleCatalog(superActor.getId());

		assertEquals(List.of("ADMIN", "LEADER", "MEMBER", "SUPER_ADMIN"), catalog.stream().map(AdminUserRoleService.RoleCatalogSummary::code).toList());
		assertEquals(List.of(true, true, true, false), catalog.stream().map(AdminUserRoleService.RoleCatalogSummary::assignable).toList());
	}

	@Test
	void queriesReturnActiveRolesForSameLabUser() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, "admin", "admin@example.edu");
		User target = user(UUID.randomUUID(), lab, "member", "member@example.edu");
		Role admin = role(UUID.randomUUID(), AdminUserRoleService.ADMIN_ROLE_CODE);
		Role member = role(UUID.randomUUID(), AdminUserRoleService.MEMBER_ROLE_CODE);
		UserRole activeMember = userRole(target, member, UserRoleStatus.ACTIVE);
		stubActorAndTarget(actor, admin, target, List.of(activeMember));

		assertEquals(1, service.listActiveRolesForUser(actor.getId(), target.getId()).size());
	}

	@Test
	void serviceStructureUsesSpringTransactionsNoPasswordAndNoRolePermissionSurface()
			throws NoSuchMethodException {
		assertNotNull(AdminUserRoleService.class.getAnnotation(Service.class));
		assertTransactional("assignRoleToUser", AdminUserRoleService.AssignUserRoleCommand.class, false);
		assertTransactional("revokeRoleFromUser", AdminUserRoleService.RevokeUserRoleCommand.class, false);
		assertTransactional("replaceRolesForUser", AdminUserRoleService.ReplaceUserRolesCommand.class, false);
		assertTransactional("listActiveRolesForUser", new Class<?>[] {UUID.class, UUID.class}, true);
		assertTransactional("listRoleCatalog", UUID.class, true);
		assertFalse(Arrays.stream(AdminUserRoleService.class.getDeclaredMethods())
				.anyMatch(method -> method.getReturnType() == RolePermission.class));
		assertFalse(Arrays.stream(AdminUserRoleService.AssignUserRoleCommand.class.getRecordComponents())
				.anyMatch(component -> component.getName().toLowerCase().contains("password")));
	}

	private void stubActorAndTarget(User actor, Role actorRole, User target, List<UserRole> targetActiveAssignments) {
		stubActiveActor(actor, actorRole);
		when(userRepository.findByIdAndLab(target.getId(), actor.getLab())).thenReturn(Optional.of(target));
		when(userRoleRepository.findByUserAndStatus(target, UserRoleStatus.ACTIVE)).thenReturn(targetActiveAssignments);
	}

	private void stubActiveActor(User actor, Role role) {
		when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
		when(userRoleRepository.findByUserAndStatus(actor, UserRoleStatus.ACTIVE))
				.thenReturn(List.of(userRole(actor, role, UserRoleStatus.ACTIVE)));
	}

	private void verifyNoRolePermissionChanges() {
		assertFalse(Arrays.stream(AdminUserRoleService.class.getDeclaredFields())
				.anyMatch(field -> field.getType().getName().contains("RolePermission")));
	}

	private static void assertTransactional(String methodName, Class<?> argType, boolean readOnly)
			throws NoSuchMethodException {
		assertTransactional(methodName, new Class<?>[] {argType}, readOnly);
	}

	private static void assertTransactional(String methodName, Class<?>[] argTypes, boolean readOnly)
			throws NoSuchMethodException {
		Method method = AdminUserRoleService.class.getMethod(methodName, argTypes);
		Transactional transactional = method.getAnnotation(Transactional.class);
		assertNotNull(transactional);
		assertEquals(readOnly, transactional.readOnly());
	}

	private static Lab lab(UUID id) {
		Lab lab = new Lab();
		lab.setId(id);
		return lab;
	}

	private static User user(UUID id, Lab lab, String username, String email) {
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

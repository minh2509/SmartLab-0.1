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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.File;
import com.smartlab.entity.Lab;
import com.smartlab.entity.Role;
import com.smartlab.entity.User;
import com.smartlab.entity.UserRole;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;
import com.smartlab.exception.DuplicateUserEmailException;
import com.smartlab.exception.DuplicateUsernameException;
import com.smartlab.exception.ForbiddenAdminOperationException;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.FileRepository;
import com.smartlab.repository.RoleRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.UserRoleRepository;

class AdminUserServiceTests {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final FileRepository fileRepository = mock(FileRepository.class);
	private final RoleRepository roleRepository = mock(RoleRepository.class);
	private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final AdminRolePolicy rolePolicy = new AdminRolePolicy(userRepository, roleRepository, userRoleRepository);
	private final AdminUserService service = new AdminUserService(
			userRepository,
			fileRepository,
			userRoleRepository,
			passwordEncoder,
			rolePolicy);

	@Test
	void createManagedUserUsesActorLabEncodesTemporaryPasswordAndAssignsInitialRolesAtomically() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, "admin", "admin@example.edu", UserAccountStatus.ACTIVE);
		Role adminRole = role(UUID.randomUUID(), AdminUserRoleService.ADMIN_ROLE_CODE);
		Role memberRole = role(UUID.randomUUID(), AdminUserRoleService.MEMBER_ROLE_CODE);
		stubActiveActor(actor, adminRole);
		when(roleRepository.findByCodeIn(List.of(AdminUserRoleService.MEMBER_ROLE_CODE))).thenReturn(List.of(memberRole));
		when(userRepository.existsByLabAndEmail(lab, "minh@example.edu")).thenReturn(false);
		when(userRepository.existsByLabAndUsername(lab, "minh")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User saved = invocation.getArgument(0);
			saved.setId(UUID.randomUUID());
			return saved;
		});
		when(userRoleRepository.findByUserAndRole(any(User.class), org.mockito.Mockito.eq(memberRole)))
				.thenReturn(Optional.empty());
		when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> {
			UserRole assignment = invocation.getArgument(0);
			assignment.setId(UUID.randomUUID());
			return assignment;
		});

		AdminUserService.ManagedUserSummary created = service.createManagedUser(
				new AdminUserService.CreateManagedUserCommand(
						actor.getId(),
						"minh",
						"  Minh@Example.EDU ",
						"TemporaryPass123!",
						"Minh Hoang",
						null,
						List.of(" member ", "MEMBER")));

		assertEquals(lab.getId(), created.labId());
		assertEquals("minh@example.edu", created.email());
		assertEquals(List.of(AdminUserRoleService.MEMBER_ROLE_CODE), created.roleCodes());
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		assertEquals(lab, userCaptor.getValue().getLab());
		assertTrue(passwordEncoder.matches("TemporaryPass123!", userCaptor.getValue().getPasswordHash()));
		assertFalse("TemporaryPass123!".equals(userCaptor.getValue().getPasswordHash()));
		ArgumentCaptor<UserRole> userRoleCaptor = ArgumentCaptor.forClass(UserRole.class);
		verify(userRoleRepository).save(userRoleCaptor.capture());
		assertEquals(actor, userRoleCaptor.getValue().getAssignedBy());
		assertEquals(UserRoleStatus.ACTIVE, userRoleCaptor.getValue().getStatus());
	}

	@Test
	void createManagedUserTrimsSuppliedTemporaryPasswordBeforeEncodingAndReturningIt() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, "admin", "admin@example.edu", UserAccountStatus.ACTIVE);
		Role adminRole = role(UUID.randomUUID(), AdminUserRoleService.ADMIN_ROLE_CODE);
		Role memberRole = role(UUID.randomUUID(), AdminUserRoleService.MEMBER_ROLE_CODE);
		PasswordEncoder exactEncoder = mock(PasswordEncoder.class);
		AdminUserService exactPasswordService = new AdminUserService(
				userRepository,
				fileRepository,
				userRoleRepository,
				exactEncoder,
				rolePolicy);
		String originalPassword = "  TemporaryPass123!  ";
		String trimmedPassword = "TemporaryPass123!";
		stubActiveActor(actor, adminRole);
		when(roleRepository.findByCodeIn(List.of(AdminUserRoleService.MEMBER_ROLE_CODE))).thenReturn(List.of(memberRole));
		when(exactEncoder.encode(trimmedPassword)).thenReturn("encoded-trimmed-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User saved = invocation.getArgument(0);
			saved.setId(UUID.randomUUID());
			return saved;
		});
		when(userRoleRepository.findByUserAndRole(any(User.class), org.mockito.Mockito.eq(memberRole)))
				.thenReturn(Optional.empty());

		AdminUserService.ManagedUserSummary created = exactPasswordService.createManagedUser(
				new AdminUserService.CreateManagedUserCommand(
						actor.getId(),
						"minh",
						"minh@example.edu",
						originalPassword,
						"Minh Hoang",
						null,
						List.of("MEMBER")));

		verify(exactEncoder).encode(trimmedPassword);
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		assertEquals("encoded-trimmed-password", userCaptor.getValue().getPasswordHash());
		assertFalse(originalPassword.equals(userCaptor.getValue().getPasswordHash()));
		assertEquals(trimmedPassword, created.temporaryPassword());
		assertFalse(created.temporaryPasswordGenerated());
	}

	@Test
	void createManagedUserRejectsDuplicateEmailShortTemporaryPasswordAndForbiddenRole() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, "admin", "admin@example.edu", UserAccountStatus.ACTIVE);
		Role adminRole = role(UUID.randomUUID(), AdminUserRoleService.ADMIN_ROLE_CODE);
		stubActiveActor(actor, adminRole);
		when(roleRepository.findByCodeIn(List.of(AdminUserRoleService.MEMBER_ROLE_CODE)))
				.thenReturn(List.of(role(UUID.randomUUID(), AdminUserRoleService.MEMBER_ROLE_CODE)));
		when(userRepository.existsByLabAndEmail(lab, "dupe@example.edu")).thenReturn(true);

		assertThrows(
				DuplicateUserEmailException.class,
				() -> service.createManagedUser(new AdminUserService.CreateManagedUserCommand(
						actor.getId(),
						"dupe",
						"dupe@example.edu",
						"TemporaryPass123!",
						"Duplicate User",
						null,
						List.of("MEMBER"))));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.createManagedUser(new AdminUserService.CreateManagedUserCommand(
						actor.getId(),
						"blank",
						"blank@example.edu",
						"short",
						"Blank Password",
						null,
						List.of("MEMBER"))));
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.createManagedUser(new AdminUserService.CreateManagedUserCommand(
						actor.getId(),
						"admin2",
						"admin2@example.edu",
						"TemporaryPass123!",
						"Admin Two",
						null,
						List.of("ADMIN"))));
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void createManagedUserGeneratesTemporaryPasswordForWhitespaceOnlyInput() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, "admin", "admin@example.edu", UserAccountStatus.ACTIVE);
		Role adminRole = role(UUID.randomUUID(), AdminUserRoleService.ADMIN_ROLE_CODE);
		Role memberRole = role(UUID.randomUUID(), AdminUserRoleService.MEMBER_ROLE_CODE);
		PasswordEncoder exactEncoder = mock(PasswordEncoder.class);
		AdminUserService exactPasswordService = new AdminUserService(
				userRepository,
				fileRepository,
				userRoleRepository,
				exactEncoder,
				rolePolicy);
		stubActiveActor(actor, adminRole);
		when(roleRepository.findByCodeIn(List.of(AdminUserRoleService.MEMBER_ROLE_CODE))).thenReturn(List.of(memberRole));
		when(exactEncoder.encode(any())).thenReturn("encoded-generated-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User saved = invocation.getArgument(0);
			saved.setId(UUID.randomUUID());
			return saved;
		});
		when(userRoleRepository.findByUserAndRole(any(User.class), org.mockito.Mockito.eq(memberRole)))
				.thenReturn(Optional.empty());

		AdminUserService.ManagedUserSummary created = exactPasswordService.createManagedUser(
				new AdminUserService.CreateManagedUserCommand(
						actor.getId(),
						"blank-only",
						"blank-only@example.edu",
						"            ",
						"Blank Only Password",
						null,
						List.of("MEMBER")));

		ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
		verify(exactEncoder).encode(passwordCaptor.capture());
		String generatedPassword = passwordCaptor.getValue();
		assertNotNull(generatedPassword);
		assertFalse(generatedPassword.isBlank());
		assertTrue(generatedPassword.length() >= 12);
		assertTrue(generatedPassword.length() <= 72);
		assertEquals(generatedPassword, created.temporaryPassword());
		assertTrue(created.temporaryPasswordGenerated());
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		assertEquals("encoded-generated-password", userCaptor.getValue().getPasswordHash());
	}

	@Test
	void createManagedUserRejectsUnknownRoleBeforeSavingUser() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, "admin", "admin@example.edu", UserAccountStatus.ACTIVE);
		Role adminRole = role(UUID.randomUUID(), AdminUserRoleService.ADMIN_ROLE_CODE);
		stubActiveActor(actor, adminRole);
		when(roleRepository.findByCodeIn(List.of(AdminUserRoleService.MEMBER_ROLE_CODE))).thenReturn(List.of());

		assertThrows(
				ResourceNotFoundException.class,
				() -> service.createManagedUser(new AdminUserService.CreateManagedUserCommand(
						actor.getId(),
						"minh",
						"minh@example.edu",
						"TemporaryPass123!",
						"Minh Hoang",
						null,
						List.of("MEMBER"))));
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void updateManagedUserUsesActorLabAndRejectsCrossLabSelfAndAdminTargetForRegularAdmin() {
		Lab actorLab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), actorLab, "admin", "admin@example.edu", UserAccountStatus.ACTIVE);
		Role adminRole = role(UUID.randomUUID(), AdminUserRoleService.ADMIN_ROLE_CODE);
		stubActiveActor(actor, adminRole);
		User target = user(UUID.randomUUID(), actorLab, "member", "member@example.edu", UserAccountStatus.ACTIVE);
		Role targetAdminRole = role(UUID.randomUUID(), AdminUserRoleService.ADMIN_ROLE_CODE);

		when(userRepository.findByIdAndLab(target.getId(), actorLab)).thenReturn(Optional.of(target));
		when(userRoleRepository.findByUserAndStatus(target, UserRoleStatus.ACTIVE))
				.thenReturn(List.of(userRole(target, targetAdminRole, UserRoleStatus.ACTIVE)));

		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.updateManagedUser(new AdminUserService.UpdateManagedUserCommand(
						actor.getId(),
						target.getId(),
						"newname",
						null,
						null,
						null,
						false)));
		assertThrows(
				ResourceNotFoundException.class,
				() -> service.updateManagedUser(new AdminUserService.UpdateManagedUserCommand(
						actor.getId(),
						UUID.randomUUID(),
						"newname",
						null,
						null,
						null,
						false)));
		when(userRepository.findByIdAndLab(actor.getId(), actorLab)).thenReturn(Optional.of(actor));
		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.updateManagedUser(new AdminUserService.UpdateManagedUserCommand(
						actor.getId(),
						actor.getId(),
						"self",
						null,
						null,
						null,
						false)));
	}

	@Test
	void updateManagedUserUpdatesMutableFieldsAndRejectsDuplicateUsernameAndEmail() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, "super", "super@example.edu", UserAccountStatus.ACTIVE);
		User target = user(UUID.randomUUID(), lab, "old", "old@example.edu", UserAccountStatus.ACTIVE);
		File avatar = new File();
		avatar.setId(UUID.randomUUID());
		stubActiveActor(actor, role(UUID.randomUUID(), AdminUserRoleService.SUPER_ADMIN_ROLE_CODE));
		when(userRepository.findByIdAndLab(target.getId(), lab)).thenReturn(Optional.of(target));
		when(userRoleRepository.findByUserAndStatus(target, UserRoleStatus.ACTIVE)).thenReturn(List.of());
		when(fileRepository.findById(avatar.getId())).thenReturn(Optional.of(avatar));
		when(userRepository.existsByLabAndUsernameAndIdNot(lab, "newname", target.getId())).thenReturn(false);
		when(userRepository.existsByLabAndEmailAndIdNot(lab, "new@example.edu", target.getId())).thenReturn(false);

		AdminUserService.ManagedUserSummary updated = service.updateManagedUser(
				new AdminUserService.UpdateManagedUserCommand(
						actor.getId(),
						target.getId(),
						"newname",
						" New@Example.EDU ",
						"New Name",
						avatar.getId(),
						false));

		assertEquals("newname", updated.username());
		assertEquals("new@example.edu", updated.email());
		assertEquals(avatar, target.getAvatarFile());
		when(userRepository.existsByLabAndUsernameAndIdNot(lab, "taken", target.getId())).thenReturn(true);
		assertThrows(
				DuplicateUsernameException.class,
				() -> service.updateManagedUser(new AdminUserService.UpdateManagedUserCommand(
						actor.getId(),
						target.getId(),
						"taken",
						null,
						null,
						null,
						false)));
		when(userRepository.existsByLabAndEmailAndIdNot(lab, "taken@example.edu", target.getId())).thenReturn(true);
		assertThrows(
				DuplicateUserEmailException.class,
				() -> service.updateManagedUser(new AdminUserService.UpdateManagedUserCommand(
						actor.getId(),
						target.getId(),
						null,
						"taken@example.edu",
						null,
						null,
						false)));
	}

	@Test
	void changeAccountStatusAllowsOnlyActiveAndLockedAndDoesNotPhysicallyDeleteUser() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, "super", "super@example.edu", UserAccountStatus.ACTIVE);
		User target = user(UUID.randomUUID(), lab, "member", "member@example.edu", UserAccountStatus.ACTIVE);
		stubActiveActor(actor, role(UUID.randomUUID(), AdminUserRoleService.SUPER_ADMIN_ROLE_CODE));
		when(userRepository.findByIdAndLab(target.getId(), lab)).thenReturn(Optional.of(target));
		when(userRoleRepository.findByUserAndStatus(target, UserRoleStatus.ACTIVE)).thenReturn(List.of());

		assertEquals(UserAccountStatus.LOCKED, service.changeAccountStatus(
				new AdminUserService.ChangeAccountStatusCommand(actor.getId(), target.getId(), UserAccountStatus.LOCKED))
				.accountStatus());
		assertEquals(UserAccountStatus.LOCKED, target.getAccountStatus());
		verify(userRepository, never()).delete(any(User.class));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.changeAccountStatus(new AdminUserService.ChangeAccountStatusCommand(
						actor.getId(),
						target.getId(),
						UserAccountStatus.PENDING)));
	}

	@Test
	void readMethodsScopeToActorLabAndBatchLoadRoleCodes() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, "admin", "admin@example.edu", UserAccountStatus.ACTIVE);
		User member = user(UUID.randomUUID(), lab, "member", "member@example.edu", UserAccountStatus.ACTIVE);
		Role adminRole = role(UUID.randomUUID(), AdminUserRoleService.ADMIN_ROLE_CODE);
		Role memberRole = role(UUID.randomUUID(), AdminUserRoleService.MEMBER_ROLE_CODE);
		stubActiveActor(actor, adminRole);
		when(userRepository.findByIdAndLab(member.getId(), lab)).thenReturn(Optional.of(member));
		when(userRepository.findByLabAndEmail(lab, "member@example.edu")).thenReturn(Optional.of(member));
		when(userRepository.findByLabAndAccountStatusNot(lab, UserAccountStatus.DELETED)).thenReturn(List.of(member));
		when(userRoleRepository.findByUserAndStatus(member, UserRoleStatus.ACTIVE))
				.thenReturn(List.of(userRole(member, memberRole, UserRoleStatus.ACTIVE)));
		when(userRoleRepository.findByUserInAndStatus(List.of(member), UserRoleStatus.ACTIVE))
				.thenReturn(List.of(userRole(member, memberRole, UserRoleStatus.ACTIVE)));

		assertEquals(List.of(AdminUserRoleService.MEMBER_ROLE_CODE), service.findUserById(actor.getId(), member.getId()).roleCodes());
		assertEquals("member@example.edu", service.findUserByEmail(actor.getId(), " Member@Example.EDU ").email());
		assertEquals(List.of(AdminUserRoleService.MEMBER_ROLE_CODE), service.listUsers(actor.getId(), null).get(0).roleCodes());
	}

	@Test
	void lockedOrDeletedActorCannotPerformServiceOperationsWhenReloadedFromDatabase() {
		Lab lab = lab(UUID.randomUUID());
		User actor = user(UUID.randomUUID(), lab, "admin", "admin@example.edu", UserAccountStatus.LOCKED);
		when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> service.listUsers(actor.getId(), null));
	}

	@Test
	void serviceStructureUsesSpringTransactionsAndExposesOnlySafeCredentialMetadata() throws NoSuchMethodException {
		assertNotNull(AdminUserService.class.getAnnotation(Service.class));
		assertTransactional("createManagedUser", AdminUserService.CreateManagedUserCommand.class, false);
		assertTransactional("updateManagedUser", AdminUserService.UpdateManagedUserCommand.class, false);
		assertTransactional("changeAccountStatus", AdminUserService.ChangeAccountStatusCommand.class, false);
		assertTransactional("findUserById", new Class<?>[] {UUID.class, UUID.class}, true);
		assertTransactional("listUsers", new Class<?>[] {UUID.class, UserAccountStatus.class}, true);
		assertFalse(Arrays.stream(AdminUserService.ManagedUserSummary.class.getRecordComponents())
				.anyMatch(component -> component.getName().equals("passwordHash")
						|| component.getName().toLowerCase().contains("hash")));
		assertTrue(Arrays.stream(AdminUserService.ManagedUserSummary.class.getRecordComponents())
				.anyMatch(component -> component.getName().equals("temporaryPassword")));
		assertTrue(Arrays.stream(AdminUserService.ManagedUserSummary.class.getRecordComponents())
				.anyMatch(component -> component.getName().equals("temporaryPasswordGenerated")));
		assertTrue(Arrays.stream(AdminUserService.CreateManagedUserCommand.class.getRecordComponents())
				.anyMatch(component -> component.getName().equals("temporaryPassword")));
		assertFalse(Arrays.stream(AdminUserService.CreateManagedUserCommand.class.getRecordComponents())
				.anyMatch(component -> component.getName().equals("passwordHash")));
	}

	private void stubActiveActor(User actor, Role role) {
		when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
		when(userRoleRepository.findByUserAndStatus(actor, UserRoleStatus.ACTIVE))
				.thenReturn(List.of(userRole(actor, role, UserRoleStatus.ACTIVE)));
	}

	private static void assertTransactional(String methodName, Class<?> argType, boolean readOnly)
			throws NoSuchMethodException {
		assertTransactional(methodName, new Class<?>[] {argType}, readOnly);
	}

	private static void assertTransactional(String methodName, Class<?>[] argTypes, boolean readOnly)
			throws NoSuchMethodException {
		Method method = AdminUserService.class.getMethod(methodName, argTypes);
		Transactional transactional = method.getAnnotation(Transactional.class);
		assertNotNull(transactional);
		assertEquals(readOnly, transactional.readOnly());
	}

	private static Lab lab(UUID id) {
		Lab lab = new Lab();
		lab.setId(id);
		lab.setCode("SMART");
		return lab;
	}

	private static User user(UUID id, Lab lab, String username, String email, UserAccountStatus status) {
		User user = new User();
		user.setId(id);
		user.setLab(lab);
		user.setUsername(username);
		user.setEmail(email);
		user.setPasswordHash("hash");
		user.setFullName("Full Name");
		user.setAccountStatus(status);
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

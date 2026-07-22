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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.File;
import com.smartlab.entity.Lab;
import com.smartlab.entity.Role;
import com.smartlab.entity.User;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;
import com.smartlab.exception.DuplicateUserEmailException;
import com.smartlab.exception.DuplicateUsernameException;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ProtectedAdministratorOperationException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.FileRepository;
import com.smartlab.repository.LabRepository;
import com.smartlab.repository.RoleRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.UserRoleRepository;

class AdminUserServiceTests {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final LabRepository labRepository = mock(LabRepository.class);
	private final FileRepository fileRepository = mock(FileRepository.class);
	private final RoleRepository roleRepository = mock(RoleRepository.class);
	private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final AdminUserService service = new AdminUserService(
			userRepository,
			labRepository,
			fileRepository,
			roleRepository,
			userRoleRepository,
			passwordEncoder);

	@Test
	void createManagedUserNormalizesEmailAndDoesNotAssignRoleImplicitly() {
		UUID labId = UUID.randomUUID();
		Lab lab = lab(labId);
		when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
		when(userRepository.existsByLabAndEmail(lab, "minh@example.edu")).thenReturn(false);
		when(userRepository.existsByLabAndUsername(lab, "minh")).thenReturn(false);
		when(passwordEncoder.encode("temporary-password")).thenReturn("encoded-temporary-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			user.setId(UUID.randomUUID());
			return user;
		});

		AdminUserService.ManagedUserSummary created = service.createManagedUser(
				new AdminUserService.CreateManagedUserCommand(
						labId,
						"minh",
						"  Minh@Example.EDU ",
						"temporary-password",
						"Minh Hoang",
						null));

		assertEquals("minh@example.edu", created.email());
		assertEquals(UserAccountStatus.ACTIVE, created.accountStatus());
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		assertEquals("encoded-temporary-password", userCaptor.getValue().getPasswordHash());
		assertEquals("minh@example.edu", userCaptor.getValue().getEmail());
		verify(passwordEncoder).encode("temporary-password");
		verify(userRoleRepository, never()).save(any());
	}

	@Test
	void createManagedUserRejectsDuplicateEmailMissingLabBlankRequiredFieldsAndBlankPassword() {
		UUID labId = UUID.randomUUID();
		Lab lab = lab(labId);
		when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
		when(userRepository.existsByLabAndEmail(lab, "dupe@example.edu")).thenReturn(true);

		assertThrows(
				DuplicateUserEmailException.class,
				() -> service.createManagedUser(new AdminUserService.CreateManagedUserCommand(
						labId,
						"dupe",
						"dupe@example.edu",
						"password123",
						"Duplicate User",
						null)));
		assertThrows(
				ResourceNotFoundException.class,
				() -> service.createManagedUser(new AdminUserService.CreateManagedUserCommand(
						UUID.randomUUID(),
						"missing",
						"missing@example.edu",
						"password123",
						"Missing Lab",
						null)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.createManagedUser(new AdminUserService.CreateManagedUserCommand(
						labId,
						" ",
						"blank@example.edu",
						"password123",
						"Blank User",
						null)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> service.createManagedUser(new AdminUserService.CreateManagedUserCommand(
						labId,
						"blank-hash",
						"blank-hash@example.edu",
						" ",
						"Blank Hash",
						null)));
	}

	@Test
	void updateManagedUserUpdatesMutableFieldsAndClearsAvatarOnlyWhenExplicit() {
		UUID userId = UUID.randomUUID();
		UUID labId = UUID.randomUUID();
		UUID avatarId = UUID.randomUUID();
		Lab lab = lab(labId);
		User user = user(userId, lab, "old", "old@example.edu", UserAccountStatus.ACTIVE);
		File avatar = new File();
		avatar.setId(avatarId);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(userRepository.existsByLabAndEmailAndIdNot(lab, "new@example.edu", userId)).thenReturn(false);
		when(userRepository.existsByLabAndUsernameAndIdNot(lab, "newname", userId)).thenReturn(false);
		when(fileRepository.findById(avatarId)).thenReturn(Optional.of(avatar));

		AdminUserService.ManagedUserSummary updated = service.updateManagedUser(
				new AdminUserService.UpdateManagedUserCommand(
						userId,
						"newname",
						"  New@Example.EDU ",
						"New Name",
						avatarId,
						false));

		assertEquals("new@example.edu", updated.email());
		assertEquals("newname", user.getUsername());
		assertEquals("New Name", user.getFullName());
		assertEquals(avatar, user.getAvatarFile());

		service.updateManagedUser(new AdminUserService.UpdateManagedUserCommand(userId, null, null, null, null, true));
		assertEquals(null, user.getAvatarFile());
	}

	@Test
	void updateManagedUserAllowsUnchangedEmailPreservesOmittedFieldsAndRejectsDuplicates() {
		UUID userId = UUID.randomUUID();
		Lab lab = lab(UUID.randomUUID());
		User user = user(userId, lab, "old", "old@example.edu", UserAccountStatus.ACTIVE);
		user.setFullName("Old Name");
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(userRepository.existsByLabAndEmailAndIdNot(lab, "other@example.edu", userId)).thenReturn(true);

		AdminUserService.ManagedUserSummary unchanged = service.updateManagedUser(
				new AdminUserService.UpdateManagedUserCommand(userId, null, " OLD@example.edu ", null, null, false));

		assertEquals("old@example.edu", unchanged.email());
		assertEquals("old", user.getUsername());
		assertEquals("Old Name", user.getFullName());
		assertThrows(
				DuplicateUserEmailException.class,
				() -> service.updateManagedUser(
						new AdminUserService.UpdateManagedUserCommand(
								userId,
								null,
								"other@example.edu",
								null,
								null,
								false)));
	}

	@Test
	void updateManagedUserRejectsDuplicateUsernameAndMissingUser() {
		UUID userId = UUID.randomUUID();
		Lab lab = lab(UUID.randomUUID());
		User user = user(userId, lab, "old", "old@example.edu", UserAccountStatus.ACTIVE);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(userRepository.existsByLabAndUsernameAndIdNot(lab, "taken", userId)).thenReturn(true);

		assertThrows(
				DuplicateUsernameException.class,
				() -> service.updateManagedUser(
						new AdminUserService.UpdateManagedUserCommand(userId, "taken", null, null, null, false)));
		assertThrows(
				ResourceNotFoundException.class,
				() -> service.updateManagedUser(
						new AdminUserService.UpdateManagedUserCommand(
								UUID.randomUUID(),
								"name",
								null,
								null,
								null,
								false)));
	}

	@Test
	void changeAccountStatusIsIdempotentRejectsNullAndDoesNotPhysicallyDeleteUser() {
		UUID userId = UUID.randomUUID();
		User user = user(userId, lab(UUID.randomUUID()), "minh", "minh@example.edu", UserAccountStatus.ACTIVE);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		assertEquals(UserAccountStatus.ACTIVE, service.changeAccountStatus(userId, UserAccountStatus.ACTIVE).accountStatus());
		assertEquals(UserAccountStatus.LOCKED, service.changeAccountStatus(userId, UserAccountStatus.LOCKED).accountStatus());
		assertEquals(UserAccountStatus.LOCKED, user.getAccountStatus());
		verify(userRepository, never()).delete(any(User.class));
		assertThrows(InvalidAdminServiceInputException.class, () -> service.changeAccountStatus(userId, null));
	}

	@Test
	void lockUnlockResetPasswordAndSoftDeleteUseExplicitLifecycleRules() {
		UUID userId = UUID.randomUUID();
		User user = user(userId, lab(UUID.randomUUID()), "minh", "minh@example.edu", UserAccountStatus.ACTIVE);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(roleRepository.findByCode(AdminUserRoleService.SUPER_ADMIN_ROLE_CODE)).thenReturn(Optional.empty());
		when(passwordEncoder.encode("new-temporary-password")).thenReturn("encoded-new-temporary-password");

		assertEquals(UserAccountStatus.LOCKED, service.lockUser(userId).accountStatus());
		assertEquals(UserAccountStatus.ACTIVE, service.unlockUser(userId).accountStatus());
		service.resetPassword(userId, "new-temporary-password");
		assertEquals("encoded-new-temporary-password", user.getPasswordHash());

		AdminUserService.ManagedUserSummary deleted = service.softDeleteUser(userId, null);

		assertEquals(UserAccountStatus.DELETED, deleted.accountStatus());
		assertNotNull(user.getDeletedAt());
		verify(userRepository, never()).delete(any(User.class));
		assertThrows(InvalidAdminServiceInputException.class, () -> service.lockUser(userId));
		assertThrows(InvalidAdminServiceInputException.class, () -> service.unlockUser(userId));
		assertThrows(InvalidAdminServiceInputException.class, () -> service.resetPassword(userId, " "));
	}

	@Test
	void changeAccountStatusProtectsFinalActiveSuperAdminWhenEnforceable() {
		UUID userId = UUID.randomUUID();
		User user = user(userId, lab(UUID.randomUUID()), "root", "root@example.edu", UserAccountStatus.ACTIVE);
		Role superAdmin = role(UUID.randomUUID(), AdminUserRoleService.SUPER_ADMIN_ROLE_CODE);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(roleRepository.findByCode(AdminUserRoleService.SUPER_ADMIN_ROLE_CODE)).thenReturn(Optional.of(superAdmin));
		when(userRoleRepository.existsByUserAndRoleAndStatus(user, superAdmin, UserRoleStatus.ACTIVE)).thenReturn(true);
		when(userRoleRepository.countByRoleAndStatusAndUserAccountStatus(
				superAdmin,
				UserRoleStatus.ACTIVE,
				UserAccountStatus.ACTIVE)).thenReturn(1L);

		assertThrows(
				ProtectedAdministratorOperationException.class,
				() -> service.changeAccountStatus(userId, UserAccountStatus.LOCKED));
		assertEquals(UserAccountStatus.ACTIVE, user.getAccountStatus());
	}

	@Test
	void readMethodsUseNormalizedEmailAndStatusRepositoryQueries() {
		UUID userId = UUID.randomUUID();
		UUID labId = UUID.randomUUID();
		Lab lab = lab(labId);
		User user = user(userId, lab, "minh", "minh@example.edu", UserAccountStatus.ACTIVE);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
		when(userRepository.findByLabAndEmail(lab, "minh@example.edu")).thenReturn(Optional.of(user));
		when(userRepository.findByLab(lab)).thenReturn(List.of(user));
		when(userRepository.findByAccountStatus(UserAccountStatus.ACTIVE)).thenReturn(List.of(user));
		when(userRepository.findByLabAndAccountStatus(lab, UserAccountStatus.ACTIVE)).thenReturn(List.of(user));

		assertTrue(service.findUserById(userId).isPresent());
		assertTrue(service.findUserByLabAndEmail(labId, " Minh@Example.EDU ").isPresent());
		assertEquals(1, service.listUsersByLab(labId).size());
		assertEquals(1, service.listUsersByAccountStatus(UserAccountStatus.ACTIVE).size());
		assertEquals(1, service.listUsersByLabAndAccountStatus(labId, UserAccountStatus.ACTIVE).size());
	}

	@Test
	void serviceStructureUsesSpringTransactionsAndDoesNotExposePasswordHash() throws NoSuchMethodException {
		assertNotNull(AdminUserService.class.getAnnotation(Service.class));
		assertTrue(AdminUserService.class.getConstructors()[0].getParameterCount() > 0);
		assertTransactional("createManagedUser", AdminUserService.CreateManagedUserCommand.class, false);
		assertTransactional("updateManagedUser", AdminUserService.UpdateManagedUserCommand.class, false);
		assertTransactional("changeAccountStatus", UUID.class, UserAccountStatus.class, false);
		assertTransactional("lockUser", UUID.class, false);
		assertTransactional("unlockUser", UUID.class, false);
		assertTransactional("resetPassword", new Class<?>[] {UUID.class, String.class}, false);
		assertTransactional("softDeleteUser", new Class<?>[] {UUID.class, UUID.class}, false);
		assertTransactional("findUserById", UUID.class, true);
		assertFalse(AdminUserService.ManagedUserSummary.class.getName().toLowerCase().contains("password"));
		assertFalse(Arrays.stream(AdminUserService.ManagedUserSummary.class.getRecordComponents())
				.anyMatch(component -> component.getName().toLowerCase().contains("password")));
		assertTrue(Arrays.stream(AdminUserService.CreateManagedUserCommand.class.getRecordComponents())
				.anyMatch(component -> component.getName().equals("password")));
		assertFalse(Arrays.stream(AdminUserService.CreateManagedUserCommand.class.getRecordComponents())
				.anyMatch(component -> component.getName().equals("passwordHash")
						|| component.getName().toLowerCase().contains("plaintext")));
		assertFalse(Arrays.stream(AdminUserService.UpdateManagedUserCommand.class.getRecordComponents())
				.anyMatch(component -> component.getName().toLowerCase().contains("password")));
	}

	private static void assertTransactional(String methodName, Class<?> argType, boolean readOnly) throws NoSuchMethodException {
		assertTransactional(methodName, new Class<?>[] {argType}, readOnly);
	}

	private static void assertTransactional(
			String methodName,
			Class<?> argType1,
			Class<?> argType2,
			boolean readOnly) throws NoSuchMethodException {
		assertTransactional(methodName, new Class<?>[] {argType1, argType2}, readOnly);
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
}

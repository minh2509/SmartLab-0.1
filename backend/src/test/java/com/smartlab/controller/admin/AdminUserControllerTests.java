package com.smartlab.controller.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.smartlab.enums.UserAccountStatus;
import com.smartlab.exception.ApiExceptionHandler;
import com.smartlab.exception.DuplicateUserEmailException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.mapper.AdminUserApiMapper;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminUserService;

class AdminUserControllerTests {

	private final AdminUserService adminUserService = mock(AdminUserService.class);
	private final AuthenticatedActorResolver actorResolver = mock(AuthenticatedActorResolver.class);
	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new AdminUserController(adminUserService, new AdminUserApiMapper(), actorResolver))
			.setControllerAdvice(new ApiExceptionHandler())
			.setValidator(validator())
			.build();

	@Test
	void createUserAcceptsTemporaryPasswordRoleCodesAndReturnsActiveRolesWithoutCredentials() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID labId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminUserService.createManagedUser(any(AdminUserService.CreateManagedUserCommand.class)))
				.thenReturn(userSummary(
						userId,
						labId,
						"minh",
						"minh@example.edu",
						UserAccountStatus.ACTIVE,
						List.of("MEMBER")));

		mockMvc.perform(post("/api/admin/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "minh",
								  "email": "Minh@Example.EDU",
								  "temporaryPassword": "TemporaryPass123!",
								  "fullName": "Minh Hoang",
								  "avatarFileId": null,
								  "roleCodes": [" member ", "MEMBER"]
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/admin/users/" + userId))
				.andExpect(jsonPath("$.id").value(userId.toString()))
				.andExpect(jsonPath("$.labId").value(labId.toString()))
				.andExpect(jsonPath("$.email").value("minh@example.edu"))
				.andExpect(jsonPath("$.roleCodes[0]").value("MEMBER"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.temporaryPassword").doesNotExist())
				.andExpect(content().string(not(containsString("TemporaryPass123!"))));

		ArgumentCaptor<AdminUserService.CreateManagedUserCommand> captor =
				ArgumentCaptor.forClass(AdminUserService.CreateManagedUserCommand.class);
		verify(adminUserService).createManagedUser(captor.capture());
		assertEquals(actorUserId, captor.getValue().actorUserId());
		assertEquals("TemporaryPass123!", captor.getValue().temporaryPassword());
		assertEquals(List.of(" member ", "MEMBER"), captor.getValue().roleCodes());
	}

	@Test
	void createUserRejectsPasswordHashContractAndValidationDoesNotEchoPassword() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());

		mockMvc.perform(post("/api/admin/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "minh",
								  "email": "not-an-email",
								  "passwordHash": "caller-supplied-hash",
								  "temporaryPassword": "TemporaryPass123!",
								  "fullName": "Minh Hoang",
								  "roleCodes": []
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed."))
				.andExpect(content().string(not(containsString("caller-supplied-hash"))))
				.andExpect(content().string(not(containsString("TemporaryPass123!"))))
				.andExpect(content().string(not(containsString("temporaryPassword"))));

		verify(adminUserService, never()).createManagedUser(any());
	}

	@Test
	void getUserAndFindByEmailUseActorScopedServiceAndMapMissingUserToNotFound() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminUserService.findUserById(actorUserId, userId))
				.thenThrow(new ResourceNotFoundException("User was not found."));
		when(adminUserService.findUserByEmail(actorUserId, "missing@example.edu"))
				.thenThrow(new ResourceNotFoundException("User was not found."));

		mockMvc.perform(get("/api/admin/users/{userId}", userId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("User was not found."));
		mockMvc.perform(get("/api/admin/users/by-email")
						.param("email", "missing@example.edu"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("User was not found."));
	}

	@Test
	void listUsersIsActorLabScopedAndDoesNotRequireLabId() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID labId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminUserService.listUsers(actorUserId, null))
				.thenReturn(List.of(userSummary(
						UUID.randomUUID(),
						labId,
						"member",
						"member@example.edu",
						UserAccountStatus.ACTIVE,
						List.of("LEADER", "MEMBER"))));
		when(adminUserService.listUsers(actorUserId, UserAccountStatus.LOCKED)).thenReturn(List.of());

		mockMvc.perform(get("/api/admin/users"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].roleCodes[0]").value("LEADER"))
				.andExpect(jsonPath("$[0].roleCodes[1]").value("MEMBER"));
		mockMvc.perform(get("/api/admin/users").param("status", "LOCKED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	void updateAndStatusEndpointsPassActorIdentityAndAllowedCommands() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID avatarId = UUID.randomUUID();
		UUID labId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminUserService.updateManagedUser(any(AdminUserService.UpdateManagedUserCommand.class)))
				.thenReturn(userSummary(userId, labId, "newname", "new@example.edu", UserAccountStatus.ACTIVE, List.of("MEMBER")));
		when(adminUserService.changeAccountStatus(any(AdminUserService.ChangeAccountStatusCommand.class)))
				.thenReturn(userSummary(userId, labId, "newname", "new@example.edu", UserAccountStatus.LOCKED, List.of("MEMBER")));

		mockMvc.perform(patch("/api/admin/users/{userId}", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "newname",
								  "email": "new@example.edu",
								  "fullName": "New Name",
								  "avatarFileId": "%s",
								  "clearAvatarFile": false
								}
								""".formatted(avatarId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("newname"))
				.andExpect(jsonPath("$.roleCodes[0]").value("MEMBER"));
		mockMvc.perform(patch("/api/admin/users/{userId}/status", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status": "LOCKED"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accountStatus").value("LOCKED"));

		ArgumentCaptor<AdminUserService.UpdateManagedUserCommand> updateCaptor =
				ArgumentCaptor.forClass(AdminUserService.UpdateManagedUserCommand.class);
		verify(adminUserService).updateManagedUser(updateCaptor.capture());
		assertEquals(actorUserId, updateCaptor.getValue().actorUserId());
		assertEquals(userId, updateCaptor.getValue().userId());
		assertEquals(avatarId, updateCaptor.getValue().avatarFileId());
		ArgumentCaptor<AdminUserService.ChangeAccountStatusCommand> statusCaptor =
				ArgumentCaptor.forClass(AdminUserService.ChangeAccountStatusCommand.class);
		verify(adminUserService).changeAccountStatus(statusCaptor.capture());
		assertEquals(actorUserId, statusCaptor.getValue().actorUserId());
	}

	@Test
	void partialEmailPatchReachesServiceAndReturnsDuplicateEmailConflict() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminUserService.updateManagedUser(any(AdminUserService.UpdateManagedUserCommand.class)))
				.thenThrow(new DuplicateUserEmailException("User email already exists in the lab."));

		mockMvc.perform(patch("/api/admin/users/{userId}", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"taken@example.edu"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("User email already exists in the lab."));

		ArgumentCaptor<AdminUserService.UpdateManagedUserCommand> captor =
				ArgumentCaptor.forClass(AdminUserService.UpdateManagedUserCommand.class);
		verify(adminUserService).updateManagedUser(captor.capture());
		assertEquals(actorUserId, captor.getValue().actorUserId());
		assertEquals(userId, captor.getValue().userId());
		assertNull(captor.getValue().username());
		assertEquals("taken@example.edu", captor.getValue().email());
		assertNull(captor.getValue().fullName());
		assertNull(captor.getValue().avatarFileId());
		assertEquals(false, captor.getValue().clearAvatarFile());
	}

	@Test
	void partialFullNamePatchDefaultsOtherFieldsToOmitted() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID labId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminUserService.updateManagedUser(any(AdminUserService.UpdateManagedUserCommand.class)))
				.thenReturn(userSummary(userId, labId, "member", "member@example.edu", UserAccountStatus.ACTIVE, List.of("MEMBER")));

		mockMvc.perform(patch("/api/admin/users/{userId}", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Updated Name"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userId.toString()));

		ArgumentCaptor<AdminUserService.UpdateManagedUserCommand> captor =
				ArgumentCaptor.forClass(AdminUserService.UpdateManagedUserCommand.class);
		verify(adminUserService).updateManagedUser(captor.capture());
		assertNull(captor.getValue().username());
		assertNull(captor.getValue().email());
		assertEquals("Updated Name", captor.getValue().fullName());
		assertNull(captor.getValue().avatarFileId());
		assertEquals(false, captor.getValue().clearAvatarFile());
	}

	@Test
	void clearAvatarPatchSupportsSingleBooleanField() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID labId = UUID.randomUUID();
		when(actorResolver.requireActorUserId()).thenReturn(actorUserId);
		when(adminUserService.updateManagedUser(any(AdminUserService.UpdateManagedUserCommand.class)))
				.thenReturn(userSummary(userId, labId, "member", "member@example.edu", UserAccountStatus.ACTIVE, List.of("MEMBER")));

		mockMvc.perform(patch("/api/admin/users/{userId}", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"clearAvatarFile":true}
								"""))
				.andExpect(status().isOk());

		ArgumentCaptor<AdminUserService.UpdateManagedUserCommand> captor =
				ArgumentCaptor.forClass(AdminUserService.UpdateManagedUserCommand.class);
		verify(adminUserService).updateManagedUser(captor.capture());
		assertEquals(true, captor.getValue().clearAvatarFile());
	}

	@Test
	void invalidSuppliedEmailReturnsBadRequestWithoutCallingService() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());

		mockMvc.perform(patch("/api/admin/users/{userId}", UUID.randomUUID())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"not-an-email"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed."));

		verify(adminUserService, never()).updateManagedUser(any());
	}

	@Test
	void unknownPasswordHashPatchFieldReturnsBadRequestWithoutEchoingValueOrCallingService() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());

		mockMvc.perform(patch("/api/admin/users/{userId}", UUID.randomUUID())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Updated Name",
								  "passwordHash": "caller-supplied-hash"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed."))
				.andExpect(content().string(not(containsString("caller-supplied-hash"))));

		verify(adminUserService, never()).updateManagedUser(any());
	}

	@Test
	void duplicateEmailUsesGlobalExceptionHandlerConflictMapping() throws Exception {
		when(actorResolver.requireActorUserId()).thenReturn(UUID.randomUUID());
		when(adminUserService.createManagedUser(any(AdminUserService.CreateManagedUserCommand.class)))
				.thenThrow(new DuplicateUserEmailException("User email already exists in the lab."));

		mockMvc.perform(post("/api/admin/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "minh",
								  "email": "minh@example.edu",
								  "temporaryPassword": "TemporaryPass123!",
								  "fullName": "Minh Hoang",
								  "roleCodes": ["MEMBER"]
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("User email already exists in the lab."));
	}

	private static LocalValidatorFactoryBean validator() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		return validator;
	}

	private static AdminUserService.ManagedUserSummary userSummary(
			UUID id,
			UUID labId,
			String username,
			String email,
			UserAccountStatus status,
			List<String> roleCodes) {
		return new AdminUserService.ManagedUserSummary(
				id,
				labId,
				username,
				email,
				"Full Name",
				null,
				status,
				roleCodes);
	}
}

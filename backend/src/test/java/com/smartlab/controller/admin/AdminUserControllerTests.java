package com.smartlab.controller.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Optional;
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
import com.smartlab.service.admin.AdminUserService;

class AdminUserControllerTests {

	private final AdminUserService adminUserService = mock(AdminUserService.class);
	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new AdminUserController(adminUserService, new AdminUserApiMapper()))
			.setControllerAdvice(new ApiExceptionHandler())
			.setValidator(validator())
			.build();

	@Test
	void createUserReturnsCreatedResponseWithoutPasswordHash() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID labId = UUID.randomUUID();
		when(adminUserService.createManagedUser(any(AdminUserService.CreateManagedUserCommand.class)))
				.thenReturn(userSummary(userId, labId, "minh", "minh@example.edu", UserAccountStatus.ACTIVE));

		mockMvc.perform(post("/api/admin/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "labId": "%s",
								  "username": "minh",
								  "email": "Minh@Example.EDU",
								  "passwordHash": "derived-password-hash",
								  "fullName": "Minh Hoang"
								}
								""".formatted(labId)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/admin/users/" + userId))
				.andExpect(jsonPath("$.id").value(userId.toString()))
				.andExpect(jsonPath("$.email").value("minh@example.edu"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(content().string(not(containsString("derived-password-hash"))));

		ArgumentCaptor<AdminUserService.CreateManagedUserCommand> captor =
				ArgumentCaptor.forClass(AdminUserService.CreateManagedUserCommand.class);
		verify(adminUserService).createManagedUser(captor.capture());
		assertEquals(labId, captor.getValue().labId());
		assertEquals("derived-password-hash", captor.getValue().passwordHash());
	}

	@Test
	void createUserValidationFailureDoesNotEchoPasswordHashOrCallService() throws Exception {
		mockMvc.perform(post("/api/admin/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "labId": "%s",
								  "username": "minh",
								  "email": "not-an-email",
								  "passwordHash": "",
								  "fullName": "Minh Hoang"
								}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed."))
				.andExpect(content().string(not(containsString("passwordHash"))));

		verify(adminUserService, never()).createManagedUser(any());
	}

	@Test
	void getUserAndFindByEmailMapMissingUserToNotFound() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID labId = UUID.randomUUID();
		when(adminUserService.findUserById(userId)).thenReturn(Optional.empty());
		when(adminUserService.findUserByLabAndEmail(labId, "missing@example.edu")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/admin/users/{userId}", userId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("User was not found."));
		mockMvc.perform(get("/api/admin/users/by-email")
						.param("labId", labId.toString())
						.param("email", "missing@example.edu"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("User was not found."));
	}

	@Test
	void listUsersSupportsServiceBackedFiltersOnly() throws Exception {
		UUID labId = UUID.randomUUID();
		when(adminUserService.listUsersByLabAndAccountStatus(labId, UserAccountStatus.ACTIVE))
				.thenReturn(List.of(userSummary(UUID.randomUUID(), labId, "active", "active@example.edu", UserAccountStatus.ACTIVE)));
		when(adminUserService.listUsersByLab(labId)).thenReturn(List.of());
		when(adminUserService.listUsersByAccountStatus(UserAccountStatus.LOCKED)).thenReturn(List.of());

		mockMvc.perform(get("/api/admin/users")
						.param("labId", labId.toString())
						.param("status", "ACTIVE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].accountStatus").value("ACTIVE"));
		mockMvc.perform(get("/api/admin/users").param("labId", labId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
		mockMvc.perform(get("/api/admin/users").param("status", "LOCKED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
		mockMvc.perform(get("/api/admin/users"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("At least one user filter must be provided."));
	}

	@Test
	void updateAndStatusEndpointsMapOnlyAllowedCommands() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID avatarId = UUID.randomUUID();
		UUID labId = UUID.randomUUID();
		when(adminUserService.updateManagedUser(any(AdminUserService.UpdateManagedUserCommand.class)))
				.thenReturn(userSummary(userId, labId, "newname", "new@example.edu", UserAccountStatus.ACTIVE));
		when(adminUserService.changeAccountStatus(userId, UserAccountStatus.LOCKED))
				.thenReturn(userSummary(userId, labId, "newname", "new@example.edu", UserAccountStatus.LOCKED));

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
				.andExpect(jsonPath("$.username").value("newname"));
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
		assertEquals(userId, updateCaptor.getValue().userId());
		assertEquals(avatarId, updateCaptor.getValue().avatarFileId());
	}

	@Test
	void duplicateEmailUsesGlobalExceptionHandlerConflictMapping() throws Exception {
		UUID labId = UUID.randomUUID();
		when(adminUserService.createManagedUser(any(AdminUserService.CreateManagedUserCommand.class)))
				.thenThrow(new DuplicateUserEmailException("User email already exists in the lab."));

		mockMvc.perform(post("/api/admin/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "labId": "%s",
								  "username": "minh",
								  "email": "minh@example.edu",
								  "passwordHash": "derived-password-hash",
								  "fullName": "Minh Hoang"
								}
								""".formatted(labId)))
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
			UserAccountStatus status) {
		return new AdminUserService.ManagedUserSummary(
				id,
				labId,
				username,
				email,
				"Full Name",
				null,
				status);
	}
}

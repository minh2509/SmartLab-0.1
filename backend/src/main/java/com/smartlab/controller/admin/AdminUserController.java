package com.smartlab.controller.admin;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.request.admin.ChangeUserStatusRequest;
import com.smartlab.dto.request.admin.CreateAdminUserRequest;
import com.smartlab.dto.request.admin.ResetAdminUserPasswordRequest;
import com.smartlab.dto.request.admin.UpdateAdminUserRequest;
import com.smartlab.dto.response.admin.AdminUserResponse;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.mapper.AdminUserApiMapper;
import com.smartlab.service.admin.AdminUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
@Profile("!nodb")
public class AdminUserController {

	private final AdminUserService adminUserService;
	private final AdminUserApiMapper mapper;

	public AdminUserController(AdminUserService adminUserService, AdminUserApiMapper mapper) {
		this.adminUserService = adminUserService;
		this.mapper = mapper;
	}

	@PostMapping
	public ResponseEntity<AdminUserResponse> createUser(@Valid @RequestBody CreateAdminUserRequest request) {
		AdminUserService.ManagedUserSummary created = adminUserService.createManagedUser(
				new AdminUserService.CreateManagedUserCommand(
						request.labId(),
						request.username(),
						request.email(),
						request.passwordHash(),
						request.fullName(),
						request.avatarFileId()));
		return ResponseEntity
				.created(URI.create("/api/admin/users/" + created.id()))
				.body(mapper.toUserResponse(created));
	}

	@GetMapping("/{userId}")
	public AdminUserResponse getUser(@PathVariable UUID userId) {
		return adminUserService.findUserById(userId)
				.map(mapper::toUserResponse)
				.orElseThrow(() -> new ResourceNotFoundException("User was not found."));
	}

	@GetMapping("/by-email")
	public AdminUserResponse findUserByEmail(
			@RequestParam UUID labId,
			@RequestParam String email) {
		return adminUserService.findUserByLabAndEmail(labId, email)
				.map(mapper::toUserResponse)
				.orElseThrow(() -> new ResourceNotFoundException("User was not found."));
	}

	@GetMapping
	public List<AdminUserResponse> listUsers(
			@RequestParam(required = false) UUID labId,
			@RequestParam(required = false) UserAccountStatus status) {
		if (labId != null && status != null) {
			return adminUserService.listUsersByLabAndAccountStatus(labId, status)
					.stream()
					.map(mapper::toUserResponse)
					.toList();
		}
		if (labId != null) {
			return adminUserService.listUsersByLab(labId).stream().map(mapper::toUserResponse).toList();
		}
		if (status != null) {
			return adminUserService.listUsersByAccountStatus(status).stream().map(mapper::toUserResponse).toList();
		}
		throw new InvalidAdminServiceInputException("At least one user filter must be provided.");
	}

	@PutMapping("/{userId}")
	public AdminUserResponse updateUser(
			@PathVariable UUID userId,
			@Valid @RequestBody UpdateAdminUserRequest request) {
		return mapper.toUserResponse(adminUserService.updateManagedUser(
				new AdminUserService.UpdateManagedUserCommand(
						userId,
						request.username(),
						request.email(),
						request.fullName(),
						request.avatarFileId(),
						request.clearAvatarFile())));
	}

	@PatchMapping("/{userId}")
	public AdminUserResponse patchUser(
			@PathVariable UUID userId,
			@Valid @RequestBody UpdateAdminUserRequest request) {
		return updateUser(userId, request);
	}

	@PatchMapping("/{userId}/status")
	public AdminUserResponse changeUserStatus(
			@PathVariable UUID userId,
			@Valid @RequestBody ChangeUserStatusRequest request) {
		return mapper.toUserResponse(adminUserService.changeAccountStatus(userId, request.status()));
	}

	@PatchMapping("/{userId}/lock")
	public AdminUserResponse lockUser(@PathVariable UUID userId) {
		return mapper.toUserResponse(adminUserService.lockUser(userId));
	}

	@PatchMapping("/{userId}/unlock")
	public AdminUserResponse unlockUser(@PathVariable UUID userId) {
		return mapper.toUserResponse(adminUserService.unlockUser(userId));
	}

	@PatchMapping("/{userId}/reset-password")
	public AdminUserResponse resetPassword(
			@PathVariable UUID userId,
			@Valid @RequestBody ResetAdminUserPasswordRequest request) {
		return mapper.toUserResponse(adminUserService.resetPassword(userId, request.passwordHash()));
	}

	@DeleteMapping("/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void softDeleteUser(@PathVariable UUID userId) {
		adminUserService.softDeleteUser(userId, null);
	}
}

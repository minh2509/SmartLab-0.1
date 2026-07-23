package com.smartlab.controller.admin;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
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
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.request.admin.ChangeUserStatusRequest;
import com.smartlab.dto.request.admin.CreateAdminUserRequest;
import com.smartlab.dto.request.admin.ResetUserPasswordRequest;
import com.smartlab.dto.request.admin.UpdateAdminUserRequest;
import com.smartlab.dto.response.admin.AdminUserCredentialResponse;
import com.smartlab.dto.response.admin.AdminUserResponse;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.mapper.AdminUserApiMapper;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
@Profile("!nodb")
public class AdminUserController {

	private final AdminUserService adminUserService;
	private final AdminUserApiMapper mapper;
	private final AuthenticatedActorResolver actorResolver;

	public AdminUserController(
			AdminUserService adminUserService,
			AdminUserApiMapper mapper,
			AuthenticatedActorResolver actorResolver) {
		this.adminUserService = adminUserService;
		this.mapper = mapper;
		this.actorResolver = actorResolver;
	}

	@PostMapping
	public ResponseEntity<AdminUserCredentialResponse> createUser(@Valid @RequestBody CreateAdminUserRequest request) {
		UUID actorUserId = actorResolver.requireActorUserId();
		AdminUserService.ManagedUserSummary created = adminUserService.createManagedUser(
				new AdminUserService.CreateManagedUserCommand(
						actorUserId,
						request.username(),
						request.email(),
						request.temporaryPassword(),
						request.fullName(),
						request.avatarFileId(),
						request.roleCodes()));
		return ResponseEntity
				.created(URI.create("/api/admin/users/" + created.id()))
				.body(mapper.toCredentialResponse(created));
	}

	@GetMapping("/{userId}")
	public AdminUserResponse getUser(@PathVariable UUID userId) {
		return mapper.toUserResponse(adminUserService.findUserById(actorResolver.requireActorUserId(), userId));
	}

	@GetMapping("/by-email")
	public AdminUserResponse findUserByEmail(
			@RequestParam String email) {
		return mapper.toUserResponse(adminUserService.findUserByEmail(actorResolver.requireActorUserId(), email));
	}

	@GetMapping
	public List<AdminUserResponse> listUsers(
			@RequestParam(required = false) UserAccountStatus status) {
		return adminUserService.listUsers(actorResolver.requireActorUserId(), status)
				.stream()
				.map(mapper::toUserResponse)
				.toList();
	}

	@PatchMapping("/{userId}")
	public AdminUserResponse updateUser(
			@PathVariable UUID userId,
			@Valid @RequestBody UpdateAdminUserRequest request) {
		return updateUserInternal(userId, request);
	}

	@PutMapping("/{userId}")
	public AdminUserResponse replaceUser(
			@PathVariable UUID userId,
			@Valid @RequestBody UpdateAdminUserRequest request) {
		return updateUserInternal(userId, request);
	}

	private AdminUserResponse updateUserInternal(UUID userId, UpdateAdminUserRequest request) {
		return mapper.toUserResponse(adminUserService.updateManagedUser(
				new AdminUserService.UpdateManagedUserCommand(
						actorResolver.requireActorUserId(),
						userId,
						request.username(),
						request.email(),
						request.fullName(),
						request.avatarFileId(),
						request.clearAvatarFile())));
	}

	@PatchMapping("/{userId}/status")
	public AdminUserResponse changeUserStatus(
			@PathVariable UUID userId,
			@Valid @RequestBody ChangeUserStatusRequest request) {
		return changeUserStatusInternal(userId, request.status());
	}

	@PatchMapping("/{userId}/lock")
	public AdminUserResponse lockUser(@PathVariable UUID userId) {
		return changeUserStatusInternal(userId, UserAccountStatus.LOCKED);
	}

	@PatchMapping("/{userId}/unlock")
	public AdminUserResponse unlockUser(@PathVariable UUID userId) {
		return changeUserStatusInternal(userId, UserAccountStatus.ACTIVE);
	}

	@PatchMapping("/{userId}/reset-password")
	public AdminUserCredentialResponse resetPassword(
			@PathVariable UUID userId,
			@Valid @RequestBody ResetUserPasswordRequest request) {
		return mapper.toCredentialResponse(adminUserService.resetTemporaryPassword(
				new AdminUserService.ResetTemporaryPasswordCommand(
						actorResolver.requireActorUserId(),
						userId,
						request.temporaryPassword())));
	}

	@DeleteMapping("/{userId}")
	public ResponseEntity<Void> softDeleteUser(@PathVariable UUID userId) {
		adminUserService.softDeleteUser(new AdminUserService.SoftDeleteUserCommand(
				actorResolver.requireActorUserId(),
				userId));
		return ResponseEntity.noContent().build();
	}

	private AdminUserResponse changeUserStatusInternal(UUID userId, UserAccountStatus status) {
		return mapper.toUserResponse(adminUserService.changeAccountStatus(
				new AdminUserService.ChangeAccountStatusCommand(
						actorResolver.requireActorUserId(),
						userId,
						status)));
	}
}

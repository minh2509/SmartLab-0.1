package com.smartlab.service.admin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
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
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.UserRoleRepository;

@Service
@Profile("!nodb")
public class AdminUserService {

	private final UserRepository userRepository;
	private final FileRepository fileRepository;
	private final UserRoleRepository userRoleRepository;
	private final PasswordEncoder passwordEncoder;
	private final AdminRolePolicy rolePolicy;

	public AdminUserService(
			UserRepository userRepository,
			FileRepository fileRepository,
			UserRoleRepository userRoleRepository,
			PasswordEncoder passwordEncoder,
			AdminRolePolicy rolePolicy) {
		this.userRepository = userRepository;
		this.fileRepository = fileRepository;
		this.userRoleRepository = userRoleRepository;
		this.passwordEncoder = passwordEncoder;
		this.rolePolicy = rolePolicy;
	}

	@Transactional
	public ManagedUserSummary createManagedUser(CreateManagedUserCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Create managed user command must not be null.");
		}

		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		Lab lab = actor.lab();
		String username = requireTrimmed(command.username(), "Username");
		String email = normalizeRequiredEmail(command.email());
		String temporaryPassword = temporaryPasswordOrGenerated(command.temporaryPassword());
		String fullName = requireTrimmed(command.fullName(), "Full name");
		List<String> roleCodes = rolePolicy.normalizeRoleCodes(command.roleCodes());
		rolePolicy.assertAssignableRoleCodes(actor, roleCodes);
		List<Role> roles = rolePolicy.resolveRolesByCodes(roleCodes);

		if (userRepository.existsByLabAndEmail(lab, email)) {
			throw new DuplicateUserEmailException("User email already exists in the lab.");
		}
		if (userRepository.existsByLabAndUsername(lab, username)) {
			throw new DuplicateUsernameException("Username already exists in the lab.");
		}

		User user = new User();
		user.setLab(lab);
		user.setUsername(username);
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
		user.setFullName(fullName);
		user.setAccountStatus(UserAccountStatus.ACTIVE);
		if (command.avatarFileId() != null) {
			user.setAvatarFile(findFile(command.avatarFileId()));
		}

		User saved = userRepository.save(user);
		assignInitialRoles(saved, roles, actor.actor());
		return ManagedUserSummary.from(saved, roleCodes)
				.withTemporaryPassword(temporaryPassword, isBlank(command.temporaryPassword()));
	}

	@Transactional
	public ManagedUserSummary updateManagedUser(UpdateManagedUserCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Update managed user command must not be null.");
		}

		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		User user = rolePolicy.findUserInActorLab(actor, command.userId());
		rolePolicy.assertCanMutateTarget(actor, user);
		if (command.username() != null) {
			String username = requireTrimmed(command.username(), "Username");
			if (!username.equals(user.getUsername())
					&& userRepository.existsByLabAndUsernameAndIdNot(user.getLab(), username, user.getId())) {
				throw new DuplicateUsernameException("Username already exists in the lab.");
			}
			user.setUsername(username);
		}
		if (command.email() != null) {
			String email = normalizeRequiredEmail(command.email());
			if (!email.equals(user.getEmail())
					&& userRepository.existsByLabAndEmailAndIdNot(user.getLab(), email, user.getId())) {
				throw new DuplicateUserEmailException("User email already exists in the lab.");
			}
			user.setEmail(email);
		}
		if (command.fullName() != null) {
			user.setFullName(requireTrimmed(command.fullName(), "Full name"));
		}
		if (command.clearAvatarFile()) {
			user.setAvatarFile(null);
		} else if (command.avatarFileId() != null) {
			user.setAvatarFile(findFile(command.avatarFileId()));
		}

		return ManagedUserSummary.from(user, rolePolicy.activeRoleCodes(user));
	}

	@Transactional
	public ManagedUserSummary changeAccountStatus(ChangeAccountStatusCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Change account status command must not be null.");
		}
		if (command.status() == null) {
			throw new InvalidAdminServiceInputException("Account status must not be null.");
		}
		if (command.status() != UserAccountStatus.ACTIVE && command.status() != UserAccountStatus.LOCKED) {
			throw new InvalidAdminServiceInputException("Only ACTIVE and LOCKED account statuses can be set here.");
		}

		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		User user = rolePolicy.findUserInActorLab(actor, command.userId());
		rolePolicy.assertCanMutateTarget(actor, user);
		if (command.status() == user.getAccountStatus()) {
			return ManagedUserSummary.from(user, rolePolicy.activeRoleCodes(user));
		}
		user.setAccountStatus(command.status());
		return ManagedUserSummary.from(user, rolePolicy.activeRoleCodes(user));
	}

	@Transactional
	public ManagedUserSummary resetTemporaryPassword(ResetTemporaryPasswordCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Reset password command must not be null.");
		}
		String temporaryPassword = temporaryPasswordOrGenerated(command.temporaryPassword());
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		User user = rolePolicy.findUserInActorLab(actor, command.userId());
		rolePolicy.assertCanMutateTarget(actor, user);
		user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
		return ManagedUserSummary.from(user, rolePolicy.activeRoleCodes(user))
				.withTemporaryPassword(temporaryPassword, isBlank(command.temporaryPassword()));
	}

	@Transactional
	public void softDeleteUser(SoftDeleteUserCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Soft delete user command must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		User user = rolePolicy.findUserInActorLab(actor, command.userId());
		rolePolicy.assertCanMutateTarget(actor, user);
		user.setAccountStatus(UserAccountStatus.DELETED);
		user.setDeletedAt(OffsetDateTime.now());
		user.setDeletedBy(actor.actor());
	}

	@Transactional(readOnly = true)
	public ManagedUserSummary findUserById(UUID actorUserId, UUID userId) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		User user = rolePolicy.findUserInActorLab(actor, userId);
		return ManagedUserSummary.from(user, rolePolicy.activeRoleCodes(user));
	}

	@Transactional(readOnly = true)
	public ManagedUserSummary findUserByEmail(UUID actorUserId, String email) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		return userRepository.findByLabAndEmail(actor.lab(), normalizeRequiredEmail(email))
				.map(user -> ManagedUserSummary.from(user, rolePolicy.activeRoleCodes(user)))
				.orElseThrow(() -> new ResourceNotFoundException("User was not found."));
	}

	@Transactional(readOnly = true)
	public List<ManagedUserSummary> listUsers(UUID actorUserId, UserAccountStatus accountStatus) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		List<User> users = accountStatus == null
				? userRepository.findByLabAndAccountStatusNot(actor.lab(), UserAccountStatus.DELETED)
				: userRepository.findByLabAndAccountStatus(actor.lab(), accountStatus);
		return summariesWithActiveRoles(users);
	}

	private List<ManagedUserSummary> summariesWithActiveRoles(List<User> users) {
		Map<UUID, List<String>> roleCodesByUserId = rolePolicy.activeRoleCodesByUserId(users);
		return users.stream()
				.map(user -> ManagedUserSummary.from(
						user,
						roleCodesByUserId.getOrDefault(user.getId(), List.of())))
				.toList();
	}

	private void assignInitialRoles(User user, List<Role> roles, User actor) {
		for (Role role : roles) {
			UserRole assignment = userRoleRepository.findByUserAndRole(user, role)
					.orElseGet(() -> newAssignment(user, role));
			assignment.setStatus(UserRoleStatus.ACTIVE);
			assignment.setAssignedBy(actor);
			if (assignment.getId() == null) {
				userRoleRepository.save(assignment);
			}
		}
	}

	private static UserRole newAssignment(User user, Role role) {
		UserRole assignment = new UserRole();
		assignment.setUser(user);
		assignment.setRole(role);
		return assignment;
	}

	private File findFile(UUID fileId) {
		return fileRepository.findById(fileId)
				.orElseThrow(() -> new ResourceNotFoundException("Avatar file was not found."));
	}

	private static String requireTemporaryPassword(String value) {
		if (value == null || value.isBlank()) {
			throw new InvalidAdminServiceInputException("Temporary password must not be blank.");
		}
		if (value.length() < 12 || value.length() > 72) {
			throw new InvalidAdminServiceInputException("Temporary password must be between 12 and 72 characters.");
		}
		return value;
	}

	private static String temporaryPasswordOrGenerated(String value) {
		if (!isBlank(value)) {
			return requireTemporaryPassword(value.trim());
		}
		return "SL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 18) + "!aA7";
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isBlank();
	}

	private static String normalizeRequiredEmail(String email) {
		return requireTrimmed(email, "Email").toLowerCase(Locale.ROOT);
	}

	private static String requireTrimmed(String value, String fieldName) {
		if (value == null || value.trim().isBlank()) {
			throw new InvalidAdminServiceInputException(fieldName + " must not be blank.");
		}
		return value.trim();
	}

	public record CreateManagedUserCommand(
			UUID actorUserId,
			String username,
			String email,
			String temporaryPassword,
			String fullName,
			UUID avatarFileId,
			List<String> roleCodes) {
	}

	public record UpdateManagedUserCommand(
			UUID actorUserId,
			UUID userId,
			String username,
			String email,
			String fullName,
			UUID avatarFileId,
			boolean clearAvatarFile) {
	}

	public record ChangeAccountStatusCommand(
			UUID actorUserId,
			UUID userId,
			UserAccountStatus status) {
	}

	public record ResetTemporaryPasswordCommand(
			UUID actorUserId,
			UUID userId,
			String temporaryPassword) {
	}

	public record SoftDeleteUserCommand(
			UUID actorUserId,
			UUID userId) {
	}

	public record ManagedUserSummary(
			UUID id,
			UUID labId,
			String username,
			String email,
			String fullName,
			UUID avatarFileId,
			UserAccountStatus accountStatus,
			OffsetDateTime lastLoginAt,
			List<String> roleCodes,
			String temporaryPassword,
			boolean temporaryPasswordGenerated) {

		static ManagedUserSummary from(User user, List<String> roleCodes) {
			UUID labId = user.getLab() == null ? null : user.getLab().getId();
			UUID avatarFileId = user.getAvatarFile() == null ? null : user.getAvatarFile().getId();
			return new ManagedUserSummary(
					user.getId(),
					labId,
					user.getUsername(),
					user.getEmail(),
					user.getFullName(),
					avatarFileId,
					user.getAccountStatus(),
					user.getLastLoginAt(),
					roleCodes.stream().distinct().sorted().toList(),
					null,
					false);
		}

		ManagedUserSummary withTemporaryPassword(String temporaryPassword, boolean generated) {
			return new ManagedUserSummary(
					id,
					labId,
					username,
					email,
					fullName,
					avatarFileId,
					accountStatus,
					lastLoginAt,
					roleCodes,
					temporaryPassword,
					generated);
		}
	}
}

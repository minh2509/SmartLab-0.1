package com.smartlab.service.admin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
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

@Service
@Profile("!nodb")
public class AdminUserService {

	private final UserRepository userRepository;
	private final LabRepository labRepository;
	private final FileRepository fileRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;

	public AdminUserService(
			UserRepository userRepository,
			LabRepository labRepository,
			FileRepository fileRepository,
			RoleRepository roleRepository,
			UserRoleRepository userRoleRepository) {
		this.userRepository = userRepository;
		this.labRepository = labRepository;
		this.fileRepository = fileRepository;
		this.roleRepository = roleRepository;
		this.userRoleRepository = userRoleRepository;
	}

	@Transactional
	public ManagedUserSummary createManagedUser(CreateManagedUserCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Create managed user command must not be null.");
		}

		Lab lab = findLab(command.labId());
		String username = requireTrimmed(command.username(), "Username");
		String email = normalizeRequiredEmail(command.email());
		String passwordHash = requireTrimmed(command.passwordHash(), "Password hash");
		String fullName = requireTrimmed(command.fullName(), "Full name");
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
		user.setPasswordHash(passwordHash);
		user.setFullName(fullName);
		user.setAccountStatus(UserAccountStatus.ACTIVE);
		if (command.avatarFileId() != null) {
			user.setAvatarFile(findFile(command.avatarFileId()));
		}

		return ManagedUserSummary.from(userRepository.save(user));
	}

	@Transactional
	public ManagedUserSummary updateManagedUser(UpdateManagedUserCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Update managed user command must not be null.");
		}

		User user = findUser(command.userId());
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

		return ManagedUserSummary.from(user);
	}

	@Transactional
	public ManagedUserSummary changeAccountStatus(UUID userId, UserAccountStatus status) {
		if (status == null) {
			throw new InvalidAdminServiceInputException("Account status must not be null.");
		}

		User user = findUser(userId);
		if (status == user.getAccountStatus()) {
			return ManagedUserSummary.from(user);
		}
		if (status != UserAccountStatus.ACTIVE) {
			protectFinalActiveSuperAdminAccount(user);
		}
		user.setAccountStatus(status);
		return ManagedUserSummary.from(user);
	}

	@Transactional
	public ManagedUserSummary lockUser(UUID userId) {
		if (findUser(userId).getAccountStatus() == UserAccountStatus.DELETED) {
			throw new InvalidAdminServiceInputException("Deleted users cannot be locked.");
		}
		return changeAccountStatus(userId, UserAccountStatus.LOCKED);
	}

	@Transactional
	public ManagedUserSummary unlockUser(UUID userId) {
		if (findUser(userId).getAccountStatus() == UserAccountStatus.DELETED) {
			throw new InvalidAdminServiceInputException("Deleted users cannot be unlocked.");
		}
		return changeAccountStatus(userId, UserAccountStatus.ACTIVE);
	}

	@Transactional
	public ManagedUserSummary resetPassword(UUID userId, String passwordHash) {
		User user = findUser(userId);
		user.setPasswordHash(requireTrimmed(passwordHash, "Password hash"));
		return ManagedUserSummary.from(user);
	}

	@Transactional
	public ManagedUserSummary softDeleteUser(UUID userId, UUID deletedByUserId) {
		User user = findUser(userId);
		if (user.getAccountStatus() == UserAccountStatus.DELETED) {
			return ManagedUserSummary.from(user);
		}
		protectFinalActiveSuperAdminAccount(user);
		user.setAccountStatus(UserAccountStatus.DELETED);
		user.setDeletedAt(OffsetDateTime.now());
		if (deletedByUserId != null) {
			user.setDeletedBy(findUser(deletedByUserId));
		}
		return ManagedUserSummary.from(user);
	}

	@Transactional(readOnly = true)
	public Optional<ManagedUserSummary> findUserById(UUID userId) {
		if (userId == null) {
			throw new InvalidAdminServiceInputException("User ID must not be null.");
		}
		return userRepository.findById(userId).map(ManagedUserSummary::from);
	}

	@Transactional(readOnly = true)
	public Optional<ManagedUserSummary> findUserByLabAndEmail(UUID labId, String email) {
		Lab lab = findLab(labId);
		return userRepository.findByLabAndEmail(lab, normalizeRequiredEmail(email)).map(ManagedUserSummary::from);
	}

	@Transactional(readOnly = true)
	public List<ManagedUserSummary> listUsersByLab(UUID labId) {
		return userRepository.findByLab(findLab(labId)).stream().map(ManagedUserSummary::from).toList();
	}

	@Transactional(readOnly = true)
	public List<ManagedUserSummary> listUsersByAccountStatus(UserAccountStatus accountStatus) {
		if (accountStatus == null) {
			throw new InvalidAdminServiceInputException("Account status must not be null.");
		}
		return userRepository.findByAccountStatus(accountStatus).stream().map(ManagedUserSummary::from).toList();
	}

	@Transactional(readOnly = true)
	public List<ManagedUserSummary> listUsersByLabAndAccountStatus(UUID labId, UserAccountStatus accountStatus) {
		if (accountStatus == null) {
			throw new InvalidAdminServiceInputException("Account status must not be null.");
		}
		return userRepository.findByLabAndAccountStatus(findLab(labId), accountStatus)
				.stream()
				.map(ManagedUserSummary::from)
				.toList();
	}

	private void protectFinalActiveSuperAdminAccount(User user) {
		Optional<Role> protectedRole = roleRepository.findByCode(AdminUserRoleService.SUPER_ADMIN_ROLE_CODE);
		if (protectedRole.isEmpty()) {
			return;
		}
		Role superAdmin = protectedRole.get();
		boolean hasProtectedRole = userRoleRepository.existsByUserAndRoleAndStatus(
				user,
				superAdmin,
				UserRoleStatus.ACTIVE);
		if (!hasProtectedRole) {
			return;
		}
		long activeSuperAdmins = userRoleRepository.countByRoleAndStatusAndUserAccountStatus(
				superAdmin,
				UserRoleStatus.ACTIVE,
				UserAccountStatus.ACTIVE);
		if (activeSuperAdmins <= 1) {
			throw new ProtectedAdministratorOperationException("Cannot disable the final active SUPER_ADMIN account.");
		}
	}

	private User findUser(UUID userId) {
		if (userId == null) {
			throw new InvalidAdminServiceInputException("User ID must not be null.");
		}
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User was not found."));
	}

	private Lab findLab(UUID labId) {
		if (labId == null) {
			throw new InvalidAdminServiceInputException("Lab ID must not be null.");
		}
		return labRepository.findById(labId)
				.orElseThrow(() -> new ResourceNotFoundException("Lab was not found."));
	}

	private File findFile(UUID fileId) {
		return fileRepository.findById(fileId)
				.orElseThrow(() -> new ResourceNotFoundException("Avatar file was not found."));
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
			UUID labId,
			String username,
			String email,
			String passwordHash,
			String fullName,
			UUID avatarFileId) {
	}

	public record UpdateManagedUserCommand(
			UUID userId,
			String username,
			String email,
			String fullName,
			UUID avatarFileId,
			boolean clearAvatarFile) {
	}

	public record ManagedUserSummary(
			UUID id,
			UUID labId,
			String username,
			String email,
			String fullName,
			UUID avatarFileId,
			UserAccountStatus accountStatus) {

		static ManagedUserSummary from(User user) {
			UUID labId = user.getLab() == null ? null : user.getLab().getId();
			UUID avatarFileId = user.getAvatarFile() == null ? null : user.getAvatarFile().getId();
			return new ManagedUserSummary(
					user.getId(),
					labId,
					user.getUsername(),
					user.getEmail(),
					user.getFullName(),
					avatarFileId,
					user.getAccountStatus());
		}
	}
}

package com.smartlab.service.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.smartlab.entity.Lab;
import com.smartlab.entity.Role;
import com.smartlab.entity.User;
import com.smartlab.entity.UserRole;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;
import com.smartlab.exception.ForbiddenAdminOperationException;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.RoleRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.UserRoleRepository;

@Component
@Profile("!nodb")
class AdminRolePolicy {

	static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";
	static final String ADMIN_ROLE_CODE = "ADMIN";
	static final String LEADER_ROLE_CODE = "LEADER";
	static final String MEMBER_ROLE_CODE = "MEMBER";

	private static final Set<String> SYSTEM_ROLE_CODES = Set.of(
			SUPER_ADMIN_ROLE_CODE,
			ADMIN_ROLE_CODE,
			LEADER_ROLE_CODE,
			MEMBER_ROLE_CODE);

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;

	AdminRolePolicy(
			UserRepository userRepository,
			RoleRepository roleRepository,
			UserRoleRepository userRoleRepository) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userRoleRepository = userRoleRepository;
	}

	ActorContext requireAdminActor(UUID actorUserId) {
		if (actorUserId == null) {
			throw new InvalidAdminServiceInputException("Authenticated actor ID must not be null.");
		}
		User actor = userRepository.findById(actorUserId)
				.orElseThrow(() -> new ForbiddenAdminOperationException("Authenticated actor cannot manage users."));
		if (actor.getAccountStatus() != UserAccountStatus.ACTIVE) {
			throw new ForbiddenAdminOperationException("Authenticated actor cannot manage users.");
		}
		List<String> roleCodes = activeRoleCodes(actor);
		if (!roleCodes.contains(SUPER_ADMIN_ROLE_CODE) && !roleCodes.contains(ADMIN_ROLE_CODE)) {
			throw new ForbiddenAdminOperationException("Authenticated actor cannot manage users.");
		}
		return new ActorContext(actor, Set.copyOf(roleCodes));
	}

	User findUserInActorLab(ActorContext actor, UUID userId) {
		if (userId == null) {
			throw new InvalidAdminServiceInputException("User ID must not be null.");
		}
		return userRepository.findByIdAndLab(userId, actor.lab())
				.orElseThrow(() -> new ResourceNotFoundException("User was not found."));
	}

	void assertCanMutateTarget(ActorContext actor, User target) {
		if (target == null) {
			throw new ResourceNotFoundException("User was not found.");
		}
		if (!sameLab(actor.actor(), target)) {
			throw new ResourceNotFoundException("User was not found.");
		}
		if (sameUser(actor.actor(), target)) {
			throw new ForbiddenAdminOperationException("Administrators cannot modify their own account.");
		}
		List<String> targetRoleCodes = activeRoleCodes(target);
		if (targetRoleCodes.contains(SUPER_ADMIN_ROLE_CODE)) {
			throw new ForbiddenAdminOperationException("SUPER_ADMIN accounts cannot be managed through this API.");
		}
		if (!actor.isSuperAdmin() && targetRoleCodes.contains(ADMIN_ROLE_CODE)) {
			throw new ForbiddenAdminOperationException("Regular ADMIN users cannot modify ADMIN accounts.");
		}
	}

	List<String> normalizeRoleCodes(Collection<String> roleCodes) {
		if (roleCodes == null || roleCodes.isEmpty()) {
			throw new InvalidAdminServiceInputException("At least one role code must be provided.");
		}
		LinkedHashSet<String> normalized = new LinkedHashSet<>();
		for (String roleCode : roleCodes) {
			if (roleCode == null || roleCode.trim().isBlank()) {
				throw new InvalidAdminServiceInputException("Role code must not be blank.");
			}
			normalized.add(roleCode.trim().toUpperCase(Locale.ROOT));
		}
		return normalized.stream().sorted().toList();
	}

	List<Role> resolveRolesByCodes(Collection<String> normalizedRoleCodes) {
		List<String> roleCodes = new ArrayList<>(normalizedRoleCodes);
		if (roleCodes.isEmpty()) {
			throw new InvalidAdminServiceInputException("At least one role code must be provided.");
		}
		Map<String, Role> rolesByCode = roleRepository.findByCodeIn(roleCodes)
				.stream()
				.collect(Collectors.toMap(Role::getCode, Function.identity()));
		List<String> missing = roleCodes.stream()
				.filter(roleCode -> !rolesByCode.containsKey(roleCode))
				.toList();
		if (!missing.isEmpty()) {
			throw new ResourceNotFoundException("Role was not found.");
		}
		return roleCodes.stream().map(rolesByCode::get).toList();
	}

	void assertAssignableRoleCodes(ActorContext actor, Collection<String> normalizedRoleCodes) {
		for (String roleCode : normalizedRoleCodes) {
			if (SUPER_ADMIN_ROLE_CODE.equals(roleCode)) {
				throw new ForbiddenAdminOperationException("SUPER_ADMIN cannot be assigned through this API.");
			}
			if (ADMIN_ROLE_CODE.equals(roleCode) && !actor.isSuperAdmin()) {
				throw new ForbiddenAdminOperationException("Regular ADMIN users cannot assign ADMIN.");
			}
			if (!SYSTEM_ROLE_CODES.contains(roleCode)) {
				throw new InvalidAdminServiceInputException("Unsupported role code.");
			}
		}
	}

	boolean isRoleAssignable(ActorContext actor, String roleCode) {
		if (SUPER_ADMIN_ROLE_CODE.equals(roleCode)) {
			return false;
		}
		if (ADMIN_ROLE_CODE.equals(roleCode)) {
			return actor.isSuperAdmin();
		}
		return LEADER_ROLE_CODE.equals(roleCode) || MEMBER_ROLE_CODE.equals(roleCode);
	}

	boolean isSystemRole(String roleCode) {
		return SYSTEM_ROLE_CODES.contains(roleCode);
	}

	List<String> activeRoleCodes(User user) {
		return userRoleRepository.findByUserAndStatus(user, UserRoleStatus.ACTIVE)
				.stream()
				.map(UserRole::getRole)
				.map(Role::getCode)
				.distinct()
				.sorted()
				.toList();
	}

	Map<UUID, List<String>> activeRoleCodesByUserId(List<User> users) {
		if (users.isEmpty()) {
			return Map.of();
		}
		return userRoleRepository.findByUserInAndStatus(users, UserRoleStatus.ACTIVE)
				.stream()
				.collect(Collectors.groupingBy(
						userRole -> userRole.getUser().getId(),
						Collectors.mapping(userRole -> userRole.getRole().getCode(), Collectors.collectingAndThen(
								Collectors.toCollection(LinkedHashSet::new),
								codes -> codes.stream().sorted().toList()))));
	}

	List<Role> sortedSystemRoles() {
		return roleRepository.findAll()
				.stream()
				.filter(role -> isSystemRole(role.getCode()))
				.sorted(Comparator.comparing(Role::getCode))
				.toList();
	}

	private static boolean sameLab(User first, User second) {
		if (first == null || second == null || first.getLab() == null || second.getLab() == null) {
			return false;
		}
		UUID firstLabId = first.getLab().getId();
		UUID secondLabId = second.getLab().getId();
		return firstLabId != null && firstLabId.equals(secondLabId);
	}

	private static boolean sameUser(User first, User second) {
		if (first == null || second == null) {
			return false;
		}
		UUID firstUserId = first.getId();
		UUID secondUserId = second.getId();
		return firstUserId != null && firstUserId.equals(secondUserId);
	}

	record ActorContext(User actor, Set<String> activeRoleCodes) {

		boolean isSuperAdmin() {
			return activeRoleCodes.contains(SUPER_ADMIN_ROLE_CODE);
		}

		Lab lab() {
			return actor.getLab();
		}
	}
}

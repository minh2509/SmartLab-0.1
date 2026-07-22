package com.smartlab.service.admin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.smartlab.entity.Lab;
import com.smartlab.entity.User;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;
import com.smartlab.exception.ForbiddenAdminOperationException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.RoleRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.UserRoleRepository;

class AdminRolePolicyTests {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final RoleRepository roleRepository = mock(RoleRepository.class);
	private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
	private final AdminRolePolicy policy = new AdminRolePolicy(userRepository, roleRepository, userRoleRepository);

	@Test
	void sameLabMutationAllowsDifferentLabObjectsWithSameUuid() {
		UUID labId = UUID.randomUUID();
		User actor = user(UUID.randomUUID(), lab(labId));
		User target = user(UUID.randomUUID(), lab(labId));
		when(userRoleRepository.findByUserAndStatus(target, UserRoleStatus.ACTIVE)).thenReturn(java.util.List.of());

		assertDoesNotThrow(() -> policy.assertCanMutateTarget(adminActor(actor), target));
	}

	@Test
	void crossLabMutationThrowsNotFound() {
		User actor = user(UUID.randomUUID(), lab(UUID.randomUUID()));
		User target = user(UUID.randomUUID(), lab(UUID.randomUUID()));

		assertThrows(ResourceNotFoundException.class, () -> policy.assertCanMutateTarget(adminActor(actor), target));
	}

	@Test
	void selfMutationUsesUserIdNotObjectIdentity() {
		UUID labId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		User actor = user(userId, lab(labId));
		User target = user(userId, lab(labId));

		assertThrows(
				ForbiddenAdminOperationException.class,
				() -> policy.assertCanMutateTarget(adminActor(actor), target));
	}

	private static AdminRolePolicy.ActorContext adminActor(User actor) {
		return new AdminRolePolicy.ActorContext(actor, Set.of(AdminUserRoleService.ADMIN_ROLE_CODE));
	}

	private static Lab lab(UUID id) {
		Lab lab = new Lab();
		lab.setId(id);
		return lab;
	}

	private static User user(UUID id, Lab lab) {
		User user = new User();
		user.setId(id);
		user.setLab(lab);
		user.setUsername("user");
		user.setEmail("user@example.edu");
		user.setFullName("User");
		user.setPasswordHash("hash");
		user.setAccountStatus(UserAccountStatus.ACTIVE);
		return user;
	}
}

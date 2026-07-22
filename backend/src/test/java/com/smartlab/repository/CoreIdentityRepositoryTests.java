package com.smartlab.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.File;
import com.smartlab.entity.Lab;
import com.smartlab.entity.Permission;
import com.smartlab.entity.Role;
import com.smartlab.entity.RolePermission;
import com.smartlab.entity.User;
import com.smartlab.entity.UserRole;

class CoreIdentityRepositoryTests {

	@Test
	void repositoriesUseUuidJpaRepositoryContracts() {
		assertJpaRepository(LabRepository.class);
		assertJpaRepository(RoleRepository.class);
		assertJpaRepository(PermissionRepository.class);
		assertJpaRepository(UserRepository.class);
		assertJpaRepository(FileRepository.class);
		assertJpaRepository(UserRoleRepository.class);
		assertJpaRepository(RolePermissionRepository.class);
	}

	@Test
	void repositoriesExposeOnlyFocusedCoreIdentityQueries() throws NoSuchMethodException {
		assertReturnType(LabRepository.class.getMethod("findByCode", String.class), Optional.class);
		assertReturnType(RoleRepository.class.getMethod("findByCode", String.class), Optional.class);
		assertReturnType(PermissionRepository.class.getMethod("findByCode", String.class), Optional.class);
		assertReturnType(UserRepository.class.getMethod("findByLabAndUsername", Lab.class, String.class), Optional.class);
		assertReturnType(UserRepository.class.getMethod("findByLabAndEmail", Lab.class, String.class), Optional.class);
		assertReturnType(
				UserRoleRepository.class.getMethod("existsByUserAndRole", User.class, Role.class),
				boolean.class);
		assertReturnType(
				RolePermissionRepository.class.getMethod(
						"existsByRoleAndPermission",
						Role.class,
						Permission.class),
				boolean.class);
		assertReturnType(
				RolePermissionRepository.class.getMethod("findByRoleIn", Collection.class),
				java.util.List.class);
	}

	private static void assertJpaRepository(Class<?> repositoryType) {
		assertTrue(JpaRepository.class.isAssignableFrom(repositoryType));
		assertEquals(1, countEntitySpecificRepositoryInterfaces(repositoryType));
	}

	private static long countEntitySpecificRepositoryInterfaces(Class<?> repositoryType) {
		return java.util.Arrays.stream(repositoryType.getGenericInterfaces())
				.filter(type -> type.getTypeName().contains("<"))
				.filter(type -> type.getTypeName().contains(UUID.class.getName()))
				.filter(type -> type.getTypeName().contains(entityNameFor(repositoryType)))
				.count();
	}

	private static String entityNameFor(Class<?> repositoryType) {
		if (repositoryType == LabRepository.class) {
			return Lab.class.getName();
		}
		if (repositoryType == RoleRepository.class) {
			return Role.class.getName();
		}
		if (repositoryType == PermissionRepository.class) {
			return Permission.class.getName();
		}
		if (repositoryType == UserRepository.class) {
			return User.class.getName();
		}
		if (repositoryType == FileRepository.class) {
			return File.class.getName();
		}
		if (repositoryType == UserRoleRepository.class) {
			return UserRole.class.getName();
		}
		if (repositoryType == RolePermissionRepository.class) {
			return RolePermission.class.getName();
		}
		throw new IllegalArgumentException("Unsupported repository type: " + repositoryType.getName());
	}

	private static void assertReturnType(Method method, Class<?> returnType) {
		assertEquals(returnType, method.getReturnType());
	}
}

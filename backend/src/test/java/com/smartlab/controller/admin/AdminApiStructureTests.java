package com.smartlab.controller.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.request.admin.AssignAdminUserRoleRequest;
import com.smartlab.dto.request.admin.CreateAdminUserRequest;
import com.smartlab.dto.request.admin.UpdateAdminUserRequest;
import com.smartlab.dto.response.admin.AdminPermissionResponse;
import com.smartlab.dto.response.admin.AdminRoleResponse;
import com.smartlab.dto.response.admin.AdminSystemRoleResponse;
import com.smartlab.dto.response.admin.AdminUserResponse;
import com.smartlab.dto.response.admin.AdminUserRoleResponse;

class AdminApiStructureTests {

	@Test
	void controllersAreRestAdaptersWithoutRepositoryDependenciesAndInactiveInNodb() {
		assertControllerBoundary(AdminUserController.class);
		assertControllerBoundary(AdminUserRoleController.class);
		assertControllerBoundary(AdminRoleController.class);
	}

	@Test
	void requestAndResponseDtosDoNotExposePlaintextPasswordsOrPasswordHashesInResponses() {
		assertTrue(hasRecordComponent(CreateAdminUserRequest.class, "passwordHash"));
		assertFalse(hasRecordComponent(CreateAdminUserRequest.class, "password"));
		assertFalse(hasRecordComponent(UpdateAdminUserRequest.class, "password"));
		assertNoCredentialComponent(AdminUserResponse.class);
		assertNoCredentialComponent(AdminRoleResponse.class);
		assertNoCredentialComponent(AdminSystemRoleResponse.class);
		assertNoCredentialComponent(AdminPermissionResponse.class);
		assertNoCredentialComponent(AdminUserRoleResponse.class);
	}

	@Test
	void adminRoleAndPermissionContractHasNoPermissionMutationEndpoints() {
		assertTrue(hasMethodWithAnnotation(AdminUserRoleController.class, PostMapping.class));
		assertFalse(hasMutationMappingContainingPermission(AdminUserController.class));
		assertFalse(hasMutationMappingContainingPermission(AdminUserRoleController.class));
		assertFalse(hasMutationMappingContainingPermission(AdminRoleController.class));
		assertTrue(hasRecordComponent(AssignAdminUserRoleRequest.class, "roleId"));
	}

	private static void assertControllerBoundary(Class<?> controllerType) {
		assertNotNull(controllerType.getAnnotation(RestController.class));
		Profile profile = controllerType.getAnnotation(Profile.class);
		assertNotNull(profile);
		assertTrue(Arrays.asList(profile.value()).contains("!nodb"));
		for (Field field : controllerType.getDeclaredFields()) {
			assertFalse(field.getType().getName().contains(".repository."));
		}
	}

	private static boolean hasRecordComponent(Class<?> recordType, String componentName) {
		return Arrays.stream(recordType.getRecordComponents())
				.anyMatch(component -> component.getName().equals(componentName));
	}

	private static boolean hasMethodWithAnnotation(
			Class<?> controllerType,
			Class<? extends java.lang.annotation.Annotation> annotationType) {
		return Arrays.stream(controllerType.getDeclaredMethods())
				.anyMatch(method -> method.getAnnotation(annotationType) != null);
	}

	private static boolean hasMutationMappingContainingPermission(Class<?> controllerType) {
		List<Class<? extends java.lang.annotation.Annotation>> mutationAnnotations = List.of(
				PostMapping.class,
				PutMapping.class,
				PatchMapping.class,
				DeleteMapping.class);
		return Arrays.stream(controllerType.getDeclaredMethods())
				.filter(method -> mutationAnnotations.stream().anyMatch(annotation -> method.getAnnotation(annotation) != null))
				.anyMatch(AdminApiStructureTests::hasPermissionMappingPath);
	}

	private static boolean hasPermissionMappingPath(Method method) {
		return Arrays.stream(method.getAnnotations())
				.flatMap(annotation -> annotationPaths(annotation).stream())
				.map(String::toLowerCase)
				.anyMatch(path -> path.contains("permission"));
	}

	private static List<String> annotationPaths(java.lang.annotation.Annotation annotation) {
		if (annotation instanceof PostMapping mapping) {
			return concat(mapping.value(), mapping.path());
		}
		if (annotation instanceof PutMapping mapping) {
			return concat(mapping.value(), mapping.path());
		}
		if (annotation instanceof PatchMapping mapping) {
			return concat(mapping.value(), mapping.path());
		}
		if (annotation instanceof DeleteMapping mapping) {
			return concat(mapping.value(), mapping.path());
		}
		return List.of();
	}

	private static List<String> concat(String[] first, String[] second) {
		return java.util.stream.Stream.concat(Arrays.stream(first), Arrays.stream(second)).toList();
	}

	private static void assertNoCredentialComponent(Class<?> recordType) {
		assertFalse(Arrays.stream(recordType.getRecordComponents())
				.anyMatch(component -> {
					String name = component.getName().toLowerCase();
					return name.contains("password") || name.contains("credential") || name.contains("hash");
				}));
	}
}

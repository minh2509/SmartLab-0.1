package com.smartlab.controller.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.request.admin.CreateAdminUserRequest;
import com.smartlab.dto.request.admin.UpdateAdminUserRequest;
import com.smartlab.dto.response.admin.AdminRoleResponse;
import com.smartlab.dto.response.admin.AdminUserResponse;
import com.smartlab.dto.response.admin.AdminUserRoleResponse;

class AdminApiStructureTests {

	@Test
	void controllersAreRestAdaptersWithoutRepositoryDependenciesAndInactiveInNodb() {
		assertControllerBoundary(AdminUserController.class);
		assertControllerBoundary(AdminUserRoleController.class);
	}

	@Test
	void requestAndResponseDtosDoNotExposePlaintextPasswordsOrPasswordHashesInResponses() {
		assertTrue(hasRecordComponent(CreateAdminUserRequest.class, "passwordHash"));
		assertFalse(hasRecordComponent(CreateAdminUserRequest.class, "password"));
		assertFalse(hasRecordComponent(UpdateAdminUserRequest.class, "password"));
		assertNoCredentialComponent(AdminUserResponse.class);
		assertNoCredentialComponent(AdminRoleResponse.class);
		assertNoCredentialComponent(AdminUserRoleResponse.class);
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

	private static void assertNoCredentialComponent(Class<?> recordType) {
		assertFalse(Arrays.stream(recordType.getRecordComponents())
				.anyMatch(component -> {
					String name = component.getName().toLowerCase();
					return name.contains("password") || name.contains("credential") || name.contains("hash");
				}));
	}
}

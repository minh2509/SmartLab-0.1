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
import com.smartlab.dto.request.admin.ReplaceUserRolesRequest;
import com.smartlab.dto.request.admin.UpdateAdminUserRequest;
import com.smartlab.dto.response.admin.AdminRoleCatalogResponse;
import com.smartlab.dto.response.admin.AdminRoleResponse;
import com.smartlab.dto.response.admin.AdminPostDetailResponse;
import com.smartlab.dto.response.admin.AdminUserResponse;
import com.smartlab.dto.response.admin.AdminUserRoleResponse;

class AdminApiStructureTests {

	@Test
	void controllersAreRestAdaptersWithoutRepositoryDependenciesAndInactiveInNodb() {
		assertControllerBoundary(AdminPostController.class);
		assertControllerBoundary(AdminUserController.class);
		assertControllerBoundary(AdminRoleController.class);
		assertControllerBoundary(AdminUserRoleController.class);
	}

	@Test
	void requestAndResponseDtosKeepCredentialBoundaryAndRoleCodesExplicit() {
		assertTrue(hasRecordComponent(CreateAdminUserRequest.class, "temporaryPassword"));
		assertTrue(hasRecordComponent(CreateAdminUserRequest.class, "roleCodes"));
		assertFalse(hasRecordComponent(CreateAdminUserRequest.class, "passwordHash"));
		assertFalse(hasRecordComponent(CreateAdminUserRequest.class, "labId"));
		assertFalse(hasRecordComponent(CreateAdminUserRequest.class, "password"));
		assertTrue(hasRecordComponent(ReplaceUserRolesRequest.class, "roleCodes"));
		assertTrue(hasRecordComponent(AdminUserResponse.class, "roleCodes"));
		assertFalse(hasFieldOrRecordComponent(UpdateAdminUserRequest.class, "password"));
		assertNoCredentialComponent(AdminUserResponse.class);
		assertNoCredentialComponent(AdminRoleCatalogResponse.class);
		assertNoCredentialComponent(AdminRoleResponse.class);
		assertNoCredentialComponent(AdminUserRoleResponse.class);
		assertNoCredentialComponent(AdminPostDetailResponse.class);
		assertNoCredentialComponent(AdminPostDetailResponse.AuthorResponse.class);
		assertNoCredentialComponent(AdminPostDetailResponse.AttachmentResponse.class);
		assertNoCredentialComponent(AdminPostDetailResponse.ModerationHistoryResponse.class);
		assertFalse(hasRecordComponent(AdminPostDetailResponse.class, "reviewNote"));
		assertFalse(hasRecordComponent(AdminPostDetailResponse.FileResponse.class, "storedName"));
		assertFalse(hasRecordComponent(AdminPostDetailResponse.FileResponse.class, "storagePath"));
		assertFalse(hasRecordComponent(AdminPostDetailResponse.FileResponse.class, "deletedAt"));
		assertFalse(hasRecordComponent(AdminPostDetailResponse.AttachmentResponse.class, "storedName"));
		assertFalse(hasRecordComponent(AdminPostDetailResponse.AttachmentResponse.class, "storagePath"));
		assertFalse(hasRecordComponent(AdminPostDetailResponse.AttachmentResponse.class, "deletedAt"));
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

	private static boolean hasFieldOrRecordComponent(Class<?> type, String componentName) {
		if (type.isRecord()) {
			return hasRecordComponent(type, componentName);
		}
		return Arrays.stream(type.getDeclaredFields())
				.anyMatch(field -> field.getName().equals(componentName));
	}

	private static void assertNoCredentialComponent(Class<?> recordType) {
		assertFalse(Arrays.stream(recordType.getRecordComponents())
				.anyMatch(component -> {
					String name = component.getName().toLowerCase();
					return name.contains("password") || name.contains("credential") || name.contains("hash");
				}));
	}
}

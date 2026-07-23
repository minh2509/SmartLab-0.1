package com.smartlab.controller.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.request.admin.CreateAdminUserRequest;
import com.smartlab.dto.request.admin.ReplaceUserRolesRequest;
import com.smartlab.dto.request.admin.UpdateAdminUserRequest;
import com.smartlab.dto.response.admin.AdminPostPageResponse;
import com.smartlab.dto.response.admin.AdminPostDetailResponse;
import com.smartlab.dto.response.admin.AdminPostModerationActionResponse;
import com.smartlab.dto.response.admin.AdminPermissionResponse;
import com.smartlab.dto.response.admin.AdminRoleCatalogResponse;
import com.smartlab.dto.response.admin.AdminRoleResponse;
import com.smartlab.dto.response.admin.AdminSystemRoleResponse;
import com.smartlab.dto.response.admin.AdminUserResponse;
import com.smartlab.dto.response.admin.AdminUserRoleResponse;
import com.smartlab.entity.Post;
import com.smartlab.entity.PostModerationLog;
import com.smartlab.entity.User;

class AdminApiStructureTests {

	@Test
	void controllersAreRestAdaptersWithoutRepositoryDependenciesAndInactiveInNodb() {
		assertControllerBoundary(AdminPostController.class);
		assertControllerBoundary(AdminLabAnnouncementController.class);
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
		assertNoCredentialComponent(AdminSystemRoleResponse.class);
		assertNoCredentialComponent(AdminPermissionResponse.class);
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
		assertTrue(AdminPostModerationActionResponse.class.isRecord());
		assertEquals(
				List.of(
						"postId",
						"action",
						"fromStatus",
						"toStatus",
						"moderationStatus",
						"reviewedById",
						"reviewedByName",
						"reviewedAt"),
				Arrays.stream(AdminPostModerationActionResponse.class.getRecordComponents())
						.map(component -> component.getName())
						.toList());
		assertNoCredentialComponent(AdminPostModerationActionResponse.class);
		assertNoEntityComponent(AdminPostModerationActionResponse.class);
		assertFalse(hasRecordComponent(AdminPostModerationActionResponse.class, "email"));
		assertFalse(hasRecordComponent(AdminPostModerationActionResponse.class, "username"));
		assertFalse(hasRecordComponent(AdminPostModerationActionResponse.class, "reviewNote"));
		assertFalse(hasRecordComponent(AdminPostModerationActionResponse.class, "storagePath"));
		assertFalse(hasRecordComponent(AdminPostModerationActionResponse.class, "deletedAt"));
	}

	@Test
	void adminPostApproveRouteReturnsFocusedResponseDto() throws NoSuchMethodException {
		Method approveMethod = AdminPostController.class.getMethod("approvePost", UUID.class);
		PostMapping postMapping = approveMethod.getAnnotation(PostMapping.class);

		assertNotNull(postMapping);
		assertEquals(List.of("/{postId}/approve"), List.of(postMapping.value()));
		assertEquals(AdminPostModerationActionResponse.class, approveMethod.getReturnType());
	}

	@Test
	void adminLabAnnouncementRouteReturnsExistingSafePageDto() throws NoSuchMethodException {
		Method listMethod = AdminLabAnnouncementController.class.getMethod(
				"listLabAnnouncements",
				Integer.class,
				Integer.class);
		GetMapping getMapping = listMethod.getAnnotation(GetMapping.class);

		assertNotNull(getMapping);
		assertEquals(List.of(), List.of(getMapping.value()));
		assertEquals(AdminPostPageResponse.class, listMethod.getReturnType());
		assertNoCredentialComponent(AdminPostPageResponse.class);
		assertNoEntityComponent(AdminPostPageResponse.class);
	}

	@Test
	void adminLabAnnouncementDetailRouteReturnsExistingSafeDetailDto() throws NoSuchMethodException {
		Method detailMethod = AdminLabAnnouncementController.class.getMethod(
				"getLabAnnouncementDetail",
				UUID.class);
		GetMapping getMapping = detailMethod.getAnnotation(GetMapping.class);

		assertNotNull(getMapping);
		assertEquals(List.of("/{postId}"), List.of(getMapping.value()));
		assertEquals(AdminPostDetailResponse.class, detailMethod.getReturnType());
		assertNoCredentialComponent(AdminPostDetailResponse.class);
		assertNoEntityComponent(AdminPostDetailResponse.class);
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

	private static void assertNoEntityComponent(Class<?> recordType) {
		assertFalse(Arrays.stream(recordType.getRecordComponents())
				.map(component -> component.getType())
				.anyMatch(type -> type == Post.class || type == PostModerationLog.class || type == User.class));
	}
}

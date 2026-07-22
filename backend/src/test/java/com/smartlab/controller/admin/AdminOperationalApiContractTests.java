package com.smartlab.controller.admin;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.response.admin.AdminDashboardResponse;
import com.smartlab.dto.response.admin.AdminJoinRequestResponse;
import com.smartlab.dto.response.admin.AdminNotificationDetailResponse;
import com.smartlab.dto.response.admin.AdminNotificationResponse;
import com.smartlab.dto.response.admin.PageResponse;

class AdminOperationalApiContractTests {

	@Test
	void operationalControllersAreProfiledRestAdaptersWithoutRepositoryDependencies() {
		assertControllerBoundary(AdminDashboardController.class, "/api/admin/dashboard");
		assertControllerBoundary(AdminJoinRequestController.class, "/api/admin/project-join-requests");
		assertControllerBoundary(AdminNotificationController.class, "/api/admin/notifications");
	}

	@Test
	void operationalControllersExposeTheExpectedHttpContract() {
		assertMethodMapping(AdminDashboardController.class, "getDashboard", GetMapping.class);

		assertMethodMapping(AdminJoinRequestController.class, "getJoinRequests", GetMapping.class);
		assertMethodMapping(AdminJoinRequestController.class, "getJoinRequestDetail", GetMapping.class,
				"/{requestId}");
		assertMethodMapping(AdminJoinRequestController.class, "approve", PatchMapping.class,
				"/{requestId}/approve");
		assertMethodMapping(AdminJoinRequestController.class, "reject", PatchMapping.class,
				"/{requestId}/reject");

		assertMethodMapping(AdminNotificationController.class, "getNotifications", GetMapping.class);
		assertMethodMapping(AdminNotificationController.class, "getNotificationDetail", GetMapping.class,
				"/{notificationId}");
		assertMethodMapping(AdminNotificationController.class, "createNotification", PostMapping.class);
		assertMethodMapping(AdminNotificationController.class, "hideNotification", DeleteMapping.class,
				"/{notificationId}");
	}

	@Test
	void operationalResponseDtosDoNotExposePasswordOrHashFields() {
		assertNoCredentialComponents(
				AdminDashboardResponse.class,
				AdminDashboardResponse.RecentActivityResponse.class,
				AdminJoinRequestResponse.class,
				AdminJoinRequestResponse.ProjectSummary.class,
				AdminJoinRequestResponse.UserSummary.class,
				AdminJoinRequestResponse.CvFileSummary.class,
				AdminNotificationResponse.class,
				AdminNotificationResponse.UserSummary.class,
				AdminNotificationDetailResponse.class,
				AdminNotificationDetailResponse.UserSummary.class,
				AdminNotificationDetailResponse.RecipientResponse.class,
				PageResponse.class);
	}

	private static void assertControllerBoundary(Class<?> controllerType, String expectedBasePath) {
		assertNotNull(controllerType.getAnnotation(RestController.class),
				() -> controllerType.getSimpleName() + " must be a @RestController");

		Profile profile = controllerType.getAnnotation(Profile.class);
		assertNotNull(profile, () -> controllerType.getSimpleName() + " must declare a Spring profile");
		assertTrue(Arrays.asList(profile.value()).contains("!nodb"),
				() -> controllerType.getSimpleName() + " must be inactive in the nodb profile");

		RequestMapping requestMapping = controllerType.getAnnotation(RequestMapping.class);
		assertNotNull(requestMapping, () -> controllerType.getSimpleName() + " must declare a base route");
		assertArrayEquals(new String[] { expectedBasePath }, requestMapping.value());

		for (Field field : controllerType.getDeclaredFields()) {
			assertFalse(isRepositoryType(field.getType()),
					() -> controllerType.getSimpleName() + " must not inject repository field " + field.getName());
		}
		for (Constructor<?> constructor : controllerType.getDeclaredConstructors()) {
			for (Class<?> parameterType : constructor.getParameterTypes()) {
				assertFalse(isRepositoryType(parameterType),
						() -> controllerType.getSimpleName()
								+ " must not inject repository constructor parameter " + parameterType.getName());
			}
		}
	}

	private static void assertMethodMapping(
			Class<?> controllerType,
			String methodName,
			Class<? extends Annotation> mappingType,
			String... expectedPaths) {
		Method[] matchingMethods = Arrays.stream(controllerType.getDeclaredMethods())
				.filter(method -> method.getName().equals(methodName))
				.toArray(Method[]::new);
		assertEquals(1, matchingMethods.length,
				() -> controllerType.getSimpleName() + " must expose exactly one " + methodName + " method");

		Annotation mapping = matchingMethods[0].getAnnotation(mappingType);
		assertNotNull(mapping,
				() -> controllerType.getSimpleName() + "." + methodName + " must declare @" + mappingType.getSimpleName());
		assertArrayEquals(expectedPaths, mappingPaths(mapping));
	}

	private static String[] mappingPaths(Annotation mapping) {
		if (mapping instanceof GetMapping getMapping) {
			return getMapping.value();
		}
		if (mapping instanceof PostMapping postMapping) {
			return postMapping.value();
		}
		if (mapping instanceof PatchMapping patchMapping) {
			return patchMapping.value();
		}
		if (mapping instanceof DeleteMapping deleteMapping) {
			return deleteMapping.value();
		}
		throw new IllegalArgumentException("Unsupported mapping annotation: " + mapping.annotationType().getName());
	}

	private static boolean isRepositoryType(Class<?> type) {
		return type.getPackageName().equals("com.smartlab.repository")
				|| type.getPackageName().startsWith("com.smartlab.repository.");
	}

	private static void assertNoCredentialComponents(Class<?>... responseTypes) {
		for (Class<?> responseType : responseTypes) {
			assertTrue(responseType.isRecord(), () -> responseType.getSimpleName() + " must remain a record DTO");
			Arrays.stream(responseType.getRecordComponents()).forEach(component -> {
				String normalizedName = component.getName().toLowerCase();
				assertFalse(normalizedName.contains("password") || normalizedName.contains("hash"),
						() -> responseType.getSimpleName() + " exposes credential component " + component.getName());
			});
		}
	}
}

package com.smartlab.service.common;

import static com.smartlab.enums.ProjectStatus.COMPLETED;
import static com.smartlab.enums.ProjectStatus.IN_PROGRESS;
import static com.smartlab.enums.ProjectStatus.PAUSED;
import static com.smartlab.enums.ProjectStatus.PREPARING;
import static com.smartlab.enums.ProjectStatus.PROPOSED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.smartlab.exception.ConflictingAdminOperationException;
import com.smartlab.exception.InvalidAdminServiceInputException;

class BusinessValidationServiceTests {

	private final BusinessValidationService validationService = new BusinessValidationService();

	@Test
	void requireIdReturnsProvidedId() {
		UUID projectId = UUID.randomUUID();

		assertEquals(projectId, validationService.requireId(projectId, "Project ID"));
	}

	@Test
	void requireIdRejectsNullAndUsesProvidedFieldName() {
		InvalidAdminServiceInputException exception = assertThrows(
				InvalidAdminServiceInputException.class,
				() -> validationService.requireId(null, "Project ID"));

		assertEquals("Project ID must not be null.", exception.getMessage());
	}

	@Test
	void requireIdUsesSafeFallbackForBlankFieldName() {
		InvalidAdminServiceInputException exception = assertThrows(
				InvalidAdminServiceInputException.class,
				() -> validationService.requireId(null, "  "));

		assertEquals("ID must not be null.", exception.getMessage());
	}

	@Test
	void validateProgressAcceptsInclusiveBoundaries() {
		assertEquals(0, validationService.validateProgress(0));
		assertEquals(100, validationService.validateProgress(100));
	}

	@Test
	void validateProgressRejectsNull() {
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> validationService.validateProgress(null));
	}

	@Test
	void validateProgressRejectsValuesOutsideInclusiveRange() {
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> validationService.validateProgress(-1));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> validationService.validateProgress(101));
	}

	@Test
	void validateDateRangeAcceptsOptionalDatesAndEqualBoundaries() {
		LocalDate startDate = LocalDate.of(2026, 7, 23);

		assertDoesNotThrow(() -> validationService.validateDateRange(null, null, null));
		assertDoesNotThrow(() -> validationService.validateDateRange(startDate, null, null));
		assertDoesNotThrow(() -> validationService.validateDateRange(startDate, startDate, startDate));
	}

	@Test
	void validateDateRangeRejectsExpectedEndBeforeStart() {
		LocalDate startDate = LocalDate.of(2026, 7, 23);

		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> validationService.validateDateRange(startDate, startDate.minusDays(1), null));
	}

	@Test
	void validateDateRangeRejectsActualEndBeforeStart() {
		LocalDate startDate = LocalDate.of(2026, 7, 23);

		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> validationService.validateDateRange(startDate, null, startDate.minusDays(1)));
	}

	@Test
	void validateStateTransitionAcceptsCallerDefinedAllowedTarget() {
		Set<com.smartlab.enums.ProjectStatus> allowedTargets = Set.of(PREPARING);

		assertTrue(validationService.isStateTransitionAllowed(PROPOSED, PREPARING, allowedTargets));
		assertDoesNotThrow(() -> validationService.validateStateTransition(PROPOSED, PREPARING, allowedTargets));
	}

	@Test
	void validateStateTransitionRejectsNullStatesAndRules() {
		assertFalse(validationService.isStateTransitionAllowed(null, PREPARING, Set.of(PREPARING)));
		assertFalse(validationService.isStateTransitionAllowed(PROPOSED, null, Set.of(PREPARING)));
		assertFalse(validationService.isStateTransitionAllowed(PROPOSED, PREPARING, null));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> validationService.validateStateTransition(null, PREPARING, Set.of(PREPARING)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> validationService.validateStateTransition(PROPOSED, null, Set.of(PREPARING)));
		assertThrows(
				InvalidAdminServiceInputException.class,
				() -> validationService.validateStateTransition(PROPOSED, PREPARING, null));
	}

	@Test
	void validateStateTransitionRejectsSameStateAsConflict() {
		assertFalse(validationService.isStateTransitionAllowed(IN_PROGRESS, IN_PROGRESS, Set.of(IN_PROGRESS)));
		assertThrows(
				ConflictingAdminOperationException.class,
				() -> validationService.validateStateTransition(IN_PROGRESS, IN_PROGRESS, Set.of(IN_PROGRESS)));
	}

	@Test
	void validateStateTransitionRejectsDisallowedTargetAsConflict() {
		Set<com.smartlab.enums.ProjectStatus> allowedTargets = Set.of(PAUSED, COMPLETED);

		assertFalse(validationService.isStateTransitionAllowed(IN_PROGRESS, PREPARING, allowedTargets));
		assertThrows(
				ConflictingAdminOperationException.class,
				() -> validationService.validateStateTransition(IN_PROGRESS, PREPARING, allowedTargets));
	}
}

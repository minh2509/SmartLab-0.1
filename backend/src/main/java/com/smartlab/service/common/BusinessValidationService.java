package com.smartlab.service.common;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smartlab.exception.ConflictingAdminOperationException;
import com.smartlab.exception.InvalidAdminServiceInputException;

@Service
public class BusinessValidationService {

	public UUID requireId(UUID id, String fieldName) {
		if (id == null) {
			throw new InvalidAdminServiceInputException(normalizeFieldName(fieldName) + " must not be null.");
		}
		return id;
	}

	public int validateProgress(Integer progress) {
		if (progress == null) {
			throw new InvalidAdminServiceInputException("Progress must not be null.");
		}
		if (progress < 0 || progress > 100) {
			throw new InvalidAdminServiceInputException("Progress must be between 0 and 100.");
		}
		return progress;
	}

	public void validateDateRange(
			LocalDate startDate,
			LocalDate expectedEndDate,
			LocalDate actualEndDate) {
		if (startDate != null && expectedEndDate != null && expectedEndDate.isBefore(startDate)) {
			throw new InvalidAdminServiceInputException("Expected end date must not be before the start date.");
		}
		if (startDate != null && actualEndDate != null && actualEndDate.isBefore(startDate)) {
			throw new InvalidAdminServiceInputException("Actual end date must not be before the start date.");
		}
	}

	public <S> boolean isStateTransitionAllowed(S currentState, S targetState, Set<S> allowedTargetStates) {
		return currentState != null
				&& targetState != null
				&& !Objects.equals(currentState, targetState)
				&& allowedTargetStates != null
				&& allowedTargetStates.contains(targetState);
	}

	public <S> void validateStateTransition(S currentState, S targetState, Set<S> allowedTargetStates) {
		if (currentState == null) {
			throw new InvalidAdminServiceInputException("Current state must not be null.");
		}
		if (targetState == null) {
			throw new InvalidAdminServiceInputException("Target state must not be null.");
		}
		if (allowedTargetStates == null) {
			throw new InvalidAdminServiceInputException("Allowed target states must not be null.");
		}
		if (Objects.equals(currentState, targetState)) {
			throw new ConflictingAdminOperationException("Same-state transitions are not allowed.");
		}
		if (!isStateTransitionAllowed(currentState, targetState, allowedTargetStates)) {
			throw new ConflictingAdminOperationException(
					"Transition from " + currentState + " to " + targetState + " is not allowed.");
		}
	}

	private static String normalizeFieldName(String fieldName) {
		if (fieldName == null || fieldName.isBlank()) {
			return "ID";
		}
		return fieldName.trim();
	}
}

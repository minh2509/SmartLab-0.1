package com.smartlab.service.common;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.smartlab.enums.PostStatus;
import com.smartlab.exception.InvalidPostTransitionException;

public class PostWorkflowService {

	private static final Map<PostStatus, Set<PostStatus>> ALLOWED_TRANSITIONS = createAllowedTransitions();

	public boolean isTransitionAllowed(PostStatus currentStatus, PostStatus targetStatus) {
		if (currentStatus == null || targetStatus == null || currentStatus == targetStatus) {
			return false;
		}

		return getAllowedTargets(currentStatus).contains(targetStatus);
	}

	public void validateTransition(PostStatus currentStatus, PostStatus targetStatus) {
		if (currentStatus == null) {
			throw new InvalidPostTransitionException("Current post status must not be null.");
		}
		if (targetStatus == null) {
			throw new InvalidPostTransitionException("Target post status must not be null.");
		}
		if (currentStatus == targetStatus) {
			throw new InvalidPostTransitionException("Same-status post transitions are invalid.");
		}
		if (!isTransitionAllowed(currentStatus, targetStatus)) {
			throw new InvalidPostTransitionException(
					"Post transition from " + currentStatus + " to " + targetStatus + " is not allowed.");
		}
	}

	public Set<PostStatus> getAllowedTargets(PostStatus currentStatus) {
		if (currentStatus == null) {
			return Set.of();
		}

		return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
	}

	private static Map<PostStatus, Set<PostStatus>> createAllowedTransitions() {
		Map<PostStatus, Set<PostStatus>> transitions = new EnumMap<>(PostStatus.class);
		transitions.put(PostStatus.DRAFT, Set.of(PostStatus.PENDING_REVIEW));
		transitions.put(
				PostStatus.PENDING_REVIEW,
				Set.of(PostStatus.APPROVED, PostStatus.NEEDS_REVISION, PostStatus.REJECTED));
		transitions.put(PostStatus.NEEDS_REVISION, Set.of(PostStatus.PENDING_REVIEW));
		transitions.put(PostStatus.APPROVED, Set.of(PostStatus.PUBLISHED));
		transitions.put(PostStatus.PUBLISHED, Set.of(PostStatus.APPROVED));
		transitions.put(PostStatus.REJECTED, Set.of());
		return Map.copyOf(transitions);
	}
}

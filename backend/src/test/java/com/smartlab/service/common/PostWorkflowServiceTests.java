package com.smartlab.service.common;

import static com.smartlab.enums.PostStatus.APPROVED;
import static com.smartlab.enums.PostStatus.DRAFT;
import static com.smartlab.enums.PostStatus.NEEDS_REVISION;
import static com.smartlab.enums.PostStatus.PENDING_REVIEW;
import static com.smartlab.enums.PostStatus.PUBLISHED;
import static com.smartlab.enums.PostStatus.REJECTED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.smartlab.enums.PostStatus;
import com.smartlab.exception.InvalidPostTransitionException;

class PostWorkflowServiceTests {

	private final PostWorkflowService workflowService = new PostWorkflowService();

	@Test
	void validateTransitionAllowsDraftToPendingReview() {
		assertAllowed(DRAFT, PENDING_REVIEW);
	}

	@Test
	void validateTransitionAllowsPendingReviewToApproved() {
		assertAllowed(PENDING_REVIEW, APPROVED);
	}

	@Test
	void validateTransitionAllowsPendingReviewToNeedsRevision() {
		assertAllowed(PENDING_REVIEW, NEEDS_REVISION);
	}

	@Test
	void validateTransitionAllowsPendingReviewToRejected() {
		assertAllowed(PENDING_REVIEW, REJECTED);
	}

	@Test
	void validateTransitionAllowsNeedsRevisionToPendingReview() {
		assertAllowed(NEEDS_REVISION, PENDING_REVIEW);
	}

	@Test
	void validateTransitionAllowsApprovedToPublished() {
		assertAllowed(APPROVED, PUBLISHED);
	}

	@Test
	void validateTransitionAllowsPublishedToApproved() {
		assertAllowed(PUBLISHED, APPROVED);
	}

	@Test
	void validateTransitionRejectsDraftToPublished() {
		assertRejected(DRAFT, PUBLISHED);
	}

	@Test
	void validateTransitionRejectsDraftToApproved() {
		assertRejected(DRAFT, APPROVED);
	}

	@Test
	void validateTransitionRejectsRejectedToApproved() {
		assertRejected(REJECTED, APPROVED);
	}

	@Test
	void validateTransitionRejectsRejectedToDraft() {
		assertRejected(REJECTED, DRAFT);
	}

	@Test
	void validateTransitionRejectsSameStatus() {
		assertRejected(PENDING_REVIEW, PENDING_REVIEW);
	}

	@Test
	void validateTransitionRejectsNullCurrentStatus() {
		assertThrows(InvalidPostTransitionException.class, () -> workflowService.validateTransition(null, APPROVED));
		assertFalse(workflowService.isTransitionAllowed(null, APPROVED));
	}

	@Test
	void validateTransitionRejectsNullTargetStatus() {
		assertThrows(InvalidPostTransitionException.class, () -> workflowService.validateTransition(DRAFT, null));
		assertFalse(workflowService.isTransitionAllowed(DRAFT, null));
	}

	@Test
	void validateTransitionThrowsInvalidPostTransitionException() {
		assertThrows(InvalidPostTransitionException.class, () -> workflowService.validateTransition(DRAFT, PUBLISHED));
	}

	@Test
	void getAllowedTargetsDoesNotExposeMutableInternalCollections() {
		Set<PostStatus> allowedTargets = workflowService.getAllowedTargets(PENDING_REVIEW);

		assertTrue(allowedTargets.contains(APPROVED));
		assertThrows(UnsupportedOperationException.class, () -> allowedTargets.add(DRAFT));
	}

	private void assertAllowed(PostStatus currentStatus, PostStatus targetStatus) {
		assertTrue(workflowService.isTransitionAllowed(currentStatus, targetStatus));
		assertDoesNotThrow(() -> workflowService.validateTransition(currentStatus, targetStatus));
	}

	private void assertRejected(PostStatus currentStatus, PostStatus targetStatus) {
		assertFalse(workflowService.isTransitionAllowed(currentStatus, targetStatus));
		assertThrows(
				InvalidPostTransitionException.class,
				() -> workflowService.validateTransition(currentStatus, targetStatus));
	}
}

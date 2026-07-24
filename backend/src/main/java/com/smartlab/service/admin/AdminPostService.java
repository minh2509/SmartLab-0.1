package com.smartlab.service.admin;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.dto.response.admin.AdminPostDetailResponse;
import com.smartlab.dto.response.admin.AdminPostModerationActionResponse;
import com.smartlab.dto.response.admin.AdminPostPageResponse;
import com.smartlab.entity.Lab;
import com.smartlab.entity.Post;
import com.smartlab.entity.PostModerationLog;
import com.smartlab.enums.PostContentType;
import com.smartlab.enums.PostModerationAction;
import com.smartlab.enums.PostStatus;
import com.smartlab.enums.PostVisibility;
import com.smartlab.exception.ConflictingAdminOperationException;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.InvalidPostTransitionException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.mapper.AdminPostApiMapper;
import com.smartlab.repository.PostAttachmentRepository;
import com.smartlab.repository.PostModerationLogRepository;
import com.smartlab.repository.PostRepository;
import com.smartlab.service.common.AuditLogService;
import com.smartlab.service.common.PostWorkflowService;

@Service
@Profile("!nodb")
public class AdminPostService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final PostRepository postRepository;
	private final PostAttachmentRepository postAttachmentRepository;
	private final PostModerationLogRepository postModerationLogRepository;
	private final AdminRolePolicy rolePolicy;
	private final PostWorkflowService workflowService;
	private final AuditLogService auditLogService;
	private final AdminPostApiMapper mapper;
	private final Clock clock;

	public AdminPostService(
			PostRepository postRepository,
			PostAttachmentRepository postAttachmentRepository,
			PostModerationLogRepository postModerationLogRepository,
			AdminRolePolicy rolePolicy,
			PostWorkflowService workflowService,
			AuditLogService auditLogService,
			AdminPostApiMapper mapper,
			Clock clock) {
		this.postRepository = postRepository;
		this.postAttachmentRepository = postAttachmentRepository;
		this.postModerationLogRepository = postModerationLogRepository;
		this.rolePolicy = rolePolicy;
		this.workflowService = workflowService;
		this.auditLogService = auditLogService;
		this.mapper = mapper;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public AdminPostPageResponse listPosts(ListAdminPostsQuery query) {
		if (query == null) {
			throw new InvalidAdminServiceInputException("List admin posts query must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(query.actorUserId());
		int page = normalizedPage(query.page());
		int size = normalizedSize(query.size());

		return listPostsForActor(
				actor.lab(),
				normalizedKeywordPattern(query.keyword()),
				query.status(),
				query.contentType(),
				query.authorId(),
				query.projectId(),
				query.visibility(),
				page,
				size);
	}

	@Transactional(readOnly = true)
	public AdminPostPageResponse listPendingPosts(ListPendingAdminPostsQuery query) {
		if (query == null) {
			throw new InvalidAdminServiceInputException("List pending admin posts query must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(query.actorUserId());
		int page = normalizedPage(query.page());
		int size = normalizedSize(query.size());
		PageRequest pageable = PageRequest.of(page, size);
		Page<UUID> orderedIdPage = postRepository.findPendingAdminPostIds(
				actor.lab().getId(),
				pageable);
		if (orderedIdPage.isEmpty()) {
			return mapper.toPageResponse(new PageImpl<>(
					List.of(),
					pageable,
					orderedIdPage.getTotalElements()));
		}
		List<UUID> orderedIds = orderedIdPage.getContent();
		UUID labId = actor.lab().getId();
		Map<UUID, Post> postsById = postRepository.findPendingAdminPostsByIdIn(labId, orderedIds)
				.stream()
				.collect(Collectors.toMap(Post::getId, Function.identity(), (left, right) -> left));
		if (!postsById.keySet().containsAll(orderedIds)) {
			throw new IllegalStateException("Pending admin post page fetch returned incomplete results.");
		}
		List<Post> orderedPosts = orderedIds.stream()
				.map(postsById::get)
				.toList();
		return mapper.toPageResponse(new PageImpl<>(
				orderedPosts,
				pageable,
				orderedIdPage.getTotalElements()));
	}

	@Transactional(readOnly = true)
	public AdminPostPageResponse listLabAnnouncements(ListLabAnnouncementsQuery query) {
		if (query == null) {
			throw new InvalidAdminServiceInputException("List lab announcements query must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(query.actorUserId());
		int page = normalizedPage(query.page());
		int size = normalizedSize(query.size());

		return listPostsForActor(
				actor.lab(),
				null,
				null,
				PostContentType.LAB_ANNOUNCEMENT,
				null,
				null,
				null,
				page,
				size);
	}

	@Transactional(readOnly = true)
	public AdminPostDetailResponse getPostDetail(GetAdminPostDetailQuery query) {
		if (query == null) {
			throw new InvalidAdminServiceInputException("Get admin post detail query must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(query.actorUserId());
		if (query.postId() == null) {
			throw new InvalidAdminServiceInputException("Post ID must not be null.");
		}
		Post post = postRepository.findAdminPostDetail(actor.lab().getId(), query.postId())
				.orElseThrow(() -> new ResourceNotFoundException("Post was not found."));
		return mapper.toDetailResponse(
				post,
				postAttachmentRepository.findVisibleAdminPostAttachments(post),
				postModerationLogRepository.findAdminPostModerationHistory(post));
	}

	@Transactional(readOnly = true)
	public AdminPostDetailResponse getLabAnnouncementDetail(GetAdminLabAnnouncementDetailQuery query) {
		if (query == null) {
			throw new InvalidAdminServiceInputException("Get admin lab announcement detail query must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(query.actorUserId());
		if (query.postId() == null) {
			throw new InvalidAdminServiceInputException("Post ID must not be null.");
		}
		Post post = postRepository.findAdminPostDetail(actor.lab().getId(), query.postId())
				.orElseThrow(() -> new ResourceNotFoundException("Post was not found."));
		if (post.getContentType() != PostContentType.LAB_ANNOUNCEMENT) {
			throw new ResourceNotFoundException("Post was not found.");
		}
		return mapper.toDetailResponse(
				post,
				postAttachmentRepository.findVisibleAdminPostAttachments(post),
				postModerationLogRepository.findAdminPostModerationHistory(post));
	}

	@Transactional
	public AdminPostModerationActionResponse approvePost(ApproveAdminPostCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Approve admin post command must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		if (command.postId() == null) {
			throw new InvalidAdminServiceInputException("Post ID must not be null.");
		}
		Post post = postRepository.findAdminPostForApproval(actor.lab().getId(), command.postId())
				.orElseThrow(() -> new ResourceNotFoundException("Post was not found."));
		PostStatus fromStatus = post.getModerationStatus();
		PostStatus toStatus = PostStatus.APPROVED;
		if (fromStatus != PostStatus.PENDING_REVIEW) {
			throw new ConflictingAdminOperationException("Only posts pending review can be approved.");
		}
		try {
			workflowService.validateTransition(fromStatus, toStatus);
		} catch (InvalidPostTransitionException exception) {
			throw new ConflictingAdminOperationException(exception.getMessage());
		}

		OffsetDateTime reviewedAt = OffsetDateTime.now(clock);
		post.setModerationStatus(toStatus);
		post.setReviewedBy(actor.actor());
		post.setReviewedAt(reviewedAt);
		post.setReviewNote(null);

		PostModerationLog log = new PostModerationLog();
		log.setPost(post);
		log.setAction(PostModerationAction.APPROVE);
		log.setFromStatus(fromStatus);
		log.setToStatus(toStatus);
		log.setActor(actor.actor());
		log.setReason(null);
		postModerationLogRepository.save(log);

		return mapper.toModerationActionResponse(
				post,
				PostModerationAction.APPROVE,
				fromStatus,
				toStatus,
				reviewedAt);
	}

	@Transactional
	public AdminPostModerationActionResponse publishPost(PublishAdminPostCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Publish admin post command must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		if (command.postId() == null) {
			throw new InvalidAdminServiceInputException("Post ID must not be null.");
		}
		Post post = postRepository.findAdminPostForApproval(actor.lab().getId(), command.postId())
				.orElseThrow(() -> new ResourceNotFoundException("Post was not found."));
		PostStatus fromStatus = post.getModerationStatus();
		PostStatus toStatus = PostStatus.PUBLISHED;
		if (fromStatus != PostStatus.APPROVED) {
			throw new ConflictingAdminOperationException("Only approved posts can be published.");
		}
		try {
			workflowService.validateTransition(fromStatus, toStatus);
		} catch (InvalidPostTransitionException exception) {
			throw new ConflictingAdminOperationException(exception.getMessage());
		}

		Map<String, Object> oldValue = safePostAuditSnapshot(post);
		OffsetDateTime publishedAt = OffsetDateTime.now(clock);
		post.setModerationStatus(toStatus);
		post.setPublishedAt(publishedAt);

		PostModerationLog log = new PostModerationLog();
		log.setPost(post);
		log.setAction(PostModerationAction.PUBLISH);
		log.setFromStatus(fromStatus);
		log.setToStatus(toStatus);
		log.setActor(actor.actor());
		log.setReason(null);
		postModerationLogRepository.save(log);

		auditLogService.record(new AuditLogService.AuditCommand(
				actor.actor().getId(),
				"PUBLISH_POST",
				"POST",
				post.getId(),
				oldValue,
				safePostAuditSnapshot(post)));

		return mapper.toModerationActionResponse(
				post,
				PostModerationAction.PUBLISH,
				fromStatus,
				toStatus,
				post.getReviewedAt());
	}

	@Transactional
	public AdminPostModerationActionResponse unpublishPost(UnpublishAdminPostCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Unpublish admin post command must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		if (command.postId() == null) {
			throw new InvalidAdminServiceInputException("Post ID must not be null.");
		}
		Post post = postRepository.findAdminPostForApproval(actor.lab().getId(), command.postId())
				.orElseThrow(() -> new ResourceNotFoundException("Post was not found."));
		PostStatus fromStatus = post.getModerationStatus();
		PostStatus toStatus = PostStatus.APPROVED;
		if (fromStatus != PostStatus.PUBLISHED) {
			throw new ConflictingAdminOperationException("Only published posts can be unpublished.");
		}
		try {
			workflowService.validateTransition(fromStatus, toStatus);
		} catch (InvalidPostTransitionException exception) {
			throw new ConflictingAdminOperationException(exception.getMessage());
		}

		Map<String, Object> oldValue = safePostAuditSnapshot(post);
		post.setModerationStatus(toStatus);
		post.setPublishedAt(null);

		PostModerationLog log = new PostModerationLog();
		log.setPost(post);
		log.setAction(PostModerationAction.UNPUBLISH);
		log.setFromStatus(fromStatus);
		log.setToStatus(toStatus);
		log.setActor(actor.actor());
		log.setReason(null);
		postModerationLogRepository.save(log);

		auditLogService.record(new AuditLogService.AuditCommand(
				actor.actor().getId(),
				"UNPUBLISH_POST",
				"POST",
				post.getId(),
				oldValue,
				safePostAuditSnapshot(post)));

		return mapper.toModerationActionResponse(
				post,
				PostModerationAction.UNPUBLISH,
				fromStatus,
				toStatus,
				post.getReviewedAt());
	}

	@Transactional
	public void softDeletePost(DeleteAdminPostCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Delete admin post command must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		if (command.postId() == null) {
			throw new InvalidAdminServiceInputException("Post ID must not be null.");
		}
		Post post = postRepository.findAdminPostForDeletion(actor.lab().getId(), command.postId())
				.orElseThrow(() -> new ResourceNotFoundException("Post was not found."));

		PostDeletionAuditSnapshot oldValue = safePostDeletionAuditSnapshot(post);
		post.setDeletedAt(OffsetDateTime.now(clock));
		auditLogService.record(new AuditLogService.AuditCommand(
				actor.actor().getId(),
				"DELETE_POST",
				"POST",
				post.getId(),
				oldValue,
				safePostDeletionAuditSnapshot(post)));
	}

	private static int normalizedPage(Integer page) {
		if (page == null) {
			return DEFAULT_PAGE;
		}
		if (page < 0) {
			throw new InvalidAdminServiceInputException("Page must be greater than or equal to 0.");
		}
		return page;
	}

	private static int normalizedSize(Integer size) {
		if (size == null) {
			return DEFAULT_SIZE;
		}
		if (size < 1 || size > MAX_SIZE) {
			throw new InvalidAdminServiceInputException("Size must be between 1 and 100.");
		}
		return size;
	}

	private static String normalizedKeywordPattern(String keyword) {
		if (keyword == null) {
			return null;
		}
		String normalizedKeyword = keyword.strip();
		if (normalizedKeyword.isBlank()) {
			return null;
		}
		String escapedKeyword = normalizedKeyword
				.toLowerCase(Locale.ROOT)
				.replace("!", "!!")
				.replace("%", "!%")
				.replace("_", "!_");
		return "%" + escapedKeyword + "%";
	}

	private AdminPostPageResponse listPostsForActor(
			Lab lab,
			String keywordPattern,
			PostStatus status,
			PostContentType contentType,
			UUID authorId,
			UUID projectId,
			PostVisibility visibility,
			int page,
			int size) {
		return mapper.toPageResponse(postRepository.findAdminPosts(
				lab,
				keywordPattern,
				status,
				contentType,
				authorId,
				projectId,
				visibility,
				PageRequest.of(page, size)));
	}

	private static Map<String, Object> safePostAuditSnapshot(Post post) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("postId", post.getId());
		snapshot.put("moderationStatus", post.getModerationStatus());
		snapshot.put("publishedAt", post.getPublishedAt());
		return snapshot;
	}

	private static PostDeletionAuditSnapshot safePostDeletionAuditSnapshot(Post post) {
		return new PostDeletionAuditSnapshot(
				post.getId(),
				post.getModerationStatus(),
				post.getContentType(),
				post.getVisibility(),
				post.getPublishedAt(),
				post.getReviewedBy() == null ? null : post.getReviewedBy().getId(),
				post.getReviewedAt(),
				post.getDeletedAt());
	}

	public record ListAdminPostsQuery(
			UUID actorUserId,
			Integer page,
			Integer size,
			String keyword,
			PostStatus status,
			PostContentType contentType,
			UUID authorId,
			UUID projectId,
			PostVisibility visibility) {
	}

	public record ListPendingAdminPostsQuery(
			UUID actorUserId,
			Integer page,
			Integer size) {
	}

	public record ListLabAnnouncementsQuery(
			UUID actorUserId,
			Integer page,
			Integer size) {
	}

	public record GetAdminPostDetailQuery(
			UUID actorUserId,
			UUID postId) {
	}

	public record GetAdminLabAnnouncementDetailQuery(
			UUID actorUserId,
			UUID postId) {
	}

	public record ApproveAdminPostCommand(
			UUID actorUserId,
			UUID postId) {
	}

	public record PublishAdminPostCommand(
			UUID actorUserId,
			UUID postId) {
	}

	public record UnpublishAdminPostCommand(
			UUID actorUserId,
			UUID postId) {
	}

	public record DeleteAdminPostCommand(
			UUID actorUserId,
			UUID postId) {
	}

	private record PostDeletionAuditSnapshot(
			UUID postId,
			PostStatus moderationStatus,
			PostContentType contentType,
			PostVisibility visibility,
			OffsetDateTime publishedAt,
			UUID reviewedById,
			OffsetDateTime reviewedAt,
			OffsetDateTime deletedAt) {
	}
}

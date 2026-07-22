package com.smartlab.service.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.Lab;
import com.smartlab.entity.Notification;
import com.smartlab.entity.NotificationRecipient;
import com.smartlab.entity.Project;
import com.smartlab.entity.ProjectMember;
import com.smartlab.entity.User;
import com.smartlab.enums.ProjectMemberStatus;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.LabRepository;
import com.smartlab.repository.NotificationRecipientRepository;
import com.smartlab.repository.NotificationRepository;
import com.smartlab.repository.ProjectMemberRepository;
import com.smartlab.repository.ProjectRepository;
import com.smartlab.repository.UserRepository;

@Service
@Profile("!nodb")
public class NotificationService {

	private static final int MAX_TITLE_LENGTH = 255;
	private static final int MAX_NOTIFICATION_TYPE_LENGTH = 80;
	private static final int MAX_RELATED_TYPE_LENGTH = 80;
	private static final int MAX_LINK_URL_LENGTH = 500;

	private final NotificationRepository notificationRepository;
	private final NotificationRecipientRepository notificationRecipientRepository;
	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final LabRepository labRepository;

	public NotificationService(
			NotificationRepository notificationRepository,
			NotificationRecipientRepository notificationRecipientRepository,
			UserRepository userRepository,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			LabRepository labRepository) {
		this.notificationRepository = notificationRepository;
		this.notificationRecipientRepository = notificationRecipientRepository;
		this.userRepository = userRepository;
		this.projectRepository = projectRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.labRepository = labRepository;
	}

	@Transactional
	public NotificationResult sendToUsers(NotificationCommand command, Collection<UUID> recipientIds) {
		NotificationContext context = prepare(command);
		List<User> recipients = resolveUsers(recipientIds, context.lab());
		return persist(context.notification(), recipients);
	}

	@Transactional
	public NotificationResult sendToProject(NotificationCommand command, UUID projectId) {
		NotificationContext context = prepare(command);
		if (projectId == null) {
			throw new IllegalArgumentException("Project ID must not be null.");
		}
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Notification project was not found."));
		requireSameLab(context.lab(), project.getLab(), "Project");
		List<User> recipients = projectMemberRepository
				.findByProjectAndMemberStatus(project, ProjectMemberStatus.ACTIVE)
				.stream()
				.map(ProjectMember::getUser)
				.filter(NotificationService::isNotDeleted)
				.toList();
		return persist(context.notification(), validateAndDeduplicate(recipients, context.lab()));
	}

	@Transactional
	public NotificationResult sendToLab(NotificationCommand command) {
		NotificationContext context = prepare(command);
		List<User> recipients = validateAndDeduplicate(
				userRepository.findByLab(context.lab()).stream().filter(NotificationService::isNotDeleted).toList(),
				context.lab());
		return persist(context.notification(), recipients);
	}

	private NotificationContext prepare(NotificationCommand command) {
		if (command == null) {
			throw new IllegalArgumentException("Notification command must not be null.");
		}
		if (command.labId() == null) {
			throw new IllegalArgumentException("Lab ID must not be null.");
		}

		Lab lab = labRepository.findById(command.labId())
				.orElseThrow(() -> new ResourceNotFoundException("Notification lab was not found."));
		User creator = null;
		if (command.creatorId() != null) {
			creator = userRepository.findById(command.creatorId())
					.orElseThrow(() -> new ResourceNotFoundException("Notification creator was not found."));
			requireSameLab(lab, creator.getLab(), "Creator");
			requireNotDeleted(creator, "Creator");
		}

		String relatedType = normalizeOptional(command.relatedType(), "Related type", MAX_RELATED_TYPE_LENGTH);
		if ((relatedType == null) != (command.relatedId() == null)) {
			throw new IllegalArgumentException("Related type and related ID must either both be provided or both be omitted.");
		}

		Notification notification = new Notification();
		notification.setLab(lab);
		notification.setCreatedBy(creator);
		notification.setTitle(requireTrimmed(command.title(), "Notification title", MAX_TITLE_LENGTH));
		notification.setMessage(normalizeOptional(command.message()));
		notification.setNotificationType(requireTrimmed(
				command.notificationType(),
				"Notification type",
				MAX_NOTIFICATION_TYPE_LENGTH));
		notification.setRelatedType(relatedType);
		notification.setRelatedId(command.relatedId());
		notification.setLinkUrl(validateLinkUrl(command.linkUrl()));
		return new NotificationContext(notification, lab);
	}

	private List<User> resolveUsers(Collection<UUID> recipientIds, Lab lab) {
		if (recipientIds == null) {
			throw new IllegalArgumentException("Recipient IDs must not be null.");
		}
		Set<UUID> uniqueIds = new LinkedHashSet<>();
		for (UUID recipientId : recipientIds) {
			if (recipientId == null) {
				throw new IllegalArgumentException("Recipient ID must not be null.");
			}
			uniqueIds.add(recipientId);
		}
		if (uniqueIds.isEmpty()) {
			throw new IllegalArgumentException("At least one recipient is required.");
		}

		Map<UUID, User> usersById = new LinkedHashMap<>();
		for (User user : userRepository.findAllById(uniqueIds)) {
			if (user != null && user.getId() != null) {
				usersById.putIfAbsent(user.getId(), user);
			}
		}
		if (!usersById.keySet().containsAll(uniqueIds)) {
			throw new ResourceNotFoundException("One or more notification recipients were not found.");
		}

		return uniqueIds.stream()
				.map(usersById::get)
				.peek(user -> {
					requireSameLab(lab, user.getLab(), "Recipient");
					requireNotDeleted(user, "Recipient");
				})
				.toList();
	}

	private List<User> validateAndDeduplicate(Collection<User> users, Lab lab) {
		Map<UUID, User> recipientsById = new LinkedHashMap<>();
		if (users != null) {
			for (User user : users) {
				if (user == null || user.getId() == null) {
					throw new IllegalArgumentException("Notification recipient must have an ID.");
				}
				requireSameLab(lab, user.getLab(), "Recipient");
				requireNotDeleted(user, "Recipient");
				recipientsById.putIfAbsent(user.getId(), user);
			}
		}
		if (recipientsById.isEmpty()) {
			throw new IllegalArgumentException("At least one recipient is required.");
		}
		return List.copyOf(recipientsById.values());
	}

	private NotificationResult persist(Notification notification, List<User> recipients) {
		List<User> uniqueRecipients = validateAndDeduplicate(recipients, notification.getLab());
		Notification saved = notificationRepository.save(notification);
		List<NotificationRecipient> recipientEntities = uniqueRecipients.stream()
				.map(user -> recipient(saved, user))
				.toList();
		notificationRecipientRepository.saveAll(recipientEntities);
		return new NotificationResult(saved.getId(), recipientEntities.size());
	}

	private static NotificationRecipient recipient(Notification notification, User user) {
		NotificationRecipient recipient = new NotificationRecipient();
		recipient.setNotification(notification);
		recipient.setRecipient(user);
		return recipient;
	}

	private static void requireSameLab(Lab expected, Lab actual, String subject) {
		if (expected == null || expected.getId() == null || actual == null || actual.getId() == null
				|| !expected.getId().equals(actual.getId())) {
			throw new IllegalArgumentException(subject + " must belong to the notification lab.");
		}
	}

	private static boolean isNotDeleted(User user) {
		return user != null && user.getDeletedAt() == null;
	}

	private static void requireNotDeleted(User user, String subject) {
		if (!isNotDeleted(user)) {
			throw new IllegalArgumentException(subject + " must not be soft-deleted.");
		}
	}

	private static String requireTrimmed(String value, String fieldName, int maximumLength) {
		if (value == null || value.trim().isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank.");
		}
		String trimmed = value.trim();
		if (trimmed.length() > maximumLength) {
			throw new IllegalArgumentException(fieldName + " must not exceed " + maximumLength + " characters.");
		}
		return trimmed;
	}

	private static String normalizeOptional(String value) {
		if (value == null || value.trim().isBlank()) {
			return null;
		}
		return value.trim();
	}

	private static String normalizeOptional(String value, String fieldName, int maximumLength) {
		String normalized = normalizeOptional(value);
		if (normalized != null && normalized.length() > maximumLength) {
			throw new IllegalArgumentException(fieldName + " must not exceed " + maximumLength + " characters.");
		}
		return normalized;
	}

	private static String validateLinkUrl(String value) {
		String linkUrl = normalizeOptional(value);
		if (linkUrl == null) {
			return null;
		}
		if (linkUrl.length() > MAX_LINK_URL_LENGTH) {
			throw new IllegalArgumentException("Notification link URL must not exceed 500 characters.");
		}

		try {
			URI uri = new URI(linkUrl);
			if (uri.isAbsolute()) {
				String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
				if (!("http".equals(scheme) || "https".equals(scheme)) || uri.getHost() == null) {
					throw new IllegalArgumentException("Notification link URL must use HTTP or HTTPS.");
				}
			} else if (!linkUrl.startsWith("/") || linkUrl.startsWith("//") || uri.getRawAuthority() != null) {
				throw new IllegalArgumentException("Notification link URL must be an application path or HTTP(S) URL.");
			}
			return linkUrl;
		} catch (URISyntaxException exception) {
			throw new IllegalArgumentException("Notification link URL is invalid.", exception);
		}
	}

	private record NotificationContext(Notification notification, Lab lab) {
	}

	public record NotificationCommand(
			UUID labId,
			UUID creatorId,
			String title,
			String message,
			String notificationType,
			String relatedType,
			UUID relatedId,
			String linkUrl) {
	}

	public record NotificationResult(UUID notificationId, int recipientCount) {
	}
}

package com.smartlab.service.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

class NotificationServiceTests {

	private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
	private final NotificationRecipientRepository notificationRecipientRepository =
			mock(NotificationRecipientRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final ProjectRepository projectRepository = mock(ProjectRepository.class);
	private final ProjectMemberRepository projectMemberRepository = mock(ProjectMemberRepository.class);
	private final LabRepository labRepository = mock(LabRepository.class);
	private final NotificationService service = new NotificationService(
			notificationRepository,
			notificationRecipientRepository,
			userRepository,
			projectRepository,
			projectMemberRepository,
			labRepository);

	@Test
	void sendToUsersDeduplicatesRecipientsAndPersistsOneNotification() {
		UUID notificationId = UUID.randomUUID();
		Lab lab = lab(UUID.randomUUID());
		User creator = user(UUID.randomUUID(), lab);
		User first = user(UUID.randomUUID(), lab);
		User second = user(UUID.randomUUID(), lab);
		NotificationService.NotificationCommand command = command(lab, creator, "/app/projects/demo");
		when(labRepository.findById(lab.getId())).thenReturn(Optional.of(lab));
		when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
		when(userRepository.findAllById(any())).thenReturn(List.of(first, second));
		stubNotificationSave(notificationId);

		NotificationService.NotificationResult result = service.sendToUsers(
				command,
				List.of(first.getId(), second.getId(), first.getId()));

		assertEquals(notificationId, result.notificationId());
		assertEquals(2, result.recipientCount());
		ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(notificationCaptor.capture());
		Notification notification = notificationCaptor.getValue();
		assertEquals(lab, notification.getLab());
		assertEquals(creator, notification.getCreatedBy());
		assertEquals("Project updated", notification.getTitle());
		assertEquals("PROJECT", notification.getRelatedType());
		assertEquals("/app/projects/demo", notification.getLinkUrl());
		assertEquals(Set.of(first.getId(), second.getId()), recipientIds(captureRecipients()));
	}

	@Test
	void sendToProjectTargetsOnlyActiveProjectMembersAndDeduplicatesThem() {
		UUID notificationId = UUID.randomUUID();
		Lab lab = lab(UUID.randomUUID());
		User first = user(UUID.randomUUID(), lab);
		User second = user(UUID.randomUUID(), lab);
		Project project = project(UUID.randomUUID(), lab);
		when(labRepository.findById(lab.getId())).thenReturn(Optional.of(lab));
		when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
		when(projectMemberRepository.findByProjectAndMemberStatus(project, ProjectMemberStatus.ACTIVE))
				.thenReturn(List.of(member(project, first), member(project, first), member(project, second)));
		stubNotificationSave(notificationId);

		NotificationService.NotificationResult result = service.sendToProject(command(lab, null, null), project.getId());

		assertEquals(2, result.recipientCount());
		assertEquals(Set.of(first.getId(), second.getId()), recipientIds(captureRecipients()));
		verify(projectMemberRepository).findByProjectAndMemberStatus(project, ProjectMemberStatus.ACTIVE);
	}

	@Test
	void sendToLabTargetsLabUsersAndDeduplicatesThem() {
		Lab lab = lab(UUID.randomUUID());
		User first = user(UUID.randomUUID(), lab);
		User second = user(UUID.randomUUID(), lab);
		User deleted = user(UUID.randomUUID(), lab);
		deleted.setDeletedAt(OffsetDateTime.now());
		when(labRepository.findById(lab.getId())).thenReturn(Optional.of(lab));
		when(userRepository.findByLab(lab)).thenReturn(List.of(first, second, first, deleted));
		stubNotificationSave(UUID.randomUUID());

		NotificationService.NotificationResult result = service.sendToLab(command(lab, null, "https://smartlab.edu/news"));

		assertEquals(2, result.recipientCount());
		assertEquals(Set.of(first.getId(), second.getId()), recipientIds(captureRecipients()));
	}

	@Test
	void sendToUsersRejectsSoftDeletedCreatorAndRecipient() {
		Lab lab = lab(UUID.randomUUID());
		User deleted = user(UUID.randomUUID(), lab);
		deleted.setDeletedAt(OffsetDateTime.now());
		when(labRepository.findById(lab.getId())).thenReturn(Optional.of(lab));
		when(userRepository.findById(deleted.getId())).thenReturn(Optional.of(deleted));

		assertThrows(
				IllegalArgumentException.class,
				() -> service.sendToUsers(command(lab, deleted, null), List.of(deleted.getId())));

		when(userRepository.findAllById(any())).thenReturn(List.of(deleted));
		assertThrows(
				IllegalArgumentException.class,
				() -> service.sendToUsers(command(lab, null, null), List.of(deleted.getId())));
		verify(notificationRepository, never()).save(any());
	}

	@Test
	void sendRejectsCrossLabCreatorRecipientAndProject() {
		Lab lab = lab(UUID.randomUUID());
		Lab otherLab = lab(UUID.randomUUID());
		User crossLabUser = user(UUID.randomUUID(), otherLab);
		when(labRepository.findById(lab.getId())).thenReturn(Optional.of(lab));
		when(userRepository.findById(crossLabUser.getId())).thenReturn(Optional.of(crossLabUser));

		assertThrows(
				IllegalArgumentException.class,
				() -> service.sendToUsers(command(lab, crossLabUser, null), List.of(crossLabUser.getId())));

		when(userRepository.findAllById(any())).thenReturn(List.of(crossLabUser));
		assertThrows(
				IllegalArgumentException.class,
				() -> service.sendToUsers(command(lab, null, null), List.of(crossLabUser.getId())));

		Project crossLabProject = project(UUID.randomUUID(), otherLab);
		when(projectRepository.findById(crossLabProject.getId())).thenReturn(Optional.of(crossLabProject));
		assertThrows(
				IllegalArgumentException.class,
				() -> service.sendToProject(command(lab, null, null), crossLabProject.getId()));
		verify(notificationRepository, never()).save(any());
	}

	@Test
	void sendToUsersRejectsEmptyNullAndUnknownRecipients() {
		Lab lab = lab(UUID.randomUUID());
		UUID unknownId = UUID.randomUUID();
		when(labRepository.findById(lab.getId())).thenReturn(Optional.of(lab));

		assertThrows(IllegalArgumentException.class, () -> service.sendToUsers(command(lab, null, null), null));
		assertThrows(IllegalArgumentException.class, () -> service.sendToUsers(command(lab, null, null), List.of()));
		assertThrows(
				ResourceNotFoundException.class,
				() -> service.sendToUsers(command(lab, null, null), List.of(unknownId)));
		verify(notificationRepository, never()).save(any());
	}

	@Test
	void validatesRelatedPairRequiredFieldsAndSafeLinks() {
		Lab lab = lab(UUID.randomUUID());
		when(labRepository.findById(lab.getId())).thenReturn(Optional.of(lab));

		assertThrows(IllegalArgumentException.class, () -> service.sendToLab(new NotificationService.NotificationCommand(
				lab.getId(), null, "Title", null, "INFO", "PROJECT", null, null)));
		assertThrows(IllegalArgumentException.class, () -> service.sendToLab(new NotificationService.NotificationCommand(
				lab.getId(), null, "Title", null, "INFO", null, UUID.randomUUID(), null)));
		assertThrows(IllegalArgumentException.class, () -> service.sendToLab(new NotificationService.NotificationCommand(
				lab.getId(), null, " ", null, "INFO", null, null, null)));
		assertThrows(IllegalArgumentException.class, () -> service.sendToLab(new NotificationService.NotificationCommand(
				lab.getId(), null, "Title", null, " ", null, null, null)));
		assertThrows(IllegalArgumentException.class, () -> service.sendToLab(new NotificationService.NotificationCommand(
				lab.getId(), null, "Title", null, "INFO", null, null, "javascript:alert(1)")));
		assertThrows(IllegalArgumentException.class, () -> service.sendToLab(new NotificationService.NotificationCommand(
				lab.getId(), null, "Title", null, "INFO", null, null, "app/projects/demo")));
		verify(notificationRepository, never()).save(any());
	}

	@Test
	void serviceMethodsAreTransactionalSpringService() throws NoSuchMethodException {
		assertTrue(NotificationService.class.isAnnotationPresent(Service.class));
		assertTransactional("sendToUsers", NotificationService.NotificationCommand.class, Collection.class);
		assertTransactional("sendToProject", NotificationService.NotificationCommand.class, UUID.class);
		assertTransactional("sendToLab", NotificationService.NotificationCommand.class);
	}

	private void stubNotificationSave(UUID notificationId) {
		when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
			Notification notification = invocation.getArgument(0);
			notification.setId(notificationId);
			return notification;
		});
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private List<NotificationRecipient> captureRecipients() {
		ArgumentCaptor<Iterable> captor = ArgumentCaptor.forClass(Iterable.class);
		verify(notificationRecipientRepository).saveAll(captor.capture());
		return StreamSupport.stream(captor.getValue().spliterator(), false)
				.map(NotificationRecipient.class::cast)
				.toList();
	}

	private static Set<UUID> recipientIds(List<NotificationRecipient> recipients) {
		return recipients.stream().map(recipient -> recipient.getRecipient().getId()).collect(java.util.stream.Collectors.toSet());
	}

	private static void assertTransactional(String methodName, Class<?>... argumentTypes) throws NoSuchMethodException {
		Method method = NotificationService.class.getMethod(methodName, argumentTypes);
		assertTrue(method.isAnnotationPresent(Transactional.class));
	}

	private static NotificationService.NotificationCommand command(Lab lab, User creator, String linkUrl) {
		return new NotificationService.NotificationCommand(
				lab.getId(),
				creator == null ? null : creator.getId(),
				" Project updated ",
				" Details are ready. ",
				" PROJECT_UPDATE ",
				" PROJECT ",
				UUID.randomUUID(),
				linkUrl);
	}

	private static Lab lab(UUID id) {
		Lab lab = new Lab();
		lab.setId(id);
		return lab;
	}

	private static User user(UUID id, Lab lab) {
		User user = new User();
		user.setId(id);
		user.setLab(lab);
		return user;
	}

	private static Project project(UUID id, Lab lab) {
		Project project = new Project();
		project.setId(id);
		project.setLab(lab);
		return project;
	}

	private static ProjectMember member(Project project, User user) {
		ProjectMember member = new ProjectMember();
		member.setProject(project);
		member.setUser(user);
		member.setMemberStatus(ProjectMemberStatus.ACTIVE);
		return member;
	}
}

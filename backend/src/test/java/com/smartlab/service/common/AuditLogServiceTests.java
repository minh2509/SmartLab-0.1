package com.smartlab.service.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.AuditLog;
import com.smartlab.entity.Lab;
import com.smartlab.entity.User;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.AuditLogRepository;
import com.smartlab.repository.UserRepository;

import tools.jackson.databind.ObjectMapper;

class AuditLogServiceTests {

	private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final AuditLogService service = new AuditLogService(auditLogRepository, userRepository, objectMapper);

	@Test
	void recordResolvesActorAndStoresJsonValues() {
		UUID auditId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID entityId = UUID.randomUUID();
		Lab lab = lab(UUID.randomUUID());
		User actor = user(actorId, lab);
		Map<String, Object> oldValue = Map.of("status", "PENDING", "tags", List.of("ai", "robotics"));
		Map<String, Object> newValue = Map.of("status", "APPROVED", "note", "Ready \"now\"");
		when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
		when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
			AuditLog auditLog = invocation.getArgument(0);
			auditLog.setId(auditId);
			return auditLog;
		});

		UUID result = service.record(new AuditLogService.AuditCommand(
				actorId,
				"  ADMIN_APPROVE_JOIN_REQUEST ",
				" PROJECT_JOIN_REQUEST ",
				entityId,
				oldValue,
				newValue));

		assertEquals(auditId, result);
		ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
		verify(auditLogRepository).save(captor.capture());
		AuditLog saved = captor.getValue();
		assertEquals(actor, saved.getActor());
		assertEquals(lab, saved.getLab());
		assertEquals("ADMIN_APPROVE_JOIN_REQUEST", saved.getAction());
		assertEquals("PROJECT_JOIN_REQUEST", saved.getEntityType());
		assertEquals(entityId, saved.getEntityId());
		assertEquals(objectMapper.writeValueAsString(oldValue), saved.getOldValue());
		assertEquals(objectMapper.writeValueAsString(newValue), saved.getNewValue());
		assertDoesNotThrow(() -> objectMapper.readTree(saved.getOldValue()));
		assertDoesNotThrow(() -> objectMapper.readTree(saved.getNewValue()));
	}

	@Test
	void recordAcceptsMissingOptionalActorEntityAndValues() {
		UUID auditId = UUID.randomUUID();
		when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
			AuditLog auditLog = invocation.getArgument(0);
			auditLog.setId(auditId);
			return auditLog;
		});

		assertEquals(auditId, service.record(new AuditLogService.AuditCommand(
				null,
				"SYSTEM_CHECK",
				"SYSTEM",
				null,
				null,
				null)));

		ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
		verify(auditLogRepository).save(captor.capture());
		AuditLog saved = captor.getValue();
		assertNull(saved.getActor());
		assertNull(saved.getLab());
		assertNull(saved.getEntityId());
		assertNull(saved.getOldValue());
		assertNull(saved.getNewValue());
		verify(userRepository, never()).findById(any());
	}

	@Test
	void recordRejectsMissingRequiredFieldsBeforeSaving() {
		assertThrows(IllegalArgumentException.class, () -> service.record(null));
		assertThrows(IllegalArgumentException.class, () -> service.record(
				new AuditLogService.AuditCommand(null, " ", "PROJECT", null, null, null)));
		assertThrows(IllegalArgumentException.class, () -> service.record(
				new AuditLogService.AuditCommand(null, "UPDATE", null, null, null, null)));
		assertThrows(IllegalArgumentException.class, () -> service.record(
				new AuditLogService.AuditCommand(null, "A".repeat(101), "PROJECT", null, null, null)));

		verify(auditLogRepository, never()).save(any());
	}

	@Test
	void recordRejectsNonJsonSerializableAuditValueBeforeSaving() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.record(
				new AuditLogService.AuditCommand(
						null,
						"UPDATE",
						"PROJECT",
						UUID.randomUUID(),
						new NonJsonSerializableValue(),
						null)));

		assertEquals("Audit old value must be JSON serializable.", exception.getMessage());
		verify(auditLogRepository, never()).save(any());
	}

	@Test
	void recordRejectsUnknownActor() {
		UUID actorId = UUID.randomUUID();

		assertThrows(ResourceNotFoundException.class, () -> service.record(
				new AuditLogService.AuditCommand(actorId, "UPDATE", "PROJECT", null, null, null)));
		verify(auditLogRepository, never()).save(any());
	}

	@Test
	void serviceIsTransactionalSpringService() throws NoSuchMethodException {
		assertTrue(AuditLogService.class.isAnnotationPresent(Service.class));
		Method method = AuditLogService.class.getMethod("record", AuditLogService.AuditCommand.class);
		assertTrue(method.isAnnotationPresent(Transactional.class));
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

	private static final class NonJsonSerializableValue {

		public String getBroken() {
			throw new IllegalStateException("Cannot serialize this value.");
		}
	}
}

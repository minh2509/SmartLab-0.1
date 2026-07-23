package com.smartlab.service.common;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.AuditLog;
import com.smartlab.entity.User;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.repository.AuditLogRepository;
import com.smartlab.repository.UserRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Profile("!nodb")
public class AuditLogService {

	private static final int MAX_ACTION_LENGTH = 100;
	private static final int MAX_ENTITY_TYPE_LENGTH = 100;

	private final AuditLogRepository auditLogRepository;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;

	public AuditLogService(
			AuditLogRepository auditLogRepository,
			UserRepository userRepository,
			ObjectMapper objectMapper) {
		this.auditLogRepository = auditLogRepository;
		this.userRepository = userRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public UUID record(AuditCommand command) {
		if (command == null) {
			throw new IllegalArgumentException("Audit command must not be null.");
		}

		User actor = command.actorId() == null ? null : findActor(command.actorId());
		AuditLog auditLog = new AuditLog();
		auditLog.setActor(actor);
		auditLog.setLab(actor == null ? null : actor.getLab());
		auditLog.setAction(requireTrimmed(command.action(), "Audit action", MAX_ACTION_LENGTH));
		auditLog.setEntityType(requireTrimmed(command.entityType(), "Audit entity type", MAX_ENTITY_TYPE_LENGTH));
		auditLog.setEntityId(command.entityId());
		auditLog.setOldValue(toJson(command.oldValue(), "old value"));
		auditLog.setNewValue(toJson(command.newValue(), "new value"));

		return auditLogRepository.save(auditLog).getId();
	}

	private User findActor(UUID actorId) {
		return userRepository.findById(actorId)
				.orElseThrow(() -> new ResourceNotFoundException("Audit actor was not found."));
	}

	private String toJson(Object value, String fieldName) {
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Audit " + fieldName + " must be JSON serializable.", exception);
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

	public record AuditCommand(
			UUID actorId,
			String action,
			String entityType,
			UUID entityId,
			Object oldValue,
			Object newValue) {
	}
}

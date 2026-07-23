package com.smartlab.service.admin;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.dto.response.admin.AdminResearchFieldListResponse;
import com.smartlab.dto.response.admin.AdminResearchFieldResponse;
import com.smartlab.entity.ResearchField;
import com.smartlab.enums.CatalogStatus;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.mapper.AdminResearchFieldApiMapper;
import com.smartlab.repository.ResearchFieldRepository;

@Service
@Profile("!nodb")
public class AdminResearchFieldService {

	private final ResearchFieldRepository researchFieldRepository;
	private final AdminRolePolicy rolePolicy;
	private final AdminResearchFieldApiMapper mapper;

	public AdminResearchFieldService(
			ResearchFieldRepository researchFieldRepository,
			AdminRolePolicy rolePolicy,
			AdminResearchFieldApiMapper mapper) {
		this.researchFieldRepository = researchFieldRepository;
		this.rolePolicy = rolePolicy;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	public AdminResearchFieldListResponse getResearchFields(UUID actorUserId) {
		rolePolicy.requireAdminActor(actorUserId);
		List<ResearchField> fields = researchFieldRepository.findAll();
		return mapper.toListResponse(fields);
	}

	@Transactional
	public AdminResearchFieldResponse createField(CreateResearchFieldCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Create research field command must not be null.");
		}
		rolePolicy.requireAdminActor(command.actorUserId());

		String code = requireTrimmed(command.code(), "Code").toUpperCase(Locale.ROOT);
		String name = requireTrimmed(command.name(), "Name");

		if (researchFieldRepository.findByCode(code).isPresent()) {
			throw new InvalidAdminServiceInputException("Research field code already exists.");
		}

		ResearchField field = new ResearchField();
		field.setCode(code);
		field.setName(name);
		field.setDescription(trimToNull(command.description()));
		field.setStatus(CatalogStatus.ACTIVE);

		ResearchField saved = researchFieldRepository.save(field);
		return mapper.toResponse(saved);
	}

	@Transactional
	public AdminResearchFieldResponse updateField(UpdateResearchFieldCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Update research field command must not be null.");
		}
		rolePolicy.requireAdminActor(command.actorUserId());
		if (command.fieldId() == null) {
			throw new InvalidAdminServiceInputException("Research field ID must not be null.");
		}

		ResearchField field = researchFieldRepository.findById(command.fieldId())
				.orElseThrow(() -> new ResourceNotFoundException("Research field was not found."));

		String name = requireTrimmed(command.name(), "Name");
		field.setName(name);
		field.setDescription(trimToNull(command.description()));

		ResearchField saved = researchFieldRepository.save(field);
		return mapper.toResponse(saved);
	}

	@Transactional
	public AdminResearchFieldResponse updateFieldStatus(UpdateResearchFieldStatusCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Update research field status command must not be null.");
		}
		rolePolicy.requireAdminActor(command.actorUserId());
		if (command.fieldId() == null) {
			throw new InvalidAdminServiceInputException("Research field ID must not be null.");
		}
		if (command.status() == null) {
			throw new InvalidAdminServiceInputException("Status must not be null.");
		}

		ResearchField field = researchFieldRepository.findById(command.fieldId())
				.orElseThrow(() -> new ResourceNotFoundException("Research field was not found."));

		field.setStatus(command.status());
		ResearchField saved = researchFieldRepository.save(field);
		return mapper.toResponse(saved);
	}

	private static String requireTrimmed(String value, String fieldName) {
		if (value == null || value.trim().isBlank()) {
			throw new InvalidAdminServiceInputException(fieldName + " must not be blank.");
		}
		return value.trim();
	}

	private static String trimToNull(String value) {
		if (value == null || value.trim().isBlank()) {
			return null;
		}
		return value.trim();
	}

	public record CreateResearchFieldCommand(
			UUID actorUserId,
			String code,
			String name,
			String description) {
	}

	public record UpdateResearchFieldCommand(
			UUID actorUserId,
			UUID fieldId,
			String name,
			String description) {
	}

	public record UpdateResearchFieldStatusCommand(
			UUID actorUserId,
			UUID fieldId,
			CatalogStatus status) {
	}
}

package com.smartlab.service.admin;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.dto.response.admin.AdminLabResponse;
import com.smartlab.entity.File;
import com.smartlab.entity.Lab;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.mapper.AdminLabApiMapper;
import com.smartlab.repository.FileRepository;
import com.smartlab.repository.LabRepository;

@Service
@Profile("!nodb")
public class AdminLabService {

	private final LabRepository labRepository;
	private final FileRepository fileRepository;
	private final AdminRolePolicy rolePolicy;
	private final AdminLabApiMapper mapper;

	public AdminLabService(
			LabRepository labRepository,
			FileRepository fileRepository,
			AdminRolePolicy rolePolicy,
			AdminLabApiMapper mapper) {
		this.labRepository = labRepository;
		this.fileRepository = fileRepository;
		this.rolePolicy = rolePolicy;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	public AdminLabResponse getLabInfo(UUID actorUserId) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		return mapper.toResponse(actor.lab());
	}

	@Transactional
	public AdminLabResponse updateLabInfo(UpdateLabInfoCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Update lab info command must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		Lab lab = actor.lab();

		String name = requireTrimmed(command.name(), "Name");
		lab.setName(name);
		lab.setDescription(trimToNull(command.description()));
		lab.setMission(trimToNull(command.mission()));
		lab.setVision(trimToNull(command.vision()));
		lab.setContactEmail(trimToNull(command.contactEmail()));
		lab.setWebsiteUrl(trimToNull(command.websiteUrl()));

		Lab saved = labRepository.save(lab);
		return mapper.toResponse(saved);
	}

	@Transactional
	public AdminLabResponse updateLogo(UpdateLabImageCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Update lab logo command must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		Lab lab = actor.lab();

		File file = findFileInLab(lab, command.fileId());
		lab.setLogoFile(file);

		Lab saved = labRepository.save(lab);
		return mapper.toResponse(saved);
	}

	@Transactional
	public AdminLabResponse updateCover(UpdateLabImageCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Update lab cover command must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		Lab lab = actor.lab();

		File file = findFileInLab(lab, command.fileId());
		lab.setCoverFile(file);

		Lab saved = labRepository.save(lab);
		return mapper.toResponse(saved);
	}

	private File findFileInLab(Lab lab, UUID fileId) {
		if (fileId == null) {
			throw new InvalidAdminServiceInputException("File ID must not be null.");
		}
		File file = fileRepository.findById(fileId)
				.orElseThrow(() -> new ResourceNotFoundException("File was not found."));
		if (file.getLab() == null || !file.getLab().getId().equals(lab.getId())) {
			throw new InvalidAdminServiceInputException("File does not belong to the actor's lab.");
		}
		return file;
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

	public record UpdateLabInfoCommand(
			UUID actorUserId,
			String name,
			String description,
			String mission,
			String vision,
			String contactEmail,
			String websiteUrl) {
	}

	public record UpdateLabImageCommand(
			UUID actorUserId,
			UUID fileId) {
	}
}

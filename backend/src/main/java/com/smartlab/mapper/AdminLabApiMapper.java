package com.smartlab.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.smartlab.dto.response.admin.AdminLabResponse;
import com.smartlab.entity.File;
import com.smartlab.entity.Lab;

@Component
public class AdminLabApiMapper {

	public AdminLabResponse toResponse(Lab lab) {
		if (lab == null) {
			return null;
		}
		return new AdminLabResponse(
				lab.getId(),
				lab.getName(),
				lab.getCode(),
				lab.getDescription(),
				lab.getMission(),
				lab.getVision(),
				lab.getContactEmail(),
				lab.getWebsiteUrl(),
				idOf(lab.getLogoFile()),
				idOf(lab.getCoverFile()),
				lab.getStatus());
	}

	private static UUID idOf(File file) {
		return file == null ? null : file.getId();
	}
}

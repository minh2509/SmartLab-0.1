package com.smartlab.dto.request.admin;

import com.smartlab.enums.PostVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAdminLabAnnouncementRequest(
		@NotBlank @Size(max = 255) String title,
		String summary,
		@NotBlank String content,
		@NotNull PostVisibility visibility,
		Boolean publishNow) {
}

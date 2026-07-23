package com.smartlab.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UpdateResearchFieldRequest {

	@NotBlank
	@Size(max = 150)
	private String name;

	private String description;

	public UpdateResearchFieldRequest() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}

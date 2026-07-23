package com.smartlab.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CreateResearchFieldRequest {

	@NotBlank
	@Size(max = 50)
	private String code;

	@NotBlank
	@Size(max = 150)
	private String name;

	private String description;

	public CreateResearchFieldRequest() {
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
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

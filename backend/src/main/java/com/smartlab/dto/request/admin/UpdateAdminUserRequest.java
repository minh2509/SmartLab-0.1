package com.smartlab.dto.request.admin;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public final class UpdateAdminUserRequest {

	@Size(max = 100)
	private String username;

	@Email
	@Size(max = 255)
	private String email;

	@Size(max = 255)
	private String fullName;

	private UUID avatarFileId;

	private Boolean clearAvatarFile;

	public UpdateAdminUserRequest() {
	}

	public String username() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String email() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String fullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public UUID avatarFileId() {
		return avatarFileId;
	}

	public void setAvatarFileId(UUID avatarFileId) {
		this.avatarFileId = avatarFileId;
	}

	public boolean clearAvatarFile() {
		return Boolean.TRUE.equals(clearAvatarFile);
	}

	public void setClearAvatarFile(Boolean clearAvatarFile) {
		this.clearAvatarFile = clearAvatarFile;
	}

	@JsonAnySetter
	void rejectUnknownField(String fieldName, Object value) {
		throw new IllegalArgumentException("Unknown update user field.");
	}
}

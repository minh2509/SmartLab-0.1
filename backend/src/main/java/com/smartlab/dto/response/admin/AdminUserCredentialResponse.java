package com.smartlab.dto.response.admin;

public record AdminUserCredentialResponse(
		AdminUserResponse user,
		String temporaryPassword,
		boolean generated) {
}

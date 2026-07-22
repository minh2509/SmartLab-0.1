package com.smartlab.dto.request.admin;

import com.smartlab.enums.UserAccountStatus;

import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequest(
		@NotNull UserAccountStatus status) {
}

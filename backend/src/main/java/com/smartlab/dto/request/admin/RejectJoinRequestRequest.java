package com.smartlab.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectJoinRequestRequest(
		@NotBlank
		@Size(max = 2000)
		String reason) {
}

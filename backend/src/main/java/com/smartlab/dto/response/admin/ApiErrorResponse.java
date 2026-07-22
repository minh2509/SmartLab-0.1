package com.smartlab.dto.response.admin;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;

public record ApiErrorResponse(
		OffsetDateTime timestamp,
		int status,
		String error,
		String message,
		String path) {

	public static ApiErrorResponse of(HttpStatus status, String message, String path) {
		return new ApiErrorResponse(
				OffsetDateTime.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				path);
	}
}

package com.smartlab.security;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;

final class SecurityErrorResponseWriter {

	private SecurityErrorResponseWriter() {
	}

	static void write(
			HttpServletResponse response,
			HttpStatus status,
			String message,
			String path) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
		response.getWriter().write("""
				{"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s"}"""
				.formatted(
						escape(OffsetDateTime.now().toString()),
						status.value(),
						escape(status.getReasonPhrase()),
						escape(message),
						escape(path)));
	}

	private static String escape(String value) {
		return value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r");
	}
}

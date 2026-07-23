package com.smartlab.dto.response.admin;

import java.util.List;

public record AdminPostPageResponse(
		List<AdminPostSummaryResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last) {
}

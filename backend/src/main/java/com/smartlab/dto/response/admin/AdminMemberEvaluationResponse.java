package com.smartlab.dto.response.admin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminMemberEvaluationResponse(
		UUID id,
		UUID projectId,
		String projectName,
		String evaluationPeriod,
		BigDecimal overallScore,
		String comment,
		OffsetDateTime evaluatedAt) {
}

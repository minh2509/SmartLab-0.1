package com.smartlab.dto.response.admin;

import java.util.List;
import java.util.UUID;

import com.smartlab.enums.UserAccountStatus;

public record AdminMemberDetailResponse(
		UUID userId,
		String username,
		String fullName,
		String email,
		UUID avatarFileId,
		UserAccountStatus accountStatus,
		List<String> roleCodes,
		AdminMemberProfileResponse profile,
		List<AdminResearchFieldResponse> researchFields,
		int projectCount,
		int evaluationCount) {

	public record AdminMemberProfileResponse(
			String studentCode,
			String phone,
			String personalEmail,
			String bio,
			String specialization,
			java.time.LocalDate joinedAt,
			com.smartlab.enums.MemberProfileActivityStatus activityStatus,
			String githubUrl,
			String linkedinUrl,
			String portfolioUrl) {
	}
}

package com.smartlab.dto.response.admin;

import java.time.LocalDate;
import java.util.UUID;

import com.smartlab.enums.MemberProfileActivityStatus;
import com.smartlab.enums.UserAccountStatus;

public record AdminMemberSummaryResponse(
		UUID userId,
		String username,
		String fullName,
		String email,
		UUID avatarFileId,
		UserAccountStatus accountStatus,
		MemberProfileActivityStatus activityStatus,
		java.util.List<String> roleCodes,
		LocalDate joinedAt) {
}

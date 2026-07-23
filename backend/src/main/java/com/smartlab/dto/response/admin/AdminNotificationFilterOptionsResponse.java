package com.smartlab.dto.response.admin;

import java.util.List;
import java.util.UUID;

import com.smartlab.enums.UserAccountStatus;

public record AdminNotificationFilterOptionsResponse(
		List<String> notificationTypes,
		List<String> creatableNotificationTypes,
		List<String> relatedTypes,
		List<CreatorOption> creators) {

	public AdminNotificationFilterOptionsResponse {
		notificationTypes = List.copyOf(notificationTypes);
		creatableNotificationTypes = List.copyOf(creatableNotificationTypes);
		relatedTypes = List.copyOf(relatedTypes);
		creators = List.copyOf(creators);
	}

	public record CreatorOption(
			UUID id,
			String fullName,
			String email,
			UserAccountStatus accountStatus) {
	}
}

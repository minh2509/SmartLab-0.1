package com.smartlab.dto.request.admin;

import com.smartlab.enums.MemberProfileActivityStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberActivityStatusRequest(@NotNull MemberProfileActivityStatus activityStatus) {
}

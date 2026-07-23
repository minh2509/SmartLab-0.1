package com.smartlab.dto.request.admin;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberResearchFieldsRequest(@NotNull List<UUID> fieldIds) {
}

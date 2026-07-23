package com.smartlab.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.smartlab.dto.response.admin.AdminResearchFieldListResponse;
import com.smartlab.dto.response.admin.AdminResearchFieldResponse;
import com.smartlab.entity.ResearchField;

@Component
public class AdminResearchFieldApiMapper {

	public AdminResearchFieldResponse toResponse(ResearchField field) {
		if (field == null) {
			return null;
		}
		return new AdminResearchFieldResponse(
				field.getId(),
				field.getCode(),
				field.getName(),
				field.getDescription(),
				field.getStatus(),
				field.getCreatedAt());
	}

	public AdminResearchFieldListResponse toListResponse(List<ResearchField> fields) {
		List<AdminResearchFieldResponse> list = fields == null ? List.of()
				: fields.stream().map(this::toResponse).toList();
		return new AdminResearchFieldListResponse(list);
	}
}

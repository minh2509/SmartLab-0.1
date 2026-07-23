package com.smartlab.mapper;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.smartlab.dto.response.admin.AdminMemberDetailResponse;
import com.smartlab.dto.response.admin.AdminMemberEvaluationResponse;
import com.smartlab.dto.response.admin.AdminMemberProjectResponse;
import com.smartlab.dto.response.admin.AdminMemberSummaryResponse;
import com.smartlab.dto.response.admin.AdminResearchFieldResponse;
import com.smartlab.entity.MemberEvaluation;
import com.smartlab.entity.MemberProfile;
import com.smartlab.entity.ProjectMember;
import com.smartlab.entity.ResearchField;
import com.smartlab.entity.User;

@Component
public class AdminMemberApiMapper {

	private final AdminResearchFieldApiMapper researchFieldApiMapper;

	public AdminMemberApiMapper(AdminResearchFieldApiMapper researchFieldApiMapper) {
		this.researchFieldApiMapper = researchFieldApiMapper;
	}

	public AdminMemberSummaryResponse toSummaryResponse(User user, MemberProfile profile, List<String> roleCodes) {
		return new AdminMemberSummaryResponse(
				user.getId(),
				user.getUsername(),
				user.getFullName(),
				user.getEmail(),
				user.getAvatarFile() == null ? null : user.getAvatarFile().getId(),
				user.getAccountStatus(),
				profile == null ? null : profile.getActivityStatus(),
				roleCodes == null ? List.of() : roleCodes,
				profile == null ? null : profile.getJoinedAt());
	}

	public AdminMemberDetailResponse toDetailResponse(
			User user,
			MemberProfile profile,
			List<String> roleCodes,
			List<ResearchField> fields,
			int projectCount,
			int evaluationCount) {
		AdminMemberDetailResponse.AdminMemberProfileResponse profileResponse = profile == null ? null
				: new AdminMemberDetailResponse.AdminMemberProfileResponse(
						profile.getStudentCode(),
						profile.getPhone(),
						profile.getPersonalEmail(),
						profile.getBio(),
						profile.getSpecialization(),
						profile.getJoinedAt(),
						profile.getActivityStatus(),
						profile.getGithubUrl(),
						profile.getLinkedinUrl(),
						profile.getPortfolioUrl());

		List<AdminResearchFieldResponse> fieldResponses = fields == null ? List.of()
				: fields.stream().map(researchFieldApiMapper::toResponse).toList();

		return new AdminMemberDetailResponse(
				user.getId(),
				user.getUsername(),
				user.getFullName(),
				user.getEmail(),
				user.getAvatarFile() == null ? null : user.getAvatarFile().getId(),
				user.getAccountStatus(),
				roleCodes == null ? List.of() : roleCodes,
				profileResponse,
				fieldResponses,
				projectCount,
				evaluationCount);
	}

	public AdminMemberProjectResponse toProjectResponse(ProjectMember projectMember) {
		return new AdminMemberProjectResponse(
				projectMember.getProject().getId(),
				projectMember.getProject().getName(),
				projectMember.getProject().getCode(),
				projectMember.getProjectRole(),
				projectMember.getMemberStatus(),
				projectMember.getJoinedAt());
	}

	public AdminMemberEvaluationResponse toEvaluationResponse(MemberEvaluation evaluation) {
		return new AdminMemberEvaluationResponse(
				evaluation.getId(),
				evaluation.getProject().getId(),
				evaluation.getProject().getName(),
				evaluation.getEvaluationPeriod(),
				evaluation.getOverallScore(),
				evaluation.getComment(),
				evaluation.getEvaluatedAt());
	}
}

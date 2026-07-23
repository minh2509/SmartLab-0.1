package com.smartlab.service.admin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.dto.response.admin.AdminMemberDetailResponse;
import com.smartlab.dto.response.admin.AdminMemberEvaluationResponse;
import com.smartlab.dto.response.admin.AdminMemberProjectResponse;
import com.smartlab.dto.response.admin.AdminMemberSummaryResponse;

import com.smartlab.entity.Lab;
import com.smartlab.entity.MemberEvaluation;
import com.smartlab.entity.MemberProfile;
import com.smartlab.entity.MemberResearchField;
import com.smartlab.entity.ProjectMember;
import com.smartlab.entity.ResearchField;
import com.smartlab.entity.User;
import com.smartlab.enums.MemberProfileActivityStatus;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.mapper.AdminMemberApiMapper;
import com.smartlab.repository.MemberEvaluationRepository;
import com.smartlab.repository.MemberProfileRepository;
import com.smartlab.repository.MemberResearchFieldRepository;
import com.smartlab.repository.ProjectMemberRepository;
import com.smartlab.repository.ResearchFieldRepository;
import com.smartlab.repository.UserRepository;

@Service
@Profile("!nodb")
public class AdminMemberService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final UserRepository userRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final MemberResearchFieldRepository memberResearchFieldRepository;
	private final ResearchFieldRepository researchFieldRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final MemberEvaluationRepository memberEvaluationRepository;
	private final AdminRolePolicy rolePolicy;
	private final AdminMemberApiMapper mapper;

	public AdminMemberService(
			UserRepository userRepository,
			MemberProfileRepository memberProfileRepository,
			MemberResearchFieldRepository memberResearchFieldRepository,
			ResearchFieldRepository researchFieldRepository,
			ProjectMemberRepository projectMemberRepository,
			MemberEvaluationRepository memberEvaluationRepository,
			AdminRolePolicy rolePolicy,
			AdminMemberApiMapper mapper) {
		this.userRepository = userRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.memberResearchFieldRepository = memberResearchFieldRepository;
		this.researchFieldRepository = researchFieldRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.memberEvaluationRepository = memberEvaluationRepository;
		this.rolePolicy = rolePolicy;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	public Page<AdminMemberSummaryResponse> listMembers(ListMembersQuery query) {
		if (query == null) {
			throw new InvalidAdminServiceInputException("List members query must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(query.actorUserId());
		int page = normalizedPage(query.page());
		int size = normalizedSize(query.size());
		PageRequest pageable = PageRequest.of(page, size);

		Page<User> usersPage;
		String keyword = trimToNull(query.keyword());
		if (keyword != null) {
			usersPage = userRepository.findByLabAndDeletedAtIsNullAndFullNameContainingIgnoreCase(actor.lab(), keyword, pageable);
		} else {
			usersPage = userRepository.findByLabAndDeletedAtIsNull(actor.lab(), pageable);
		}

		List<User> users = usersPage.getContent();
		var roleCodesMap = rolePolicy.activeRoleCodesByUserId(users);

		List<AdminMemberSummaryResponse> summaries = users.stream().map(user -> {
			MemberProfile profile = memberProfileRepository.findByUser(user).orElse(null);
			List<String> roleCodes = roleCodesMap.getOrDefault(user.getId(), List.of());
			return mapper.toSummaryResponse(user, profile, roleCodes);
		}).toList();

		return new PageImpl<>(summaries, pageable, usersPage.getTotalElements());
	}

	@Transactional(readOnly = true)
	public AdminMemberDetailResponse getMemberDetail(UUID actorUserId, UUID userId) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		User targetUser = findUserInLab(actor.lab(), userId);

		MemberProfile profile = memberProfileRepository.findByUser(targetUser).orElse(null);
		List<String> roleCodes = rolePolicy.activeRoleCodes(targetUser);

		List<ResearchField> fields = List.of();
		if (profile != null) {
			fields = memberResearchFieldRepository.findByMemberProfile(profile)
					.stream().map(MemberResearchField::getResearchField).toList();
		}

		int projectCount = projectMemberRepository.findByUser(targetUser).size();
		int evaluationCount = memberEvaluationRepository.findByMember(targetUser).size();

		return mapper.toDetailResponse(targetUser, profile, roleCodes, fields, projectCount, evaluationCount);
	}

	@Transactional
	public AdminMemberDetailResponse updateMemberProfile(UpdateMemberProfileCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Update member profile command must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		User targetUser = findUserInLab(actor.lab(), command.userId());

		MemberProfile profile = memberProfileRepository.findByUser(targetUser)
				.orElseGet(() -> {
					MemberProfile newProfile = new MemberProfile();
					newProfile.setUser(targetUser);
					return newProfile;
				});

		profile.setStudentCode(trimToNull(command.studentCode()));
		profile.setPhone(trimToNull(command.phone()));
		profile.setPersonalEmail(trimToNull(command.personalEmail()));
		profile.setBio(trimToNull(command.bio()));
		profile.setSpecialization(trimToNull(command.specialization()));
		profile.setJoinedAt(command.joinedAt());
		profile.setGithubUrl(trimToNull(command.githubUrl()));
		profile.setLinkedinUrl(trimToNull(command.linkedinUrl()));
		profile.setPortfolioUrl(trimToNull(command.portfolioUrl()));

		memberProfileRepository.save(profile);

		return getMemberDetail(command.actorUserId(), command.userId());
	}

	@Transactional
	public AdminMemberDetailResponse updateMemberResearchFields(UpdateMemberResearchFieldsCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Update member research fields command must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		User targetUser = findUserInLab(actor.lab(), command.userId());

		MemberProfile profile = memberProfileRepository.findByUser(targetUser)
				.orElseGet(() -> {
					MemberProfile newProfile = new MemberProfile();
					newProfile.setUser(targetUser);
					return memberProfileRepository.save(newProfile);
				});

		List<MemberResearchField> currentFields = memberResearchFieldRepository.findByMemberProfile(profile);
		memberResearchFieldRepository.deleteAll(currentFields);

		List<UUID> fieldIds = command.fieldIds() == null ? List.of() : command.fieldIds();
		List<MemberResearchField> newFields = new ArrayList<>();
		for (UUID fieldId : fieldIds) {
			ResearchField field = researchFieldRepository.findById(fieldId)
					.orElseThrow(() -> new ResourceNotFoundException("Research field was not found."));
			MemberResearchField mrf = new MemberResearchField();
			mrf.setMemberProfile(profile);
			mrf.setResearchField(field);
			newFields.add(mrf);
		}
		memberResearchFieldRepository.saveAll(newFields);

		return getMemberDetail(command.actorUserId(), command.userId());
	}

	@Transactional
	public AdminMemberDetailResponse updateMemberActivityStatus(UpdateMemberActivityStatusCommand command) {
		if (command == null) {
			throw new InvalidAdminServiceInputException("Update member activity status command must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(command.actorUserId());
		User targetUser = findUserInLab(actor.lab(), command.userId());

		if (command.activityStatus() == null) {
			throw new InvalidAdminServiceInputException("Activity status must not be null.");
		}

		MemberProfile profile = memberProfileRepository.findByUser(targetUser)
				.orElseGet(() -> {
					MemberProfile newProfile = new MemberProfile();
					newProfile.setUser(targetUser);
					return newProfile;
				});

		profile.setActivityStatus(command.activityStatus());
		memberProfileRepository.save(profile);

		return getMemberDetail(command.actorUserId(), command.userId());
	}

	@Transactional(readOnly = true)
	public List<AdminMemberProjectResponse> getMemberProjects(UUID actorUserId, UUID userId) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		User targetUser = findUserInLab(actor.lab(), userId);

		List<ProjectMember> memberships = projectMemberRepository.findByUser(targetUser);
		return memberships.stream().map(mapper::toProjectResponse).toList();
	}

	@Transactional(readOnly = true)
	public List<AdminMemberEvaluationResponse> getMemberEvaluations(UUID actorUserId, UUID userId) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		User targetUser = findUserInLab(actor.lab(), userId);

		List<MemberEvaluation> evaluations = memberEvaluationRepository.findByMember(targetUser);
		return evaluations.stream().map(mapper::toEvaluationResponse).toList();
	}

	private User findUserInLab(Lab lab, UUID userId) {
		if (userId == null) {
			throw new InvalidAdminServiceInputException("User ID must not be null.");
		}
		User user = userRepository.findByIdAndLab(userId, lab)
				.orElseThrow(() -> new ResourceNotFoundException("User was not found."));
		if (user.getDeletedAt() != null) {
			throw new ResourceNotFoundException("User was not found.");
		}
		return user;
	}

	private static int normalizedPage(Integer page) {
		if (page == null) {
			return DEFAULT_PAGE;
		}
		if (page < 0) {
			throw new InvalidAdminServiceInputException("Page must be greater than or equal to 0.");
		}
		return page;
	}

	private static int normalizedSize(Integer size) {
		if (size == null) {
			return DEFAULT_SIZE;
		}
		if (size < 1 || size > MAX_SIZE) {
			throw new InvalidAdminServiceInputException("Size must be between 1 and 100.");
		}
		return size;
	}

	private static String trimToNull(String value) {
		if (value == null || value.trim().isBlank()) {
			return null;
		}
		return value.trim();
	}

	public record ListMembersQuery(
			UUID actorUserId,
			Integer page,
			Integer size,
			String keyword) {
	}

	public record UpdateMemberProfileCommand(
			UUID actorUserId,
			UUID userId,
			String studentCode,
			String phone,
			String personalEmail,
			String bio,
			String specialization,
			LocalDate joinedAt,
			String githubUrl,
			String linkedinUrl,
			String portfolioUrl) {
	}

	public record UpdateMemberResearchFieldsCommand(
			UUID actorUserId,
			UUID userId,
			List<UUID> fieldIds) {
	}

	public record UpdateMemberActivityStatusCommand(
			UUID actorUserId,
			UUID userId,
			MemberProfileActivityStatus activityStatus) {
	}
}

package com.smartlab.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.MemberProfile;
import com.smartlab.entity.MemberResearchField;
import com.smartlab.entity.ResearchField;

public interface MemberResearchFieldRepository extends JpaRepository<MemberResearchField, UUID> {

	boolean existsByMemberProfileAndResearchField(MemberProfile memberProfile, ResearchField researchField);

	List<MemberResearchField> findByMemberProfile(MemberProfile memberProfile);
}

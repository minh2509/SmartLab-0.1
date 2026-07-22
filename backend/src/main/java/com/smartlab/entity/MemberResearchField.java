package com.smartlab.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "member_research_fields",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_member_research_fields",
				columnNames = {"member_profile_id", "research_field_id"}))
public class MemberResearchField {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_profile_id", nullable = false)
	private MemberProfile memberProfile;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "research_field_id", nullable = false)
	private ResearchField researchField;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public MemberProfile getMemberProfile() {
		return memberProfile;
	}

	public void setMemberProfile(MemberProfile memberProfile) {
		this.memberProfile = memberProfile;
	}

	public ResearchField getResearchField() {
		return researchField;
	}

	public void setResearchField(ResearchField researchField) {
		this.researchField = researchField;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MemberResearchField memberResearchField) || id == null) {
			return false;
		}
		return id.equals(memberResearchField.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}

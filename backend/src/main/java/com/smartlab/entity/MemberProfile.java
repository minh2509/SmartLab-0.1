package com.smartlab.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.smartlab.enums.MemberProfileActivityStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "member_profiles",
		uniqueConstraints = @UniqueConstraint(name = "uq_member_profiles_user", columnNames = "user_id"))
public class MemberProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "student_code", length = 100)
	private String studentCode;

	@Column(name = "phone", length = 50)
	private String phone;

	@Column(name = "personal_email", length = 255)
	private String personalEmail;

	@Column(name = "bio")
	private String bio;

	@Column(name = "specialization", length = 255)
	private String specialization;

	@Column(name = "joined_at")
	private LocalDate joinedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "activity_status", nullable = false, length = 30)
	private MemberProfileActivityStatus activityStatus = MemberProfileActivityStatus.ACTIVE;

	@Column(name = "github_url", length = 255)
	private String githubUrl;

	@Column(name = "linkedin_url", length = 255)
	private String linkedinUrl;

	@Column(name = "portfolio_url", length = 255)
	private String portfolioUrl;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime updatedAt;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getStudentCode() {
		return studentCode;
	}

	public void setStudentCode(String studentCode) {
		this.studentCode = studentCode;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getPersonalEmail() {
		return personalEmail;
	}

	public void setPersonalEmail(String personalEmail) {
		this.personalEmail = personalEmail;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public String getSpecialization() {
		return specialization;
	}

	public void setSpecialization(String specialization) {
		this.specialization = specialization;
	}

	public LocalDate getJoinedAt() {
		return joinedAt;
	}

	public void setJoinedAt(LocalDate joinedAt) {
		this.joinedAt = joinedAt;
	}

	public MemberProfileActivityStatus getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(MemberProfileActivityStatus activityStatus) {
		this.activityStatus = activityStatus;
	}

	public String getGithubUrl() {
		return githubUrl;
	}

	public void setGithubUrl(String githubUrl) {
		this.githubUrl = githubUrl;
	}

	public String getLinkedinUrl() {
		return linkedinUrl;
	}

	public void setLinkedinUrl(String linkedinUrl) {
		this.linkedinUrl = linkedinUrl;
	}

	public String getPortfolioUrl() {
		return portfolioUrl;
	}

	public void setPortfolioUrl(String portfolioUrl) {
		this.portfolioUrl = portfolioUrl;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MemberProfile memberProfile) || id == null) {
			return false;
		}
		return id.equals(memberProfile.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}

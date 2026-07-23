package com.smartlab.dto.request.admin;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public final class UpdateMemberProfileRequest {

	@Size(max = 100)
	private String studentCode;

	@Size(max = 50)
	private String phone;

	@Email
	@Size(max = 255)
	private String personalEmail;

	private String bio;

	@Size(max = 255)
	private String specialization;

	private LocalDate joinedAt;

	@Size(max = 255)
	private String githubUrl;

	@Size(max = 255)
	private String linkedinUrl;

	@Size(max = 255)
	private String portfolioUrl;

	public UpdateMemberProfileRequest() {
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
}

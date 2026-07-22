package com.smartlab.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.smartlab.enums.ProjectMemberRole;
import com.smartlab.enums.ProjectMemberStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
		name = "project_members",
		uniqueConstraints = @UniqueConstraint(name = "uq_project_members", columnNames = {"project_id", "user_id"}))
public class ProjectMember {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "project_role", nullable = false, length = 30)
	private ProjectMemberRole projectRole = ProjectMemberRole.PROJECT_MEMBER;

	@Enumerated(EnumType.STRING)
	@Column(name = "member_status", nullable = false, length = 30)
	private ProjectMemberStatus memberStatus = ProjectMemberStatus.ACTIVE;

	@Column(name = "joined_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime joinedAt;

	@Column(name = "left_at")
	private OffsetDateTime leftAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "added_by")
	private User addedBy;

	@Column(name = "note")
	private String note;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public ProjectMemberRole getProjectRole() {
		return projectRole;
	}

	public void setProjectRole(ProjectMemberRole projectRole) {
		this.projectRole = projectRole;
	}

	public ProjectMemberStatus getMemberStatus() {
		return memberStatus;
	}

	public void setMemberStatus(ProjectMemberStatus memberStatus) {
		this.memberStatus = memberStatus;
	}

	public OffsetDateTime getJoinedAt() {
		return joinedAt;
	}

	public void setJoinedAt(OffsetDateTime joinedAt) {
		this.joinedAt = joinedAt;
	}

	public OffsetDateTime getLeftAt() {
		return leftAt;
	}

	public void setLeftAt(OffsetDateTime leftAt) {
		this.leftAt = leftAt;
	}

	public User getAddedBy() {
		return addedBy;
	}

	public void setAddedBy(User addedBy) {
		this.addedBy = addedBy;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ProjectMember projectMember) || id == null) {
			return false;
		}
		return id.equals(projectMember.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}

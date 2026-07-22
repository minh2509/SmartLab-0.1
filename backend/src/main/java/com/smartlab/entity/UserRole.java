package com.smartlab.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.smartlab.enums.UserRoleStatus;

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

@Entity
@Table(name = "user_roles")
public class UserRole {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "role_id", nullable = false)
	private Role role;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_by")
	private User assignedBy;

	@Column(name = "assigned_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime assignedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private UserRoleStatus status = UserRoleStatus.ACTIVE;

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

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public User getAssignedBy() {
		return assignedBy;
	}

	public void setAssignedBy(User assignedBy) {
		this.assignedBy = assignedBy;
	}

	public OffsetDateTime getAssignedAt() {
		return assignedAt;
	}

	public void setAssignedAt(OffsetDateTime assignedAt) {
		this.assignedAt = assignedAt;
	}

	public UserRoleStatus getStatus() {
		return status;
	}

	public void setStatus(UserRoleStatus status) {
		this.status = status;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof UserRole userRole) || id == null) {
			return false;
		}
		return id.equals(userRole.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}

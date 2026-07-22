package com.smartlab.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.smartlab.enums.TaskAssigneeStatus;

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
		name = "task_assignees",
		uniqueConstraints = @UniqueConstraint(name = "uq_task_assignees", columnNames = {"task_id", "user_id"}))
public class TaskAssignee {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "task_id", nullable = false)
	private Task task;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_by")
	private User assignedBy;

	@Column(name = "assigned_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime assignedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private TaskAssigneeStatus status = TaskAssigneeStatus.ASSIGNED;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
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

	public TaskAssigneeStatus getStatus() {
		return status;
	}

	public void setStatus(TaskAssigneeStatus status) {
		this.status = status;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TaskAssignee taskAssignee) || id == null) {
			return false;
		}
		return id.equals(taskAssignee.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}

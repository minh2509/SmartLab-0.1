package com.smartlab.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.smartlab.enums.TaskReportStatus;

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
@Table(name = "task_reports")
public class TaskReport {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "task_id", nullable = false)
	private Task task;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reporter_id")
	private User reporter;

	@Column(name = "content")
	private String content;

	@Column(name = "result_summary")
	private String resultSummary;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private TaskReportStatus status = TaskReportStatus.SUBMITTED;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewed_by")
	private User reviewedBy;

	@Column(name = "reviewed_at")
	private OffsetDateTime reviewedAt;

	@Column(name = "feedback")
	private String feedback;

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

	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public User getReporter() {
		return reporter;
	}

	public void setReporter(User reporter) {
		this.reporter = reporter;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getResultSummary() {
		return resultSummary;
	}

	public void setResultSummary(String resultSummary) {
		this.resultSummary = resultSummary;
	}

	public TaskReportStatus getStatus() {
		return status;
	}

	public void setStatus(TaskReportStatus status) {
		this.status = status;
	}

	public User getReviewedBy() {
		return reviewedBy;
	}

	public void setReviewedBy(User reviewedBy) {
		this.reviewedBy = reviewedBy;
	}

	public OffsetDateTime getReviewedAt() {
		return reviewedAt;
	}

	public void setReviewedAt(OffsetDateTime reviewedAt) {
		this.reviewedAt = reviewedAt;
	}

	public String getFeedback() {
		return feedback;
	}

	public void setFeedback(String feedback) {
		this.feedback = feedback;
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
		if (!(other instanceof TaskReport taskReport) || id == null) {
			return false;
		}
		return id.equals(taskReport.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}

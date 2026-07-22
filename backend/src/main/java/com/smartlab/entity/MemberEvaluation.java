package com.smartlab.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "member_evaluations")
public class MemberEvaluation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private User member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "evaluator_id")
	private User evaluator;

	@Column(name = "evaluation_period", nullable = false, length = 100)
	private String evaluationPeriod;

	@Column(name = "overall_score", precision = 5, scale = 2)
	private BigDecimal overallScore;

	@Column(name = "comment")
	private String comment;

	@Column(name = "evaluated_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime evaluatedAt;

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

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public User getMember() {
		return member;
	}

	public void setMember(User member) {
		this.member = member;
	}

	public User getEvaluator() {
		return evaluator;
	}

	public void setEvaluator(User evaluator) {
		this.evaluator = evaluator;
	}

	public String getEvaluationPeriod() {
		return evaluationPeriod;
	}

	public void setEvaluationPeriod(String evaluationPeriod) {
		this.evaluationPeriod = evaluationPeriod;
	}

	public BigDecimal getOverallScore() {
		return overallScore;
	}

	public void setOverallScore(BigDecimal overallScore) {
		this.overallScore = overallScore;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public OffsetDateTime getEvaluatedAt() {
		return evaluatedAt;
	}

	public void setEvaluatedAt(OffsetDateTime evaluatedAt) {
		this.evaluatedAt = evaluatedAt;
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
		if (!(other instanceof MemberEvaluation memberEvaluation) || id == null) {
			return false;
		}
		return id.equals(memberEvaluation.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}

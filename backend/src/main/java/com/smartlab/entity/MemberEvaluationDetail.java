package com.smartlab.entity;

import java.math.BigDecimal;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "member_evaluation_details",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_member_evaluation_details",
				columnNames = {"evaluation_id", "criteria_id"}))
public class MemberEvaluationDetail {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "evaluation_id", nullable = false)
	private MemberEvaluation evaluation;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "criteria_id", nullable = false)
	private EvaluationCriteria criteria;

	@Column(name = "score", nullable = false, precision = 5, scale = 2)
	private BigDecimal score;

	@Column(name = "comment")
	private String comment;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public MemberEvaluation getEvaluation() {
		return evaluation;
	}

	public void setEvaluation(MemberEvaluation evaluation) {
		this.evaluation = evaluation;
	}

	public EvaluationCriteria getCriteria() {
		return criteria;
	}

	public void setCriteria(EvaluationCriteria criteria) {
		this.criteria = criteria;
	}

	public BigDecimal getScore() {
		return score;
	}

	public void setScore(BigDecimal score) {
		this.score = score;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MemberEvaluationDetail memberEvaluationDetail) || id == null) {
			return false;
		}
		return id.equals(memberEvaluationDetail.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}

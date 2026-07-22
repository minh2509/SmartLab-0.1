package com.smartlab.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.EvaluationCriteria;
import com.smartlab.entity.MemberEvaluation;
import com.smartlab.entity.MemberEvaluationDetail;

public interface MemberEvaluationDetailRepository extends JpaRepository<MemberEvaluationDetail, UUID> {

	boolean existsByEvaluationAndCriteria(MemberEvaluation evaluation, EvaluationCriteria criteria);

	List<MemberEvaluationDetail> findByEvaluation(MemberEvaluation evaluation);

	List<MemberEvaluationDetail> findByCriteria(EvaluationCriteria criteria);
}

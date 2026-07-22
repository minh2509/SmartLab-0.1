package com.smartlab.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.MemberEvaluation;
import com.smartlab.entity.Project;
import com.smartlab.entity.User;

public interface MemberEvaluationRepository extends JpaRepository<MemberEvaluation, UUID> {

	List<MemberEvaluation> findByProject(Project project);

	List<MemberEvaluation> findByMember(User member);

	List<MemberEvaluation> findByProjectAndMember(Project project, User member);

	List<MemberEvaluation> findByEvaluator(User evaluator);

	List<MemberEvaluation> findByProjectAndEvaluationPeriod(Project project, String evaluationPeriod);
}

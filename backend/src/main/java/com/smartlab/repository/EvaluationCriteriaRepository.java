package com.smartlab.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.EvaluationCriteria;

public interface EvaluationCriteriaRepository extends JpaRepository<EvaluationCriteria, UUID> {

	Optional<EvaluationCriteria> findByCode(String code);
}

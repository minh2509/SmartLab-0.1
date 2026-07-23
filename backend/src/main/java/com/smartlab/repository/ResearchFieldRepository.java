package com.smartlab.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.ResearchField;

public interface ResearchFieldRepository extends JpaRepository<ResearchField, UUID> {

	Optional<ResearchField> findByCode(String code);

	List<ResearchField> findByCodeIn(Collection<String> codes);
}

package com.smartlab.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Lab;

public interface LabRepository extends JpaRepository<Lab, UUID> {

	Optional<Lab> findByCode(String code);
}

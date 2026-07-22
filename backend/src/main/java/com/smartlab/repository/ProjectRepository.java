package com.smartlab.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Lab;
import com.smartlab.entity.Project;
import com.smartlab.enums.ProjectStatus;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

	Optional<Project> findByLabAndCode(Lab lab, String code);

	Optional<Project> findByLabAndSlug(Lab lab, String slug);

	boolean existsByLabAndCode(Lab lab, String code);

	boolean existsByLabAndSlug(Lab lab, String slug);

	List<Project> findByLabAndStatus(Lab lab, ProjectStatus status);
}

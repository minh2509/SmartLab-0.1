package com.smartlab.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartlab.entity.Lab;
import com.smartlab.entity.Project;
import com.smartlab.enums.ProjectStatus;
import com.smartlab.enums.ProjectType;
import com.smartlab.enums.ProjectVisibility;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

	Optional<Project> findByLabAndCode(Lab lab, String code);

	Optional<Project> findByLabAndSlug(Lab lab, String slug);

	boolean existsByLabAndCode(Lab lab, String code);

	boolean existsByLabAndSlug(Lab lab, String slug);

	List<Project> findByLabAndStatus(Lab lab, ProjectStatus status);

	Optional<Project> findByIdAndLabAndDeletedAtIsNull(UUID id, Lab lab);

	@Query("""
			select project
			from Project project
			where project.lab = :lab
			  and project.deletedAt is null
			  and project.status in :statuses
			  and project.projectType in :types
			  and project.visibility in :visibilities
			  and (
			      :researchFieldCode is null
			      or exists (
			          select projectField.id
			          from ProjectResearchField projectField
			          where projectField.project = project
			            and upper(projectField.researchField.code) = :researchFieldCode
			      )
			  )
			""")
	Page<Project> findAdminProjects(
			@Param("lab") Lab lab,
			@Param("statuses") Collection<ProjectStatus> statuses,
			@Param("types") Collection<ProjectType> types,
			@Param("visibilities") Collection<ProjectVisibility> visibilities,
			@Param("researchFieldCode") String researchFieldCode,
			Pageable pageable);
}

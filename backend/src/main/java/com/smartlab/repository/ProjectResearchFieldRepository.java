package com.smartlab.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Project;
import com.smartlab.entity.ProjectResearchField;
import com.smartlab.entity.ResearchField;

public interface ProjectResearchFieldRepository extends JpaRepository<ProjectResearchField, UUID> {

	boolean existsByProjectAndResearchField(Project project, ResearchField researchField);

	List<ProjectResearchField> findByProject(Project project);

	@EntityGraph(attributePaths = "researchField")
	List<ProjectResearchField> findByProjectIn(Collection<Project> projects);
}

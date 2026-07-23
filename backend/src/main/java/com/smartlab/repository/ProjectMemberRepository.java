package com.smartlab.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Project;
import com.smartlab.entity.ProjectMember;
import com.smartlab.entity.User;
import com.smartlab.enums.ProjectMemberStatus;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

	boolean existsByProjectAndUser(Project project, User user);

	List<ProjectMember> findByProject(Project project);

	List<ProjectMember> findByUser(User user);

	List<ProjectMember> findByProjectAndMemberStatus(Project project, ProjectMemberStatus memberStatus);

	@EntityGraph(attributePaths = "user")
	List<ProjectMember> findByProjectInAndMemberStatus(
			Collection<Project> projects,
			ProjectMemberStatus memberStatus);
}

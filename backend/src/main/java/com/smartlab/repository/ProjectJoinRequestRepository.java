package com.smartlab.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Project;
import com.smartlab.entity.ProjectJoinRequest;
import com.smartlab.entity.User;
import com.smartlab.enums.ProjectJoinRequestStatus;

public interface ProjectJoinRequestRepository extends JpaRepository<ProjectJoinRequest, UUID> {

	Optional<ProjectJoinRequest> findByProjectAndRequester(Project project, User requester);

	List<ProjectJoinRequest> findByProjectAndStatus(Project project, ProjectJoinRequestStatus status);

	List<ProjectJoinRequest> findByRequesterAndStatus(User requester, ProjectJoinRequestStatus status);

	boolean existsByProjectAndRequesterAndStatus(
			Project project,
			User requester,
			ProjectJoinRequestStatus status);
}

package com.smartlab.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartlab.entity.Project;
import com.smartlab.entity.ProjectJoinRequest;
import com.smartlab.entity.User;
import com.smartlab.enums.ProjectJoinRequestStatus;

import jakarta.persistence.LockModeType;

public interface ProjectJoinRequestRepository extends JpaRepository<ProjectJoinRequest, UUID> {

	Optional<ProjectJoinRequest> findByProjectAndRequester(Project project, User requester);

	List<ProjectJoinRequest> findByProjectAndStatus(Project project, ProjectJoinRequestStatus status);

	List<ProjectJoinRequest> findByRequesterAndStatus(User requester, ProjectJoinRequestStatus status);

	boolean existsByProjectAndRequesterAndStatus(
			Project project,
			User requester,
			ProjectJoinRequestStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select request from ProjectJoinRequest request where request.id = :requestId")
	Optional<ProjectJoinRequest> findByIdForUpdate(@Param("requestId") UUID requestId);
}

package com.smartlab.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartlab.entity.Project;
import com.smartlab.entity.ProjectMember;
import com.smartlab.entity.User;
import com.smartlab.enums.ProjectMemberStatus;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

	boolean existsByProjectAndUser(Project project, User user);

	List<ProjectMember> findByProject(Project project);

	List<ProjectMember> findByUser(User user);

	List<ProjectMember> findByProjectAndMemberStatus(Project project, ProjectMemberStatus memberStatus);

	@Query("""
			select member from ProjectMember member
			join fetch member.user recipient
			where member.project = :project
			and member.memberStatus = com.smartlab.enums.ProjectMemberStatus.ACTIVE
			and recipient.deletedAt is null
			and recipient.lab = member.project.lab
			""")
	List<ProjectMember> findEligibleNotificationRecipients(@Param("project") Project project);

	@Query("""
			select member.project.id as projectId, count(member.id) as recipientCount
			from ProjectMember member
			where member.project in :projects
			and member.memberStatus = com.smartlab.enums.ProjectMemberStatus.ACTIVE
			and member.user.deletedAt is null
			and member.user.lab = member.project.lab
			group by member.project.id
			""")
	List<ProjectRecipientCount> countEligibleNotificationRecipients(
			@Param("projects") Collection<Project> projects);

	interface ProjectRecipientCount {

		UUID getProjectId();

		long getRecipientCount();
	}
}

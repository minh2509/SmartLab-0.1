package com.smartlab.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartlab.entity.AuditLog;
import com.smartlab.entity.Lab;
import com.smartlab.entity.User;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

	List<AuditLog> findByLab(Lab lab);

	List<AuditLog> findByActor(User actor);

	List<AuditLog> findByAction(String action);

	List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);

	List<AuditLog> findByOrderByCreatedAtDesc();

	List<AuditLog> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

	@Query("""
			select auditLog
			from AuditLog auditLog
			where (:action is null or auditLog.action = :action)
				and (:actorId is null or auditLog.actor.id = :actorId)
				and (:entityType is null or auditLog.entityType = :entityType)
				and (:entityId is null or auditLog.entityId = :entityId)
				and (:start is null or auditLog.createdAt >= :start)
				and (:end is null or auditLog.createdAt <= :end)
			""")
	Page<AuditLog> searchAdminAuditLogs(
			@Param("action") String action,
			@Param("actorId") UUID actorId,
			@Param("entityType") String entityType,
			@Param("entityId") UUID entityId,
			@Param("start") OffsetDateTime start,
			@Param("end") OffsetDateTime end,
			Pageable pageable);
}

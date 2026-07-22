package com.smartlab.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

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
}

package com.smartlab.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Lab;
import com.smartlab.entity.Notification;
import com.smartlab.entity.User;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	List<Notification> findByLab(Lab lab);

	List<Notification> findByCreatedBy(User createdBy);

	List<Notification> findByNotificationType(String notificationType);

	List<Notification> findByLabOrderByCreatedAtDesc(Lab lab);

	List<Notification> findByRelatedTypeAndRelatedId(String relatedType, UUID relatedId);

	List<Notification> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);
}

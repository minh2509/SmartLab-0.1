package com.smartlab.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Notification;
import com.smartlab.entity.NotificationRecipient;
import com.smartlab.entity.User;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, UUID> {

	boolean existsByNotificationAndRecipient(Notification notification, User recipient);

	List<NotificationRecipient> findByNotification(Notification notification);

	List<NotificationRecipient> findByRecipient(User recipient);

	List<NotificationRecipient> findByRecipientAndReadAtIsNull(User recipient);

	List<NotificationRecipient> findByRecipientOrderByCreatedAtDesc(User recipient);

	long countByRecipientAndReadAtIsNull(User recipient);
}

package com.smartlab.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "notification_recipients",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_notification_recipients",
				columnNames = {"notification_id", "recipient_id"}))
public class NotificationRecipient {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "notification_id", nullable = false)
	private Notification notification;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipient_id", nullable = false)
	private User recipient;

	@Column(name = "read_at")
	private OffsetDateTime readAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Notification getNotification() {
		return notification;
	}

	public void setNotification(Notification notification) {
		this.notification = notification;
	}

	public User getRecipient() {
		return recipient;
	}

	public void setRecipient(User recipient) {
		this.recipient = recipient;
	}

	public OffsetDateTime getReadAt() {
		return readAt;
	}

	public void setReadAt(OffsetDateTime readAt) {
		this.readAt = readAt;
	}

	public OffsetDateTime getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(OffsetDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NotificationRecipient notificationRecipient) || id == null) {
			return false;
		}
		return id.equals(notificationRecipient.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}

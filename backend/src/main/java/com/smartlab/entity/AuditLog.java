package com.smartlab.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lab_id")
	private Lab lab;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_id")
	private User actor;

	@Column(name = "action", nullable = false, length = 100)
	private String action;

	@Column(name = "entity_type", nullable = false, length = 100)
	private String entityType;

	@Column(name = "entity_id")
	private UUID entityId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "old_value", columnDefinition = "jsonb")
	private String oldValue;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "new_value", columnDefinition = "jsonb")
	private String newValue;

	@Column(name = "ip_address", length = 100)
	private String ipAddress;

	@Column(name = "user_agent")
	private String userAgent;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Lab getLab() {
		return lab;
	}

	public void setLab(Lab lab) {
		this.lab = lab;
	}

	public User getActor() {
		return actor;
	}

	public void setActor(User actor) {
		this.actor = actor;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public UUID getEntityId() {
		return entityId;
	}

	public void setEntityId(UUID entityId) {
		this.entityId = entityId;
	}

	public String getOldValue() {
		return oldValue;
	}

	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}

	public String getNewValue() {
		return newValue;
	}

	public void setNewValue(String newValue) {
		this.newValue = newValue;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
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
		if (!(other instanceof AuditLog auditLog) || id == null) {
			return false;
		}
		return id.equals(auditLog.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}

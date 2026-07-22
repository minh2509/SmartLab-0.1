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

@Entity
@Table(name = "login_histories")
public class LoginHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "login_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime loginAt;

	@Column(name = "ip_address", length = 100)
	private String ipAddress;

	@Column(name = "user_agent")
	private String userAgent;

	@Column(name = "success", nullable = false)
	private Boolean success = false;

	@Column(name = "failure_reason")
	private String failureReason;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public OffsetDateTime getLoginAt() {
		return loginAt;
	}

	public void setLoginAt(OffsetDateTime loginAt) {
		this.loginAt = loginAt;
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

	public Boolean getSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(String failureReason) {
		this.failureReason = failureReason;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof LoginHistory loginHistory) || id == null) {
			return false;
		}
		return id.equals(loginHistory.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}

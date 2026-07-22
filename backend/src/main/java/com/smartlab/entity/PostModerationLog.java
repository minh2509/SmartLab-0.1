package com.smartlab.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.smartlab.enums.PostModerationAction;
import com.smartlab.enums.PostStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "post_moderation_logs")
public class PostModerationLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@Enumerated(EnumType.STRING)
	@Column(name = "action", nullable = false, length = 50)
	private PostModerationAction action;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 30)
	private PostStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", length = 30)
	private PostStatus toStatus;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_id")
	private User actor;

	@Column(name = "reason")
	private String reason;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Post getPost() {
		return post;
	}

	public void setPost(Post post) {
		this.post = post;
	}

	public PostModerationAction getAction() {
		return action;
	}

	public void setAction(PostModerationAction action) {
		this.action = action;
	}

	public PostStatus getFromStatus() {
		return fromStatus;
	}

	public void setFromStatus(PostStatus fromStatus) {
		this.fromStatus = fromStatus;
	}

	public PostStatus getToStatus() {
		return toStatus;
	}

	public void setToStatus(PostStatus toStatus) {
		this.toStatus = toStatus;
	}

	public User getActor() {
		return actor;
	}

	public void setActor(User actor) {
		this.actor = actor;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
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
		if (!(other instanceof PostModerationLog postModerationLog) || id == null) {
			return false;
		}
		return id.equals(postModerationLog.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}

package com.smartlab.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.smartlab.enums.FileScanStatus;
import com.smartlab.enums.FileVisibility;

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
@Table(name = "files")
public class File {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lab_id", nullable = false)
	private Lab lab;

	@Column(name = "original_name", nullable = false, length = 255)
	private String originalName;

	@Column(name = "stored_name", nullable = false, length = 255)
	private String storedName;

	@Column(name = "storage_path", nullable = false)
	private String storagePath;

	@Column(name = "mime_type", length = 150)
	private String mimeType;

	@Column(name = "file_size", nullable = false)
	private Long fileSize = 0L;

	@Column(name = "file_extension", length = 50)
	private String fileExtension;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "uploaded_by")
	private User uploadedBy;

	@Enumerated(EnumType.STRING)
	@Column(name = "visibility", nullable = false, length = 30)
	private FileVisibility visibility = FileVisibility.PRIVATE;

	@Enumerated(EnumType.STRING)
	@Column(name = "scan_status", nullable = false, length = 30)
	private FileScanStatus scanStatus = FileScanStatus.PENDING;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

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

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public String getStoredName() {
		return storedName;
	}

	public void setStoredName(String storedName) {
		this.storedName = storedName;
	}

	public String getStoragePath() {
		return storagePath;
	}

	public void setStoragePath(String storagePath) {
		this.storagePath = storagePath;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public String getFileExtension() {
		return fileExtension;
	}

	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}

	public User getUploadedBy() {
		return uploadedBy;
	}

	public void setUploadedBy(User uploadedBy) {
		this.uploadedBy = uploadedBy;
	}

	public FileVisibility getVisibility() {
		return visibility;
	}

	public void setVisibility(FileVisibility visibility) {
		this.visibility = visibility;
	}

	public FileScanStatus getScanStatus() {
		return scanStatus;
	}

	public void setScanStatus(FileScanStatus scanStatus) {
		this.scanStatus = scanStatus;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(OffsetDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof File file) || id == null) {
			return false;
		}
		return id.equals(file.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}

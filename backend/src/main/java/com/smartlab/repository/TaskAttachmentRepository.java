package com.smartlab.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.File;
import com.smartlab.entity.Task;
import com.smartlab.entity.TaskAttachment;
import com.smartlab.entity.User;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, UUID> {

	boolean existsByTaskAndFile(Task task, File file);

	List<TaskAttachment> findByTask(Task task);

	List<TaskAttachment> findByFile(File file);

	List<TaskAttachment> findByUploadedBy(User uploadedBy);
}

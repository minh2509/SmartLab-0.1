package com.smartlab.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Task;
import com.smartlab.entity.TaskReport;
import com.smartlab.entity.User;
import com.smartlab.enums.TaskReportStatus;

public interface TaskReportRepository extends JpaRepository<TaskReport, UUID> {

	List<TaskReport> findByTask(Task task);

	List<TaskReport> findByTaskOrderByCreatedAtDesc(Task task);

	List<TaskReport> findByReporter(User reporter);

	List<TaskReport> findByTaskAndStatus(Task task, TaskReportStatus status);

	List<TaskReport> findByReviewedBy(User reviewedBy);
}

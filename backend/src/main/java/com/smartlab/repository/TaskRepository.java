package com.smartlab.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Project;
import com.smartlab.entity.Task;
import com.smartlab.entity.User;
import com.smartlab.enums.TaskPriority;
import com.smartlab.enums.TaskStatus;

public interface TaskRepository extends JpaRepository<Task, UUID> {

	List<Task> findByProject(Project project);

	List<Task> findByProjectAndStatus(Project project, TaskStatus status);

	List<Task> findByProjectAndPriority(Project project, TaskPriority priority);

	List<Task> findByCreatedBy(User createdBy);

	List<Task> findByProjectAndDueDate(Project project, LocalDate dueDate);

	boolean existsByProjectAndId(Project project, UUID id);
}

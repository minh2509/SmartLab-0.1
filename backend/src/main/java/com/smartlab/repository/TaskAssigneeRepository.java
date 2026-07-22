package com.smartlab.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Task;
import com.smartlab.entity.TaskAssignee;
import com.smartlab.entity.User;
import com.smartlab.enums.TaskAssigneeStatus;

public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, UUID> {

	boolean existsByTaskAndUser(Task task, User user);

	List<TaskAssignee> findByTask(Task task);

	List<TaskAssignee> findByUser(User user);

	List<TaskAssignee> findByTaskAndStatus(Task task, TaskAssigneeStatus status);

	List<TaskAssignee> findByUserAndStatus(User user, TaskAssigneeStatus status);
}

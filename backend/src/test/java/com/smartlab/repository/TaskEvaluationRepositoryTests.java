package com.smartlab.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.EvaluationCriteria;
import com.smartlab.entity.File;
import com.smartlab.entity.MemberEvaluation;
import com.smartlab.entity.MemberEvaluationDetail;
import com.smartlab.entity.Project;
import com.smartlab.entity.Task;
import com.smartlab.entity.TaskAssignee;
import com.smartlab.entity.TaskAttachment;
import com.smartlab.entity.TaskReport;
import com.smartlab.entity.User;
import com.smartlab.enums.TaskAssigneeStatus;
import com.smartlab.enums.TaskPriority;
import com.smartlab.enums.TaskReportStatus;
import com.smartlab.enums.TaskStatus;

class TaskEvaluationRepositoryTests {

	@Test
	void repositoriesUseUuidJpaRepositoryContracts() {
		assertJpaRepository(TaskRepository.class);
		assertJpaRepository(TaskAssigneeRepository.class);
		assertJpaRepository(TaskReportRepository.class);
		assertJpaRepository(TaskAttachmentRepository.class);
		assertJpaRepository(MemberEvaluationRepository.class);
		assertJpaRepository(MemberEvaluationDetailRepository.class);
	}

	@Test
	void taskRepositoryExposesFocusedQueries() throws NoSuchMethodException {
		assertReturnType(TaskRepository.class.getMethod("findByProject", Project.class), List.class);
		assertReturnType(TaskRepository.class.getMethod("findByProjectAndStatus", Project.class, TaskStatus.class), List.class);
		assertReturnType(TaskRepository.class.getMethod("findByProjectAndPriority", Project.class, TaskPriority.class), List.class);
		assertReturnType(TaskRepository.class.getMethod("findByCreatedBy", User.class), List.class);
		assertReturnType(TaskRepository.class.getMethod("findByProjectAndDueDate", Project.class, LocalDate.class), List.class);
		assertReturnType(TaskRepository.class.getMethod("existsByProjectAndId", Project.class, UUID.class), boolean.class);
	}

	@Test
	void taskAssigneeRepositoryExposesFocusedQueries() throws NoSuchMethodException {
		assertReturnType(TaskAssigneeRepository.class.getMethod("existsByTaskAndUser", Task.class, User.class), boolean.class);
		assertReturnType(TaskAssigneeRepository.class.getMethod("findByTask", Task.class), List.class);
		assertReturnType(TaskAssigneeRepository.class.getMethod("findByUser", User.class), List.class);
		assertReturnType(
				TaskAssigneeRepository.class.getMethod("findByTaskAndStatus", Task.class, TaskAssigneeStatus.class),
				List.class);
		assertReturnType(
				TaskAssigneeRepository.class.getMethod("findByUserAndStatus", User.class, TaskAssigneeStatus.class),
				List.class);
	}

	@Test
	void taskReportAndAttachmentRepositoriesExposeFocusedQueries() throws NoSuchMethodException {
		assertReturnType(TaskReportRepository.class.getMethod("findByTask", Task.class), List.class);
		assertReturnType(TaskReportRepository.class.getMethod("findByTaskOrderByCreatedAtDesc", Task.class), List.class);
		assertReturnType(TaskReportRepository.class.getMethod("findByReporter", User.class), List.class);
		assertReturnType(
				TaskReportRepository.class.getMethod("findByTaskAndStatus", Task.class, TaskReportStatus.class),
				List.class);
		assertReturnType(TaskReportRepository.class.getMethod("findByReviewedBy", User.class), List.class);

		assertReturnType(TaskAttachmentRepository.class.getMethod("existsByTaskAndFile", Task.class, File.class), boolean.class);
		assertReturnType(TaskAttachmentRepository.class.getMethod("findByTask", Task.class), List.class);
		assertReturnType(TaskAttachmentRepository.class.getMethod("findByFile", File.class), List.class);
		assertReturnType(TaskAttachmentRepository.class.getMethod("findByUploadedBy", User.class), List.class);
	}

	@Test
	void memberEvaluationRepositoriesExposeFocusedQueries() throws NoSuchMethodException {
		assertReturnType(MemberEvaluationRepository.class.getMethod("findByProject", Project.class), List.class);
		assertReturnType(MemberEvaluationRepository.class.getMethod("findByMember", User.class), List.class);
		assertReturnType(
				MemberEvaluationRepository.class.getMethod("findByProjectAndMember", Project.class, User.class),
				List.class);
		assertReturnType(MemberEvaluationRepository.class.getMethod("findByEvaluator", User.class), List.class);
		assertReturnType(
				MemberEvaluationRepository.class.getMethod("findByProjectAndEvaluationPeriod", Project.class, String.class),
				List.class);

		assertReturnType(
				MemberEvaluationDetailRepository.class.getMethod(
						"existsByEvaluationAndCriteria",
						MemberEvaluation.class,
						EvaluationCriteria.class),
				boolean.class);
		assertReturnType(MemberEvaluationDetailRepository.class.getMethod("findByEvaluation", MemberEvaluation.class), List.class);
		assertReturnType(MemberEvaluationDetailRepository.class.getMethod("findByCriteria", EvaluationCriteria.class), List.class);
	}

	private static void assertJpaRepository(Class<?> repositoryType) {
		assertTrue(JpaRepository.class.isAssignableFrom(repositoryType));
		assertEquals(1, countEntitySpecificRepositoryInterfaces(repositoryType));
	}

	private static long countEntitySpecificRepositoryInterfaces(Class<?> repositoryType) {
		return java.util.Arrays.stream(repositoryType.getGenericInterfaces())
				.filter(type -> type.getTypeName().contains("<"))
				.filter(type -> type.getTypeName().contains(UUID.class.getName()))
				.filter(type -> type.getTypeName().contains(entityNameFor(repositoryType)))
				.count();
	}

	private static String entityNameFor(Class<?> repositoryType) {
		if (repositoryType == TaskRepository.class) {
			return Task.class.getName();
		}
		if (repositoryType == TaskAssigneeRepository.class) {
			return TaskAssignee.class.getName();
		}
		if (repositoryType == TaskReportRepository.class) {
			return TaskReport.class.getName();
		}
		if (repositoryType == TaskAttachmentRepository.class) {
			return TaskAttachment.class.getName();
		}
		if (repositoryType == MemberEvaluationRepository.class) {
			return MemberEvaluation.class.getName();
		}
		if (repositoryType == MemberEvaluationDetailRepository.class) {
			return MemberEvaluationDetail.class.getName();
		}
		throw new IllegalArgumentException("Unsupported repository type: " + repositoryType.getName());
	}

	private static void assertReturnType(Method method, Class<?> returnType) {
		assertEquals(returnType, method.getReturnType());
	}
}

package com.smartlab.service.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.entity.AuditLog;
import com.smartlab.entity.LoginHistory;
import com.smartlab.entity.User;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.repository.AuditLogRepository;
import com.smartlab.repository.LoginHistoryRepository;
import com.smartlab.repository.UserRepository;

@Service
@Profile("!nodb")
public class AdminAuditLoginService {

	private final AdminRolePolicy rolePolicy;
	private final AuditLogRepository auditLogRepository;
	private final LoginHistoryRepository loginHistoryRepository;
	private final UserRepository userRepository;

	public AdminAuditLoginService(
			AdminRolePolicy rolePolicy,
			AuditLogRepository auditLogRepository,
			LoginHistoryRepository loginHistoryRepository,
			UserRepository userRepository) {
		this.rolePolicy = rolePolicy;
		this.auditLogRepository = auditLogRepository;
		this.loginHistoryRepository = loginHistoryRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<AuditLog> listAuditLogs(UUID actorUserId) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		return auditLogRepository.findByLabOrderByCreatedAtDesc(actor.lab());
	}

	@Transactional(readOnly = true)
	public List<LoginHistory> listLoginHistories(UUID actorUserId) {
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		List<User> users = userRepository.findByLabAndAccountStatusNot(actor.lab(), UserAccountStatus.DELETED);
		if (users.isEmpty()) {
			return List.of();
		}
		return loginHistoryRepository.findByUserInOrderByLoginAtDesc(users);
	}

	@Transactional(readOnly = true)
	public List<LoginHistory> listLoginHistoriesForUser(UUID actorUserId, UUID userId) {
		if (userId == null) {
			throw new InvalidAdminServiceInputException("User ID must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(actorUserId);
		User user = rolePolicy.findUserInActorLab(actor, userId);
		return loginHistoryRepository.findByUserOrderByLoginAtDesc(user);
	}
}

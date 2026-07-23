package com.smartlab.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.LoginHistory;
import com.smartlab.entity.User;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID> {

	List<LoginHistory> findByUser(User user);

	List<LoginHistory> findBySuccess(Boolean success);

	List<LoginHistory> findByUserOrderByLoginAtDesc(User user);

	List<LoginHistory> findByUserInOrderByLoginAtDesc(List<User> users);

	List<LoginHistory> findByLoginAtBetween(OffsetDateTime start, OffsetDateTime end);

	List<LoginHistory> findByIpAddress(String ipAddress);
}

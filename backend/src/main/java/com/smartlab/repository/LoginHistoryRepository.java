package com.smartlab.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartlab.entity.LoginHistory;
import com.smartlab.entity.User;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID> {

	List<LoginHistory> findByUser(User user);

	List<LoginHistory> findBySuccess(Boolean success);

	List<LoginHistory> findByUserOrderByLoginAtDesc(User user);

	List<LoginHistory> findByLoginAtBetween(OffsetDateTime start, OffsetDateTime end);

	List<LoginHistory> findByIpAddress(String ipAddress);

	@Query("""
			select loginHistory
			from LoginHistory loginHistory
			where (:userId is null or loginHistory.user.id = :userId)
				and (:success is null or loginHistory.success = :success)
				and (:ipAddress is null or loginHistory.ipAddress = :ipAddress)
				and (:start is null or loginHistory.loginAt >= :start)
				and (:end is null or loginHistory.loginAt <= :end)
			""")
	Page<LoginHistory> searchAdminLoginHistories(
			@Param("userId") UUID userId,
			@Param("success") Boolean success,
			@Param("ipAddress") String ipAddress,
			@Param("start") OffsetDateTime start,
			@Param("end") OffsetDateTime end,
			Pageable pageable);
}

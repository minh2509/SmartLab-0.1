package com.smartlab.repository.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.smartlab.entity.AuditLog;

import jakarta.persistence.EntityManager;

@Repository
@Profile("!nodb")
public class AdminDashboardReadRepository {

	private final EntityManager entityManager;

	public AdminDashboardReadRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public DashboardCounts countVisibleRecords(UUID labId) {
		return new DashboardCounts(
				count("select count(u) from User u where u.lab.id = :labId and u.deletedAt is null", labId),
				count("select count(p) from Project p where p.lab.id = :labId and p.deletedAt is null", labId),
				count("select count(p) from Post p where p.lab.id = :labId and p.deletedAt is null", labId),
				count("""
						select count(r) from ProjectJoinRequest r
						where r.project.lab.id = :labId and r.project.deletedAt is null
						""", labId),
				count("""
						select count(t) from Task t
						where t.project.lab.id = :labId
						and t.deletedAt is null and t.project.deletedAt is null
						""", labId));
	}

	public List<AuditLog> findRecentActivities(UUID labId, int limit) {
		return entityManager.createQuery("""
				select a from AuditLog a
				left join fetch a.actor
				where a.lab.id = :labId
				order by a.createdAt desc
				""", AuditLog.class)
				.setParameter("labId", labId)
				.setMaxResults(limit)
				.getResultList();
	}

	private long count(String query, UUID labId) {
		return entityManager.createQuery(query, Long.class)
				.setParameter("labId", labId)
				.getSingleResult();
	}

	public record DashboardCounts(
			long users,
			long projects,
			long posts,
			long joinRequests,
			long tasks) {
	}
}

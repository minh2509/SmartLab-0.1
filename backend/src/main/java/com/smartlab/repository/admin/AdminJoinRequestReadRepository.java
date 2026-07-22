package com.smartlab.repository.admin;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.smartlab.entity.ProjectJoinRequest;
import com.smartlab.enums.ProjectJoinRequestStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Repository
@Profile("!nodb")
public class AdminJoinRequestReadRepository {

	private final EntityManager entityManager;

	public AdminJoinRequestReadRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public Page<ProjectJoinRequest> findPage(
			UUID labId,
			JoinRequestCriteria criteria,
			int page,
			int size) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<ProjectJoinRequest> dataQuery = builder.createQuery(ProjectJoinRequest.class);
		Root<ProjectJoinRequest> root = dataQuery.from(ProjectJoinRequest.class);
		root.fetch("project", JoinType.INNER);
		root.fetch("requester", JoinType.INNER);
		root.fetch("reviewedBy", JoinType.LEFT);
		root.fetch("cvFile", JoinType.LEFT);
		dataQuery.select(root)
				.where(predicates(builder, root, labId, criteria))
				.orderBy(builder.desc(root.get("createdAt")), builder.desc(root.get("id")));

		TypedQuery<ProjectJoinRequest> typedQuery = entityManager.createQuery(dataQuery);
		typedQuery.setFirstResult(page * size);
		typedQuery.setMaxResults(size);

		CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
		Root<ProjectJoinRequest> countRoot = countQuery.from(ProjectJoinRequest.class);
		countQuery.select(builder.count(countRoot))
				.where(predicates(builder, countRoot, labId, criteria));
		long total = entityManager.createQuery(countQuery).getSingleResult();
		return new PageImpl<>(typedQuery.getResultList(), PageRequest.of(page, size), total);
	}

	private static Predicate[] predicates(
			CriteriaBuilder builder,
			Root<ProjectJoinRequest> root,
			UUID labId,
			JoinRequestCriteria criteria) {
		List<Predicate> predicates = new ArrayList<>();
		predicates.add(builder.equal(root.get("project").get("lab").get("id"), labId));
		predicates.add(builder.isNull(root.get("project").get("deletedAt")));
		if (criteria.projectId() != null) {
			predicates.add(builder.equal(root.get("project").get("id"), criteria.projectId()));
		}
		if (criteria.status() != null) {
			predicates.add(builder.equal(root.get("status"), criteria.status()));
		}
		if (criteria.requesterId() != null) {
			predicates.add(builder.equal(root.get("requester").get("id"), criteria.requesterId()));
		}
		if (criteria.createdFrom() != null) {
			predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), criteria.createdFrom()));
		}
		if (criteria.createdTo() != null) {
			predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), criteria.createdTo()));
		}
		return predicates.toArray(Predicate[]::new);
	}

	public record JoinRequestCriteria(
			UUID projectId,
			ProjectJoinRequestStatus status,
			UUID requesterId,
			OffsetDateTime createdFrom,
			OffsetDateTime createdTo) {
	}
}

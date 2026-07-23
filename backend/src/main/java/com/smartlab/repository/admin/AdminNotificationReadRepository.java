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

import com.smartlab.entity.Notification;
import com.smartlab.entity.NotificationRecipient;
import com.smartlab.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Repository
@Profile("!nodb")
public class AdminNotificationReadRepository {

	private final EntityManager entityManager;

	public AdminNotificationReadRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public Page<Notification> findPage(
			UUID labId,
			NotificationCriteria criteria,
			int page,
			int size) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Notification> dataQuery = builder.createQuery(Notification.class);
		Root<Notification> root = dataQuery.from(Notification.class);
		root.fetch("createdBy", JoinType.LEFT);
		dataQuery.select(root)
				.where(predicates(builder, dataQuery, root, labId, criteria))
				.orderBy(builder.desc(root.get("createdAt")), builder.desc(root.get("id")));

		TypedQuery<Notification> typedQuery = entityManager.createQuery(dataQuery);
		typedQuery.setFirstResult(page * size);
		typedQuery.setMaxResults(size);

		CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
		Root<Notification> countRoot = countQuery.from(Notification.class);
		countQuery.select(builder.count(countRoot))
				.where(predicates(builder, countQuery, countRoot, labId, criteria));
		long total = entityManager.createQuery(countQuery).getSingleResult();
		return new PageImpl<>(typedQuery.getResultList(), PageRequest.of(page, size), total);
	}

	public NotificationFilterLookup findFilterOptions(UUID labId) {
		List<String> notificationTypes = entityManager.createQuery("""
				select distinct notification.notificationType
				from Notification notification
				where notification.lab.id = :labId
				and exists (
					select recipient.id
					from NotificationRecipient recipient
					where recipient.notification = notification
					and recipient.deletedAt is null
				)
				""", String.class)
				.setParameter("labId", labId)
				.getResultList();
		List<User> creators = entityManager.createQuery("""
				select distinct notification.createdBy
				from Notification notification
				where notification.lab.id = :labId
				and notification.createdBy is not null
				and notification.createdBy.lab.id = :labId
				and exists (
					select recipient.id
					from NotificationRecipient recipient
					where recipient.notification = notification
					and recipient.deletedAt is null
				)
				""", User.class)
				.setParameter("labId", labId)
				.getResultList();
		return new NotificationFilterLookup(notificationTypes, creators);
	}

	private static Predicate[] predicates(
			CriteriaBuilder builder,
			AbstractQuery<?> query,
			Root<Notification> root,
			UUID labId,
			NotificationCriteria criteria) {
		List<Predicate> predicates = new ArrayList<>();
		predicates.add(builder.equal(root.get("lab").get("id"), labId));

		Subquery<Integer> activeRecipient = query.subquery(Integer.class);
		Root<NotificationRecipient> recipient = activeRecipient.from(NotificationRecipient.class);
		activeRecipient.select(builder.literal(1));
		activeRecipient.where(
				builder.equal(recipient.get("notification"), root),
				builder.isNull(recipient.get("deletedAt")));
		predicates.add(builder.exists(activeRecipient));

		if (criteria.notificationType() != null) {
			predicates.add(builder.equal(root.get("notificationType"), criteria.notificationType()));
		}
		if (criteria.creatorId() != null) {
			predicates.add(builder.equal(root.get("createdBy").get("id"), criteria.creatorId()));
		}
		if (criteria.relatedType() != null) {
			predicates.add(builder.equal(root.get("relatedType"), criteria.relatedType()));
		}
		if (criteria.createdFrom() != null) {
			predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), criteria.createdFrom()));
		}
		if (criteria.createdTo() != null) {
			predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), criteria.createdTo()));
		}
		return predicates.toArray(Predicate[]::new);
	}

	public record NotificationCriteria(
			String notificationType,
			UUID creatorId,
			String relatedType,
			OffsetDateTime createdFrom,
			OffsetDateTime createdTo) {
	}

	public record NotificationFilterLookup(
			List<String> notificationTypes,
			List<User> creators) {

		public NotificationFilterLookup {
			notificationTypes = List.copyOf(notificationTypes);
			creators = List.copyOf(creators);
		}
	}
}

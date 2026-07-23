package com.smartlab.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartlab.entity.Lab;
import com.smartlab.entity.Post;
import com.smartlab.entity.PostCategory;
import com.smartlab.entity.Project;
import com.smartlab.entity.User;
import com.smartlab.enums.PostContentType;
import com.smartlab.enums.PostStatus;
import com.smartlab.enums.PostVisibility;

public interface PostRepository extends JpaRepository<Post, UUID> {

	Optional<Post> findByLabAndSlug(Lab lab, String slug);

	boolean existsByLabAndSlug(Lab lab, String slug);

	List<Post> findByLab(Lab lab);

	List<Post> findByLabAndModerationStatus(Lab lab, PostStatus moderationStatus);

	List<Post> findByLabAndVisibility(Lab lab, PostVisibility visibility);

	List<Post> findByProject(Project project);

	List<Post> findByProjectAndModerationStatus(Project project, PostStatus moderationStatus);

	List<Post> findByAuthor(User author);

	List<Post> findByCategory(PostCategory category);

	List<Post> findByLabAndPublishedAtIsNotNullOrderByPublishedAtDesc(Lab lab);

	@EntityGraph(attributePaths = {"author", "project", "category", "coverFile"})
	@Query(
			value = """
					select post
					from Post post
					where post.lab = :lab
						and post.deletedAt is null
						and (:keywordPattern is null
							or lower(post.title) like :keywordPattern escape '!'
							or lower(post.slug) like :keywordPattern escape '!'
							or lower(post.summary) like :keywordPattern escape '!')
						and (:status is null or post.moderationStatus = :status)
						and (:contentType is null or post.contentType = :contentType)
						and (:authorId is null or post.author.id = :authorId)
						and (:projectId is null or post.project.id = :projectId)
						and (:visibility is null or post.visibility = :visibility)
					order by post.createdAt desc, post.id desc
					""",
			countQuery = """
					select count(post)
					from Post post
					where post.lab = :lab
						and post.deletedAt is null
						and (:keywordPattern is null
							or lower(post.title) like :keywordPattern escape '!'
							or lower(post.slug) like :keywordPattern escape '!'
							or lower(post.summary) like :keywordPattern escape '!')
						and (:status is null or post.moderationStatus = :status)
						and (:contentType is null or post.contentType = :contentType)
						and (:authorId is null or post.author.id = :authorId)
						and (:projectId is null or post.project.id = :projectId)
						and (:visibility is null or post.visibility = :visibility)
					""")
	Page<Post> findAdminPosts(
			@Param("lab") Lab lab,
			@Param("keywordPattern") String keywordPattern,
			@Param("status") PostStatus status,
			@Param("contentType") PostContentType contentType,
			@Param("authorId") UUID authorId,
			@Param("projectId") UUID projectId,
			@Param("visibility") PostVisibility visibility,
			Pageable pageable);

	@Query(
			value = """
					select post.id
					from posts post
					left join (
						select log.post_id, max(log.created_at) as latest_submitted_at
						from post_moderation_logs log
						where log.action = 'SUBMIT'
						group by log.post_id
					) latest_submit on latest_submit.post_id = post.id
					where post.lab_id = :labId
						and post.deleted_at is null
						and post.moderation_status = 'PENDING_REVIEW'
					order by latest_submit.latest_submitted_at asc nulls last, post.id asc
					""",
			countQuery = """
					select count(*)
					from posts post
					where post.lab_id = :labId
						and post.deleted_at is null
						and post.moderation_status = 'PENDING_REVIEW'
					""",
			nativeQuery = true)
	Page<UUID> findPendingAdminPostIds(
			@Param("labId") UUID labId,
			Pageable pageable);

	@EntityGraph(attributePaths = {"author", "project", "category", "coverFile"})
	@Query("""
			select post
			from Post post
			where post.lab.id = :labId
				and post.deletedAt is null
				and post.moderationStatus = com.smartlab.enums.PostStatus.PENDING_REVIEW
				and post.id in :ids
			""")
	List<Post> findPendingAdminPostsByIdIn(
			@Param("labId") UUID labId,
			@Param("ids") Collection<UUID> ids);

	@EntityGraph(attributePaths = {"author", "project", "category", "coverFile"})
	@Query("""
			select post
			from Post post
			where post.id = :postId
				and post.lab.id = :labId
				and post.deletedAt is null
			""")
	Optional<Post> findAdminPostDetail(
			@Param("labId") UUID labId,
			@Param("postId") UUID postId);
}

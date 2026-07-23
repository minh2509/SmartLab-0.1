package com.smartlab.repository;

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
							or lower(post.title) like :keywordPattern
							or lower(post.slug) like :keywordPattern
							or lower(post.summary) like :keywordPattern)
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
							or lower(post.title) like :keywordPattern
							or lower(post.slug) like :keywordPattern
							or lower(post.summary) like :keywordPattern)
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
}

package com.smartlab.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartlab.entity.Post;
import com.smartlab.entity.PostModerationLog;
import com.smartlab.entity.User;
import com.smartlab.enums.PostModerationAction;

public interface PostModerationLogRepository extends JpaRepository<PostModerationLog, UUID> {

	List<PostModerationLog> findByPostOrderByCreatedAtAsc(Post post);

	List<PostModerationLog> findByPostOrderByCreatedAtDesc(Post post);

	List<PostModerationLog> findByActor(User actor);

	List<PostModerationLog> findByPostAndAction(Post post, PostModerationAction action);

	@EntityGraph(attributePaths = {"actor"})
	@Query("""
			select log
			from PostModerationLog log
			where log.post = :post
			order by log.createdAt asc, log.id asc
			""")
	List<PostModerationLog> findAdminPostModerationHistory(
			@Param("post") Post post);
}

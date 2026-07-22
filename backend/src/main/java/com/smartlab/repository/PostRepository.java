package com.smartlab.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Lab;
import com.smartlab.entity.Post;
import com.smartlab.entity.PostCategory;
import com.smartlab.entity.Project;
import com.smartlab.entity.User;
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
}

package com.smartlab.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartlab.entity.File;
import com.smartlab.entity.Post;
import com.smartlab.entity.PostAttachment;
import com.smartlab.entity.User;

public interface PostAttachmentRepository extends JpaRepository<PostAttachment, UUID> {

	boolean existsByPostAndFile(Post post, File file);

	List<PostAttachment> findByPost(Post post);

	List<PostAttachment> findByFile(File file);

	List<PostAttachment> findByUploadedBy(User uploadedBy);

	@EntityGraph(attributePaths = {"file", "uploadedBy"})
	@Query("""
			select attachment
			from PostAttachment attachment
			where attachment.post = :post
				and attachment.file.deletedAt is null
			order by attachment.createdAt asc, attachment.id asc
			""")
	List<PostAttachment> findVisibleAdminPostAttachments(
			@Param("post") Post post);
}

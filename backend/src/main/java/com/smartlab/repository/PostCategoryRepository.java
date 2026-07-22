package com.smartlab.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.PostCategory;

public interface PostCategoryRepository extends JpaRepository<PostCategory, UUID> {

	Optional<PostCategory> findByCode(String code);
}

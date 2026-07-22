package com.smartlab.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.File;

public interface FileRepository extends JpaRepository<File, UUID> {
}

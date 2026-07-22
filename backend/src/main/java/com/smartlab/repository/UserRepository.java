package com.smartlab.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Lab;
import com.smartlab.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByLabAndUsername(Lab lab, String username);

	Optional<User> findByLabAndEmail(Lab lab, String email);
}

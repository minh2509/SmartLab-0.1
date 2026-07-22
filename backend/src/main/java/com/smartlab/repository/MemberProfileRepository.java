package com.smartlab.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.MemberProfile;
import com.smartlab.entity.User;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, UUID> {

	Optional<MemberProfile> findByUser(User user);
}

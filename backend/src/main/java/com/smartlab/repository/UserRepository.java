package com.smartlab.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartlab.entity.Lab;
import com.smartlab.entity.User;
import com.smartlab.enums.UserAccountStatus;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByLabAndUsername(Lab lab, String username);

	Optional<User> findByLabAndEmail(Lab lab, String email);

	boolean existsByLabAndUsername(Lab lab, String username);

	boolean existsByLabAndEmail(Lab lab, String email);

	boolean existsByLabAndUsernameAndIdNot(Lab lab, String username, UUID id);

	boolean existsByLabAndEmailAndIdNot(Lab lab, String email, UUID id);

	List<User> findByLab(Lab lab);

	List<User> findByAccountStatus(UserAccountStatus accountStatus);

	List<User> findByLabAndAccountStatus(Lab lab, UserAccountStatus accountStatus);
}

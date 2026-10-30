package com.aeroai.repository;

import com.aeroai.model.RegisteredUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegisteredUserRepository extends JpaRepository<RegisteredUser, Long> {
    boolean existsByEmail(String email);
    java.util.Optional<RegisteredUser> findByEmail(String email);
}

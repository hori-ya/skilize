package com.skilize.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUserId(String userId);
    List<User> findAllByOrderByUserIdAsc();
    List<User> findByTlUserIdAndActiveTrue(int tlUserId);
    List<User> findByActiveTrue();
}

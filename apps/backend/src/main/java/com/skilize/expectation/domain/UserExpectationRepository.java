package com.skilize.expectation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserExpectationRepository extends JpaRepository<UserExpectation, Integer> {
    Optional<UserExpectation> findByUserId(Integer userId);
}

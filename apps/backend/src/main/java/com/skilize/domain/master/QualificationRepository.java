package com.skilize.domain.master;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QualificationRepository extends JpaRepository<Qualification, Integer> {

    @Query("SELECT q FROM Qualification q LEFT JOIN FETCH q.category ORDER BY q.sortOrder ASC")
    List<Qualification> findAllWithCategory();

    @Query("SELECT q FROM Qualification q LEFT JOIN FETCH q.category WHERE q.active = true ORDER BY q.sortOrder ASC")
    List<Qualification> findAllActiveWithCategory();

    @Query("SELECT q FROM Qualification q LEFT JOIN FETCH q.category WHERE q.active = :active ORDER BY q.sortOrder ASC")
    List<Qualification> findAllWithCategoryByActive(@Param("active") boolean active);
}

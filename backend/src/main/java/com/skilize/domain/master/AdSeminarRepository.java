package com.skilize.domain.master;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdSeminarRepository extends JpaRepository<AdSeminar, Integer> {

    @Query("SELECT a FROM AdSeminar a LEFT JOIN FETCH a.category ORDER BY a.sortOrder ASC")
    List<AdSeminar> findAllWithCategory();

    @Query("SELECT a FROM AdSeminar a LEFT JOIN FETCH a.category WHERE a.active = true ORDER BY a.sortOrder ASC")
    List<AdSeminar> findAllActiveWithCategory();

    @Query("SELECT a FROM AdSeminar a LEFT JOIN FETCH a.category WHERE a.active = :active ORDER BY a.sortOrder ASC")
    List<AdSeminar> findAllWithCategoryByActive(@Param("active") boolean active);
}

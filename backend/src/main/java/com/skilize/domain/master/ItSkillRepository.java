package com.skilize.domain.master;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItSkillRepository extends JpaRepository<ItSkill, Integer> {

    @Query("SELECT s FROM ItSkill s JOIN FETCH s.category ORDER BY s.category.sortOrder ASC, s.sortOrder ASC")
    List<ItSkill> findAllWithCategory();

    @Query("SELECT s FROM ItSkill s JOIN FETCH s.category WHERE s.active = true ORDER BY s.category.sortOrder ASC, s.sortOrder ASC")
    List<ItSkill> findAllActiveWithCategory();

    @Query("SELECT s FROM ItSkill s JOIN FETCH s.category WHERE s.active = :active ORDER BY s.category.sortOrder ASC, s.sortOrder ASC")
    List<ItSkill> findAllWithCategoryByActive(@Param("active") boolean active);
}

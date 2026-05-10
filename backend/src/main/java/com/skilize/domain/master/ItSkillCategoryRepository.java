package com.skilize.domain.master;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItSkillCategoryRepository extends JpaRepository<ItSkillCategory, Integer> {
    List<ItSkillCategory> findByActiveTrueOrderBySortOrderAsc();
    List<ItSkillCategory> findByLevelAndActiveTrueOrderBySortOrderAsc(short level);
    List<ItSkillCategory> findAllByOrderByLevelAscSortOrderAsc();
}

package com.skilize.domain.master;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillLevelRepository extends JpaRepository<SkillLevel, Integer> {
    List<SkillLevel> findAllByOrderByLevelValueAsc();
    List<SkillLevel> findByActiveOrderByLevelValueAsc(boolean active);
}

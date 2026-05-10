package com.skilize.domain.master;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeminarCategoryRepository extends JpaRepository<SeminarCategory, Integer> {
    List<SeminarCategory> findByActiveTrueOrderBySortOrderAsc();
}

package com.skilize.master.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QualificationCategoryRepository extends JpaRepository<QualificationCategory, Integer> {
    List<QualificationCategory> findByActiveTrueOrderBySortOrderAsc();
    List<QualificationCategory> findAllByOrderBySortOrderAsc();
}

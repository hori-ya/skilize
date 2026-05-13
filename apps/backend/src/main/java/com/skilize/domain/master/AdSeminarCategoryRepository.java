package com.skilize.domain.master;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdSeminarCategoryRepository extends JpaRepository<AdSeminarCategory, Integer> {
    List<AdSeminarCategory> findByActiveTrueOrderBySortOrderAsc();
    List<AdSeminarCategory> findAllByOrderBySortOrderAsc();
}

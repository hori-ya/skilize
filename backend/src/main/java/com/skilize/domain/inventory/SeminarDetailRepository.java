package com.skilize.domain.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeminarDetailRepository extends JpaRepository<SeminarDetail, Integer> {

    @Query("SELECT d FROM SeminarDetail d LEFT JOIN FETCH d.adSeminar ads LEFT JOIN FETCH ads.category LEFT JOIN FETCH d.seminarCategory WHERE d.inventory.id = :inventoryId")
    List<SeminarDetail> findByInventoryId(@Param("inventoryId") int inventoryId);

    @Modifying
    @Query("DELETE FROM SeminarDetail d WHERE d.inventory.id = :inventoryId")
    void deleteByInventoryId(@Param("inventoryId") int inventoryId);
}

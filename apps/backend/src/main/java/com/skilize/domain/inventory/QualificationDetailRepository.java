package com.skilize.domain.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QualificationDetailRepository extends JpaRepository<QualificationDetail, Integer> {

    @Query("SELECT d FROM QualificationDetail d LEFT JOIN FETCH d.qualification q LEFT JOIN FETCH q.category WHERE d.inventory.id = :inventoryId")
    List<QualificationDetail> findByInventoryId(@Param("inventoryId") int inventoryId);

    @Modifying
    @Query("DELETE FROM QualificationDetail d WHERE d.inventory.id = :inventoryId")
    void deleteByInventoryId(@Param("inventoryId") int inventoryId);
}

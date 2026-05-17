package com.skilize.interview.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryInterviewRepository extends JpaRepository<InventoryInterview, Integer> {

    @Query("SELECT i FROM InventoryInterview i JOIN FETCH i.interviewer WHERE i.inventory.id = :inventoryId AND i.interviewer.id = :interviewerId")
    Optional<InventoryInterview> findByInventoryIdAndInterviewerId(@Param("inventoryId") int inventoryId,
                                                                    @Param("interviewerId") int interviewerId);
}

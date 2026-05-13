package com.skilize.domain.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryGoalRepository extends JpaRepository<InventoryGoal, Integer> {

    @Query("SELECT g FROM InventoryGoal g LEFT JOIN FETCH g.itSkill LEFT JOIN FETCH g.qualification LEFT JOIN FETCH g.adSeminar WHERE g.inventory.id = :inventoryId")
    List<InventoryGoal> findByInventoryId(@Param("inventoryId") int inventoryId);

    @Modifying
    @Query("DELETE FROM InventoryGoal g WHERE g.inventory.id = :inventoryId")
    void deleteByInventoryId(@Param("inventoryId") int inventoryId);
}

package com.skilize.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItSkillDetailRepository extends JpaRepository<ItSkillDetail, Integer> {

    @Query("SELECT d FROM ItSkillDetail d LEFT JOIN FETCH d.itSkill LEFT JOIN FETCH d.skillLevel WHERE d.inventory.id = :inventoryId")
    List<ItSkillDetail> findByInventoryId(@Param("inventoryId") int inventoryId);

    @Modifying
    @Query("DELETE FROM ItSkillDetail d WHERE d.inventory.id = :inventoryId")
    void deleteByInventoryId(@Param("inventoryId") int inventoryId);
}

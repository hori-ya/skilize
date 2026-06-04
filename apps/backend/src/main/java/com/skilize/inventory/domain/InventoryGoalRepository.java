package com.skilize.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 目標リポジトリ。全件洗い替えパターン（deleteByInventoryId → saveAll）で使用する。
 */
public interface InventoryGoalRepository extends JpaRepository<InventoryGoal, Integer> {

    /** ITスキル・資格・ADセミナーを LEFT JOIN FETCH で一括取得する（N+1 回避）。 */
    @Query("SELECT g FROM InventoryGoal g LEFT JOIN FETCH g.itSkill LEFT JOIN FETCH g.qualification LEFT JOIN FETCH g.adSeminar WHERE g.inventory.id = :inventoryId")
    List<InventoryGoal> findByInventoryId(@Param("inventoryId") int inventoryId);

    /** 帳票出力用: ITスキル・カテゴリ・親カテゴリ・資格・ADセミナー・ADカテゴリを一括取得する（N+1 回避）。 */
    @Query("SELECT g FROM InventoryGoal g LEFT JOIN FETCH g.itSkill s LEFT JOIN FETCH s.category c LEFT JOIN FETCH c.parent LEFT JOIN FETCH g.qualification LEFT JOIN FETCH g.adSeminar ads LEFT JOIN FETCH ads.category WHERE g.inventory.id = :inventoryId")
    List<InventoryGoal> findByInventoryIdForReport(@Param("inventoryId") int inventoryId);

    /** 指定棚卸の目標を全件削除する。全件洗い替え時の DELETE に使用する。 */
    @Modifying
    @Query("DELETE FROM InventoryGoal g WHERE g.inventory.id = :inventoryId")
    void deleteByInventoryId(@Param("inventoryId") int inventoryId);
}

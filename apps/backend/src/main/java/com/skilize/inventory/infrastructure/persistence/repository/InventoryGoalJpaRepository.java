/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 目標の永続化を担う Spring Data JPA リポジトリ。
 * 全件洗い替えパターン（deleteByInventoryId → saveAll）をサポートするクエリを提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.InventoryGoalRepository から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.repository;

import com.skilize.inventory.infrastructure.persistence.entity.InventoryGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 目標 Spring Data JPA リポジトリ。全件洗い替えパターン（deleteByInventoryId → saveAll）で使用する。 */
public interface InventoryGoalJpaRepository extends JpaRepository<InventoryGoalEntity, Integer> {

    /** ITスキル・資格・ADセミナーを LEFT JOIN FETCH で一括取得する（N+1 回避）。 */
    @Query("SELECT g FROM InventoryGoalEntity g LEFT JOIN FETCH g.itSkill LEFT JOIN FETCH g.qualification LEFT JOIN FETCH g.adSeminar WHERE g.inventory.id = :inventoryId")
    List<InventoryGoalEntity> findByInventoryId(@Param("inventoryId") int inventoryId);

    /** 帳票出力用: ITスキル・カテゴリ・親カテゴリ・資格・ADセミナー・ADカテゴリを一括取得する（N+1 回避）。 */
    @Query("SELECT g FROM InventoryGoalEntity g LEFT JOIN FETCH g.itSkill s LEFT JOIN FETCH s.category c LEFT JOIN FETCH c.parent LEFT JOIN FETCH g.qualification LEFT JOIN FETCH g.adSeminar ads LEFT JOIN FETCH ads.category WHERE g.inventory.id = :inventoryId")
    List<InventoryGoalEntity> findByInventoryIdForReport(@Param("inventoryId") int inventoryId);

    /** 指定棚卸の目標を全件削除する。全件洗い替え時の DELETE に使用する。 */
    @Modifying
    @Query("DELETE FROM InventoryGoalEntity g WHERE g.inventory.id = :inventoryId")
    void deleteByInventoryId(@Param("inventoryId") int inventoryId);
}

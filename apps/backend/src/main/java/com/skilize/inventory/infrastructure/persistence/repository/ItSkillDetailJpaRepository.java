/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキル明細の永続化を担う Spring Data JPA リポジトリ。
 * 全件洗い替え・カスタムスキル昇格・帳票出力用の各クエリを提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.ItSkillDetailRepository から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.repository;

import com.skilize.inventory.infrastructure.persistence.entity.ItSkillDetailEntity;
import com.skilize.master.infrastructure.persistence.entity.ItSkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** ITスキル明細 Spring Data JPA リポジトリ。全件洗い替えパターン（deleteByInventoryId → saveAll）で使用する。 */
public interface ItSkillDetailJpaRepository extends JpaRepository<ItSkillDetailEntity, Integer> {

    /** ITスキル・スキルレベルを JOIN FETCH で一括取得する（N+1 回避）。 */
    @Query("SELECT d FROM ItSkillDetailEntity d LEFT JOIN FETCH d.itSkill LEFT JOIN FETCH d.skillLevel WHERE d.inventory.id = :inventoryId")
    List<ItSkillDetailEntity> findByInventoryId(@Param("inventoryId") int inventoryId);

    /**
     * 指定棚卸のITスキル明細を全件削除する。
     * @Modifying: SELECT 以外（INSERT/UPDATE/DELETE）のクエリに必須。
     */
    @Modifying
    @Query("DELETE FROM ItSkillDetailEntity d WHERE d.inventory.id = :inventoryId")
    void deleteByInventoryId(@Param("inventoryId") int inventoryId);

    /** 帳票出力用: ITスキル・スキルレベル・カテゴリ・親カテゴリを一括取得する（N+1 回避）。 */
    @Query("SELECT d FROM ItSkillDetailEntity d LEFT JOIN FETCH d.itSkill s LEFT JOIN FETCH s.category c LEFT JOIN FETCH c.parent LEFT JOIN FETCH d.skillLevel WHERE d.inventory.id = :inventoryId")
    List<ItSkillDetailEntity> findByInventoryIdWithCategories(@Param("inventoryId") int inventoryId);

    /** カスタムスキル名のうち it_skills マスタに未登録のものを使用件数付きで返す。 */
    @Query("SELECT d.customSkillName, COUNT(d) FROM ItSkillDetailEntity d " +
           "WHERE d.itSkill IS NULL AND d.customSkillName IS NOT NULL " +
           "AND NOT EXISTS (SELECT s FROM ItSkillEntity s WHERE s.name = d.customSkillName) " +
           "GROUP BY d.customSkillName ORDER BY COUNT(d) DESC")
    List<Object[]> findCustomUnregisteredSkillNames();

    /** 昇格後、同名カスタムスキル明細をマスタスキルへ紐付ける。 */
    @Modifying
    @Query("UPDATE ItSkillDetailEntity d SET d.itSkill = :skill, d.customSkillName = null " +
           "WHERE d.customSkillName = :customName AND d.itSkill IS NULL")
    void linkToMasterSkill(@Param("customName") String customName, @Param("skill") ItSkillEntity skill);
}

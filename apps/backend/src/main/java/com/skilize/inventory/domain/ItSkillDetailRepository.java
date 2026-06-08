/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキル明細リポジトリインターフェース。
 * 全件洗い替え・カスタムスキル昇格・帳票出力用の各クエリを提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.domain;

import com.skilize.master.domain.ItSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * ITスキル明細リポジトリ。全件洗い替えパターン（deleteByInventoryId → saveAll）で使用する。
 */
public interface ItSkillDetailRepository extends JpaRepository<ItSkillDetail, Integer> {

    /** ITスキル・スキルレベルを JOIN FETCH で一括取得する（N+1 回避）。 */
    @Query("SELECT d FROM ItSkillDetail d LEFT JOIN FETCH d.itSkill LEFT JOIN FETCH d.skillLevel WHERE d.inventory.id = :inventoryId")
    List<ItSkillDetail> findByInventoryId(@Param("inventoryId") int inventoryId);

    /**
     * 指定棚卸のITスキル明細を全件削除する。
     * @Modifying: SELECT 以外（INSERT/UPDATE/DELETE）のクエリに必須。
     * 全件洗い替え時の DELETE に使用する。
     */
    @Modifying
    @Query("DELETE FROM ItSkillDetail d WHERE d.inventory.id = :inventoryId")
    void deleteByInventoryId(@Param("inventoryId") int inventoryId);

    /** 帳票出力用: ITスキル・スキルレベル・カテゴリ・親カテゴリを一括取得する（N+1 回避）。 */
    @Query("SELECT d FROM ItSkillDetail d LEFT JOIN FETCH d.itSkill s LEFT JOIN FETCH s.category c LEFT JOIN FETCH c.parent LEFT JOIN FETCH d.skillLevel WHERE d.inventory.id = :inventoryId")
    List<ItSkillDetail> findByInventoryIdWithCategories(@Param("inventoryId") int inventoryId);

    /** カスタムスキル名のうち it_skills マスタに未登録のものを使用件数付きで返す。 */
    @Query("SELECT d.customSkillName, COUNT(d) FROM ItSkillDetail d " +
           "WHERE d.itSkill IS NULL AND d.customSkillName IS NOT NULL " +
           "AND NOT EXISTS (SELECT s FROM ItSkill s WHERE s.name = d.customSkillName) " +
           "GROUP BY d.customSkillName ORDER BY COUNT(d) DESC")
    List<Object[]> findCustomUnregisteredSkillNames();

    /** 昇格後、同名カスタムスキル明細をマスタスキルへ紐付ける。 */
    @Modifying
    @Query("UPDATE ItSkillDetail d SET d.itSkill = :skill, d.customSkillName = null " +
           "WHERE d.customSkillName = :customName AND d.itSkill IS NULL")
    void linkToMasterSkill(@Param("customName") String customName, @Param("skill") ItSkill skill);
}

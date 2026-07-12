/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 資格明細の永続化を担う Spring Data JPA リポジトリ。
 * 全件洗い替え・カスタム資格昇格用の各クエリを提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.QualificationDetailRepository から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.repository;

import com.skilize.inventory.infrastructure.persistence.entity.QualificationDetailEntity;
import com.skilize.master.infrastructure.persistence.entity.QualificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 資格明細 Spring Data JPA リポジトリ。全件洗い替えパターン（deleteByInventoryId → saveAll）で使用する。 */
public interface QualificationDetailJpaRepository extends JpaRepository<QualificationDetailEntity, Integer> {

    /** 資格・資格分類を JOIN FETCH で一括取得する（N+1 回避）。 */
    @Query("SELECT d FROM QualificationDetailEntity d LEFT JOIN FETCH d.qualification q LEFT JOIN FETCH q.category WHERE d.inventory.id = :inventoryId")
    List<QualificationDetailEntity> findByInventoryId(@Param("inventoryId") int inventoryId);

    /** 指定棚卸の資格明細を全件削除する。全件洗い替え時の DELETE に使用する。 */
    @Modifying
    @Query("DELETE FROM QualificationDetailEntity d WHERE d.inventory.id = :inventoryId")
    void deleteByInventoryId(@Param("inventoryId") int inventoryId);

    /** カスタム資格名のうち qualifications マスタに未登録のものを使用件数付きで返す。 */
    @Query("SELECT d.customQualificationName, COUNT(d) FROM QualificationDetailEntity d " +
           "WHERE d.qualification IS NULL AND d.customQualificationName IS NOT NULL " +
           "AND NOT EXISTS (SELECT q FROM QualificationEntity q WHERE q.name = d.customQualificationName) " +
           "GROUP BY d.customQualificationName ORDER BY COUNT(d) DESC")
    List<Object[]> findCustomUnregisteredQualificationNames();

    /** 昇格後、同名カスタム資格明細をマスタ資格へ紐付ける。 */
    @Modifying
    @Query("UPDATE QualificationDetailEntity d SET d.qualification = :qualification, d.customQualificationName = null " +
           "WHERE d.customQualificationName = :customName AND d.qualification IS NULL")
    void linkToMasterQualification(@Param("customName") String customName,
                                    @Param("qualification") QualificationEntity qualification);
}

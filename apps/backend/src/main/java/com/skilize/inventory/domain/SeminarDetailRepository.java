/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * セミナー明細リポジトリインターフェース。
 * 全件洗い替えパターン（deleteByInventoryId → saveAll）をサポートするクエリを提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * セミナー明細リポジトリ。全件洗い替えパターン（deleteByInventoryId → saveAll）で使用する。
 */
public interface SeminarDetailRepository extends JpaRepository<SeminarDetail, Integer> {

    /** ADセミナー・ADセミナー分類・セミナー分類を JOIN FETCH で一括取得する（N+1 回避）。 */
    @Query("SELECT d FROM SeminarDetail d LEFT JOIN FETCH d.adSeminar ads LEFT JOIN FETCH ads.category LEFT JOIN FETCH d.seminarCategory WHERE d.inventory.id = :inventoryId")
    List<SeminarDetail> findByInventoryId(@Param("inventoryId") int inventoryId);

    /** 指定棚卸のセミナー明細を全件削除する。全件洗い替え時の DELETE に使用する。 */
    @Modifying
    @Query("DELETE FROM SeminarDetail d WHERE d.inventory.id = :inventoryId")
    void deleteByInventoryId(@Param("inventoryId") int inventoryId);
}

/**************************************************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 面談メモヘッダーリポジトリ。inventory_interviews テーブルへのデータアクセスを提供する。
 * 棚卸IDと面談者IDの組み合わせで一意な面談メモを取得するクエリを定義する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.interview.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 面談メモヘッダーリポジトリ。
 * 面談メモは「棚卸 × 面談者」の組み合わせで一意。同一棚卸に複数の TL が面談した場合は複数レコードになる。
 */
public interface InventoryInterviewRepository extends JpaRepository<InventoryInterview, Integer> {

    /**
     * 指定棚卸・指定面談者の面談メモを返す。
     * JOIN FETCH で面談者（User）を一緒に取得し、インタビュアー名表示での追加クエリを防ぐ。
     */
    @Query("SELECT i FROM InventoryInterview i JOIN FETCH i.interviewer WHERE i.inventory.id = :inventoryId AND i.interviewer.id = :interviewerId")
    Optional<InventoryInterview> findByInventoryIdAndInterviewerId(@Param("inventoryId") int inventoryId,
                                                                    @Param("interviewerId") int interviewerId);
}

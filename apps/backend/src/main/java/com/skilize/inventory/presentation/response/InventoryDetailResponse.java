/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 棚卸詳細レスポンス。GET /api/inventories/{id} のレスポンスとして使用する。
 * サマリより多くのタイムスタンプ情報を含む。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.presentation.response;

import com.skilize.inventory.domain.Inventory;

/**
 * 棚卸詳細レスポンス。GET /api/inventories/{id} のレスポンスに使用する。
 * サマリより多くのタイムスタンプ情報を含む。TL/ADMIN も他ユーザーの棚卸を参照可能。
 *
 * @param id                      棚卸の内部 PK
 * @param userId                  棚卸オーナーのユーザー内部 PK
 * @param fiscalYear              対象年度（ID と名称）
 * @param status                  棚卸ステータス
 * @param submittedAt             提出日時（未提出の場合は null）
 * @param goalReviewCompletedAt   目標振り返り完了日時（未完了の場合は null）
 * @param goalCompletedAt         目標設定完了日時（未完了の場合は null）
 * @param createdAt               棚卸の作成日時
 * @param updatedAt               棚卸の最終更新日時
 */
public record InventoryDetailResponse(int id, int userId, FiscalYearRef fiscalYear, String status,
                                      String submittedAt, String goalReviewCompletedAt,
                                      String goalCompletedAt, String createdAt, String updatedAt) {

    /**
     * Inventory エンティティから InventoryDetailResponse を生成する。
     *
     * @param i 棚卸エンティティ
     * @return 棚卸詳細レスポンス
     */
    public static InventoryDetailResponse from(Inventory i) {
        return new InventoryDetailResponse(i.getId(), i.getUser().getId(),
                new FiscalYearRef(i.getFiscalYear().getId(), i.getFiscalYear().getName()),
                i.getStatus().name(),
                i.getSubmittedAt() != null ? i.getSubmittedAt().toString() : null,
                i.getGoalReviewCompletedAt() != null ? i.getGoalReviewCompletedAt().toString() : null,
                i.getGoalCompletedAt() != null ? i.getGoalCompletedAt().toString() : null,
                i.getCreatedAt() != null ? i.getCreatedAt().toString() : null,
                i.getUpdatedAt() != null ? i.getUpdatedAt().toString() : null);
    }
}

/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 棚卸サマリレスポンス。棚卸一覧取得および棚卸作成直後のレスポンスとして使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.presentation.response;

import com.skilize.inventory.domain.Inventory;

/**
 * 棚卸サマリレスポンス。GET /api/inventories/mine の一覧要素および棚卸作成直後のレスポンスに使用する。
 *
 * @param id               棚卸の内部 PK
 * @param fiscalYear       対象年度（ID と名称）
 * @param status           棚卸ステータス（DRAFT / PENDING_GOAL / GOAL_COMPLETED 等）
 * @param submittedAt      提出日時（未提出の場合は null）
 * @param goalCompletedAt  目標設定完了日時（未完了の場合は null）
 */
public record InventorySummaryResponse(int id, FiscalYearRef fiscalYear, String status,
                                       String submittedAt, String goalCompletedAt) {

    /**
     * Inventory エンティティから InventorySummaryResponse を生成する。
     *
     * @param i 棚卸エンティティ
     * @return 棚卸サマリレスポンス
     */
    public static InventorySummaryResponse from(Inventory i) {
        return new InventorySummaryResponse(i.getId(),
                new FiscalYearRef(i.getFiscalYear().getId(), i.getFiscalYear().getName()),
                i.getStatus().name(),
                i.getSubmittedAt() != null ? i.getSubmittedAt().toString() : null,
                i.getGoalCompletedAt() != null ? i.getGoalCompletedAt().toString() : null);
    }
}

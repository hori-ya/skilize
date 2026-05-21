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

    public static InventorySummaryResponse from(Inventory i) {
        return new InventorySummaryResponse(i.getId(),
                new FiscalYearRef(i.getFiscalYear().getId(), i.getFiscalYear().getName()),
                i.getStatus().name(),
                i.getSubmittedAt() != null ? i.getSubmittedAt().toString() : null,
                i.getGoalCompletedAt() != null ? i.getGoalCompletedAt().toString() : null);
    }
}

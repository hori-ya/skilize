package com.skilize.user.presentation.response;

import com.skilize.inventory.domain.Inventory;

/**
 * メンバー棚卸サマリ1件のレスポンス。GET /api/users/{id}/inventories のレスポンスに使用する（TL/ADMIN 向け）。
 * 過去年度を含む棚卸一覧を表示する際に使用する。
 *
 * @param id                 棚卸内部PK
 * @param fiscalYear         年度参照情報
 * @param status             棚卸ステータス
 * @param submittedAt        提出日時（未提出の場合は null、ISO-8601 形式）
 * @param goalCompletedAt    目標設定完了日時（未完了の場合は null、ISO-8601 形式）
 */
public record MemberInventorySummaryResponse(int id, FiscalYearRef fiscalYear, String status,
                                              String submittedAt, String goalCompletedAt) {

    public static MemberInventorySummaryResponse from(Inventory inv) {
        return new MemberInventorySummaryResponse(
                inv.getId(),
                new FiscalYearRef(inv.getFiscalYear().getId(), inv.getFiscalYear().getName()),
                inv.getStatus().name(),
                inv.getSubmittedAt() != null ? inv.getSubmittedAt().toString() : null,
                inv.getGoalCompletedAt() != null ? inv.getGoalCompletedAt().toString() : null
        );
    }
}

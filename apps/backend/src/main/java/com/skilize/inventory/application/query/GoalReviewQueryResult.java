/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 前年度目標振り返りクエリ結果。
 * 前年度の目標一覧と各目標の達成状況・振り返りコメントを保持し、目標振り返り画面のデータソースとして使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.application.query;

import java.util.List;

/**
 * 前年度目標振り返りクエリ結果。GET /api/inventories/{id}/goal-review のレスポンスに使用する。
 * 前年度棚卸が存在しない場合は hasPrevGoals=false、items=[] で返す。
 *
 * @param prevFiscalYear 前年度名（hasPrevGoals が false の場合は null）
 * @param hasPrevGoals   前年度目標の有無
 * @param items          前年度目標の振り返り一覧
 */
public record GoalReviewQueryResult(String prevFiscalYear, boolean hasPrevGoals, List<GoalReviewItem> items) {

    /**
     * 前年度目標1件の振り返りデータ。
     *
     * @param prevGoalId        前年度目標の内部 PK（振り返り更新 PUT で使用する）
     * @param goalCategory      目標カテゴリ（IT_SKILL / QUALIFICATION / AD）
     * @param goalName          目標名（スキル名・資格名・セミナー名・カスタム名のいずれか）
     * @param targetPeriod      目標達成期限（ISO-8601 形式）
     * @param reason            目標設定理由
     * @param achievementStatus 達成状況（null: 未振り返り / ACHIEVED / PARTIAL / NOT_ACHIEVED）
     * @param reviewNote        振り返りコメント
     */
    public record GoalReviewItem(int prevGoalId, String goalCategory, String goalName,
                                 String targetPeriod, String reason,
                                 String achievementStatus, String reviewNote) {}
}

package com.skilize.inventory.presentation.request;

/**
 * 目標振り返り更新1件のリクエスト要素。GoalReviewUpdateRequest のリスト要素として使用する。
 *
 * @param prevGoalId        前年度目標の内部 PK（更新対象を特定する）
 * @param achievementStatus 達成状況（ACHIEVED / PARTIAL / NOT_ACHIEVED。未振り返りの場合は null）
 * @param reviewNote        振り返りコメント（自由記述）
 */
public record GoalReviewUpdateItem(int prevGoalId, String achievementStatus, String reviewNote) {}

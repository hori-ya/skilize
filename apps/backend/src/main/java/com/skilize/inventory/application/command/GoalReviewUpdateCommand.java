package com.skilize.inventory.application.command;

/**
 * 目標振り返り更新コマンド。InventoryService.saveGoalReview() に渡す。
 * 前年度目標1件に対して達成状況とコメントを更新する。
 *
 * @param prevGoalId        前年度目標の内部 PK（更新対象を特定する）
 * @param achievementStatus 達成状況（AchievementStatus enum の文字列: ACHIEVED / PARTIAL / NOT_ACHIEVED。未振り返りの場合は null）
 * @param reviewNote        振り返りコメント（自由記述）
 */
public record GoalReviewUpdateCommand(int prevGoalId, String achievementStatus, String reviewNote) {}

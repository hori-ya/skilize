package com.skilize.inventory.presentation.response;

/**
 * 目標設定完了レスポンス。POST /api/inventories/{id}/goals/complete のレスポンスに使用する。
 * 完了条件: ITスキル/資格 ≥1件 かつ AD ≥2件。条件未達の場合は 422 を返す。
 *
 * @param id               棚卸の内部 PK
 * @param status           完了後のステータス（GOAL_COMPLETED）
 * @param goalCompletedAt  目標設定完了日時
 */
public record GoalCompleteResponse(int id, String status, String goalCompletedAt) {}

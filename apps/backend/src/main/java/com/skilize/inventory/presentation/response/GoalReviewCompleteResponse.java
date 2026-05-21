package com.skilize.inventory.presentation.response;

/**
 * 目標振り返り完了レスポンス。POST /api/inventories/{id}/goal-review/complete のレスポンスに使用する。
 * 振り返りは任意フロー（完了しなくても目標設定ステップへ進める）。
 *
 * @param id                    棚卸の内部 PK
 * @param goalReviewCompletedAt 目標振り返り完了日時
 */
public record GoalReviewCompleteResponse(int id, String goalReviewCompletedAt) {}

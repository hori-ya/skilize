package com.skilize.inventory.presentation.request;

import java.util.List;

/**
 * 目標振り返り一括更新リクエスト。PUT /api/inventories/{id}/goal-review のリクエストボディ。
 * 前年度目標ごとに達成状況とコメントを個別に更新する。
 *
 * @param items 振り返り更新データの一覧
 */
public record GoalReviewUpdateRequest(List<GoalReviewUpdateItem> items) {}

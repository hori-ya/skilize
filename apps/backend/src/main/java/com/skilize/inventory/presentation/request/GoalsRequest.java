package com.skilize.inventory.presentation.request;

import java.util.List;

/**
 * 目標一括保存リクエスト。PUT /api/inventories/{id}/goals のリクエストボディ。
 * 全件洗い替え方式のため、送信したリストがそのままDB上の目標として保存される。
 * 目標完了（POST /goals/complete）には ITスキル/資格 ≥1件、AD ≥2件の条件あり。
 *
 * @param items 今年度目標のリスト（空リスト送信で全件削除）
 */
public record GoalsRequest(List<GoalItem> items) {}

package com.skilize.inventory.presentation.response;

import java.util.List;

/**
 * 目標一覧レスポンス。GET /api/inventories/{id}/goals および
 * PUT /api/inventories/{id}/goals のレスポンスに使用する。
 *
 * @param items 今年度目標一覧
 */
public record GoalsResponse(List<GoalResponse> items) {}

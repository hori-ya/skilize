package com.skilize.inventory.presentation.response;

import java.util.List;

/**
 * ITスキル明細一覧レスポンス。GET /api/inventories/{id}/it-skill-details および
 * PUT /api/inventories/{id}/it-skill-details のレスポンスに使用する。
 *
 * @param items ITスキル明細一覧
 */
public record ItSkillDetailsResponse(List<ItSkillDetailResponse> items) {}

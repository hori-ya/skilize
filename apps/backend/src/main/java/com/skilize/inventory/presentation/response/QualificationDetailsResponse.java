package com.skilize.inventory.presentation.response;

import java.util.List;

/**
 * 資格明細一覧レスポンス。GET /api/inventories/{id}/qualification-details および
 * PUT /api/inventories/{id}/qualification-details のレスポンスに使用する。
 *
 * @param items 資格明細一覧
 */
public record QualificationDetailsResponse(List<QualificationDetailResponse> items) {}

package com.skilize.inventory.presentation.response;

import java.util.List;

/**
 * セミナー明細一覧レスポンス。GET /api/inventories/{id}/seminar-details および
 * PUT /api/inventories/{id}/seminar-details のレスポンスに使用する。
 *
 * @param items セミナー明細一覧
 */
public record SeminarDetailsResponse(List<SeminarDetailResponse> items) {}

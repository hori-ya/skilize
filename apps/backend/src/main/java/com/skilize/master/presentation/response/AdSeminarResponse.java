package com.skilize.master.presentation.response;

import com.skilize.master.domain.AdSeminar;

/**
 * ADセミナーマスタ1件のレスポンス。GET /api/ad-seminars などのレスポンスに使用する。
 *
 * @param id           ADセミナー内部PK
 * @param name         セミナー名
 * @param categoryId   所属カテゴリID（未分類の場合は null）
 * @param categoryName 所属カテゴリ名（未分類の場合は null）
 * @param description  説明
 * @param sortOrder    表示順
 * @param isActive     有効フラグ
 */
public record AdSeminarResponse(int id, String name, Integer categoryId, String categoryName,
                                 String description, int sortOrder, boolean isActive) {

    public static AdSeminarResponse from(AdSeminar a) {
        return new AdSeminarResponse(a.getId(), a.getName(),
                a.getCategory() != null ? a.getCategory().getId() : null,
                a.getCategory() != null ? a.getCategory().getName() : null,
                a.getDescription(), a.getSortOrder(), a.isActive());
    }
}

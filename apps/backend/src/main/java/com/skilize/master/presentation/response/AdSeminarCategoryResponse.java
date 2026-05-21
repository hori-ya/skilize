package com.skilize.master.presentation.response;

import com.skilize.master.domain.AdSeminarCategory;

/**
 * ADセミナーカテゴリ1件のレスポンス。GET /api/ad-seminar-categories などのレスポンスに使用する。
 *
 * @param id        カテゴリ内部PK
 * @param name      カテゴリ名
 * @param sortOrder 表示順
 * @param isActive  有効フラグ
 */
public record AdSeminarCategoryResponse(int id, String name, int sortOrder, boolean isActive) {

    public static AdSeminarCategoryResponse from(AdSeminarCategory c) {
        return new AdSeminarCategoryResponse(c.getId(), c.getName(), c.getSortOrder(), c.isActive());
    }
}

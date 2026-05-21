package com.skilize.master.presentation.response;

import com.skilize.master.domain.SeminarCategory;

/**
 * セミナーカテゴリ1件のレスポンス。GET /api/seminar-categories などのレスポンスに使用する。
 *
 * @param id        カテゴリ内部PK
 * @param name      カテゴリ名
 * @param sortOrder 表示順
 * @param isActive  有効フラグ
 */
public record SeminarCategoryResponse(int id, String name, int sortOrder, boolean isActive) {

    public static SeminarCategoryResponse from(SeminarCategory c) {
        return new SeminarCategoryResponse(c.getId(), c.getName(), c.getSortOrder(), c.isActive());
    }
}

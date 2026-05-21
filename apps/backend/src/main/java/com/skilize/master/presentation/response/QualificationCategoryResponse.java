package com.skilize.master.presentation.response;

import com.skilize.master.domain.QualificationCategory;

/**
 * 資格カテゴリ1件のレスポンス。GET /api/qualification-categories などのレスポンスに使用する。
 *
 * @param id        カテゴリ内部PK
 * @param name      カテゴリ名
 * @param sortOrder 表示順
 * @param isActive  有効フラグ
 */
public record QualificationCategoryResponse(int id, String name, int sortOrder, boolean isActive) {

    public static QualificationCategoryResponse from(QualificationCategory c) {
        return new QualificationCategoryResponse(c.getId(), c.getName(), c.getSortOrder(), c.isActive());
    }
}

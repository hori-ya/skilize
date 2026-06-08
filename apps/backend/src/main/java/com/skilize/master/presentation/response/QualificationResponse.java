/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 資格マスタ1件のレスポンス。資格一覧APIのレスポンスに使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.presentation.response;

import com.skilize.master.domain.Qualification;

/**
 * 資格マスタ1件のレスポンス。GET /api/qualifications などのレスポンスに使用する。
 *
 * @param id           資格内部PK
 * @param name         資格名
 * @param categoryId   所属カテゴリID（未分類の場合は null）
 * @param categoryName 所属カテゴリ名（未分類の場合は null）
 * @param description  説明
 * @param sortOrder    表示順
 * @param isActive     有効フラグ
 */
public record QualificationResponse(int id, String name, Integer categoryId, String categoryName,
                                     String description, int sortOrder, boolean isActive) {

    public static QualificationResponse from(Qualification q) {
        return new QualificationResponse(q.getId(), q.getName(),
                q.getCategory() != null ? q.getCategory().getId() : null,
                q.getCategory() != null ? q.getCategory().getName() : null,
                q.getDescription(), q.getSortOrder(), q.isActive());
    }
}

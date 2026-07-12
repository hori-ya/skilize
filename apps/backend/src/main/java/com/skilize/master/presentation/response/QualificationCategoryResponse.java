/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 資格カテゴリ1件のレスポンス。資格カテゴリ一覧APIのレスポンスに使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.presentation.response;

import com.skilize.master.domain.model.QualificationCategory;

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

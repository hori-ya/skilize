/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ADセミナーカテゴリ1件のレスポンス。ADセミナーカテゴリ一覧APIのレスポンスに使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
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

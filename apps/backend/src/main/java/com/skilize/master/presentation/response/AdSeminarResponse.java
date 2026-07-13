/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ADセミナーマスタ1件のレスポンス。ADセミナー一覧APIのレスポンスに使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.presentation.response;

import com.skilize.master.domain.model.AdSeminar;

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
        Integer categoryId = null;
        String categoryName = null;
        if (a.getCategory() != null) {
            categoryId = a.getCategory().getId();
            categoryName = a.getCategory().getName();
        }
        return new AdSeminarResponse(a.getId(), a.getName(),
                categoryId, categoryName,
                a.getDescription(), a.getSortOrder(), a.isActive());
    }
}

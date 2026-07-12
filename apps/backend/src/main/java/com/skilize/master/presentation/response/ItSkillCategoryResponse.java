/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキルカテゴリ1件のレスポンス。ITスキルカテゴリ一覧APIのレスポンスに使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.presentation.response;

import com.skilize.master.domain.model.ItSkillCategory;

/**
 * ITスキルカテゴリ1件のレスポンス。GET /api/it-skill-categories などのレスポンスに使用する。
 *
 * @param id        カテゴリ内部PK
 * @param parentId  親カテゴリID（ルートカテゴリの場合は null）
 * @param level     階層レベル（1=ルート、2=中間、3=末端）
 * @param name      カテゴリ名
 * @param sortOrder 表示順
 * @param isActive  有効フラグ
 */
public record ItSkillCategoryResponse(int id, Integer parentId, short level, String name,
                                       int sortOrder, boolean isActive) {

    public static ItSkillCategoryResponse from(ItSkillCategory c) {
        return new ItSkillCategoryResponse(c.getId(), c.getParentId(), c.getLevel(),
                c.getName(), c.getSortOrder(), c.isActive());
    }
}

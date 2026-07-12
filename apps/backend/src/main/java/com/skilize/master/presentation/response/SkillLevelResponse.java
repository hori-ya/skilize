/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * スキルレベルマスタ1件のレスポンス。スキルレベル一覧APIのレスポンスに使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.presentation.response;

import com.skilize.master.domain.model.SkillLevel;

/**
 * スキルレベル1件のレスポンス。GET /api/skill-levels などのレスポンスに使用する。
 *
 * @param id          スキルレベル内部PK
 * @param levelValue  レベル値（数値が大きいほど上位）
 * @param description レベルの説明
 * @param isActive    有効フラグ
 * @param scoreWeight スコア集計時の重み
 */
public record SkillLevelResponse(int id, short levelValue, String description, boolean isActive, int scoreWeight) {

    public static SkillLevelResponse from(SkillLevel s) {
        return new SkillLevelResponse(s.getId(), s.getLevelValue(), s.getDescription(), s.isActive(), s.getScoreWeight());
    }
}

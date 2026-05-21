package com.skilize.master.presentation.response;

import com.skilize.master.domain.SkillLevel;

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

package com.skilize.inventory.presentation.response;

import com.skilize.inventory.domain.InventoryGoal;

/**
 * 目標1件のレスポンス。goalCategory に応じて itSkillId / qualificationId / adSeminarId の
 * いずれか1つが設定される。カスタム目標の場合は customName が使用される。
 *
 * @param id              目標の内部 PK
 * @param goalCategory    目標カテゴリ（IT_SKILL / QUALIFICATION / AD）
 * @param itSkillId       ITスキルマスタの ID（goalCategory が IT_SKILL の場合）
 * @param itSkillName     スキル名（goalCategory が IT_SKILL の場合）
 * @param qualificationId 資格マスタの ID（goalCategory が QUALIFICATION の場合）
 * @param qualificationName 資格名（goalCategory が QUALIFICATION の場合）
 * @param adSeminarId     ADセミナーマスタの ID（goalCategory が AD の場合）
 * @param adSeminarName   ADセミナー名（goalCategory が AD の場合）
 * @param customName      カスタム目標名（マスタ未登録の場合）
 * @param targetPeriod    目標達成期限（ISO-8601 形式）
 * @param reason          目標設定理由
 */
public record GoalResponse(int id, String goalCategory,
                           Integer itSkillId, String itSkillName,
                           Integer qualificationId, String qualificationName,
                           Integer adSeminarId, String adSeminarName,
                           String customName, String targetPeriod, String reason) {

    public static GoalResponse from(InventoryGoal g) {
        return new GoalResponse(g.getId(), g.getGoalCategory().name(),
                g.getItSkill() != null ? g.getItSkill().getId() : null,
                g.getItSkill() != null ? g.getItSkill().getName() : null,
                g.getQualification() != null ? g.getQualification().getId() : null,
                g.getQualification() != null ? g.getQualification().getName() : null,
                g.getAdSeminar() != null ? g.getAdSeminar().getId() : null,
                g.getAdSeminar() != null ? g.getAdSeminar().getName() : null,
                g.getCustomName(),
                g.getTargetPeriod().toString(), g.getReason());
    }
}

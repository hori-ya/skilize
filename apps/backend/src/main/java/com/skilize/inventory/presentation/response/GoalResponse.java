/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 目標1件のレスポンス。目標一覧取得・目標保存の各エンドポイントのレスポンス要素として使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.presentation.response;

import com.skilize.inventory.domain.model.InventoryGoal;

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

    /**
     * InventoryGoal エンティティから GoalResponse を生成する。
     *
     * @param g 目標エンティティ
     * @return 目標レスポンス
     */
    public static GoalResponse from(InventoryGoal g) {
        Integer itSkillId = null;
        String itSkillName = null;
        if (g.getItSkill() != null) {
            itSkillId = g.getItSkill().getId();
            itSkillName = g.getItSkill().getName();
        }
        Integer qualificationId = null;
        String qualificationName = null;
        if (g.getQualification() != null) {
            qualificationId = g.getQualification().getId();
            qualificationName = g.getQualification().getName();
        }
        Integer adSeminarId = null;
        String adSeminarName = null;
        if (g.getAdSeminar() != null) {
            adSeminarId = g.getAdSeminar().getId();
            adSeminarName = g.getAdSeminar().getName();
        }
        return new GoalResponse(g.getId(), g.getGoalCategory().name(),
                itSkillId, itSkillName,
                qualificationId, qualificationName,
                adSeminarId, adSeminarName,
                g.getCustomName(),
                g.getTargetPeriod().toString(), g.getReason());
    }
}

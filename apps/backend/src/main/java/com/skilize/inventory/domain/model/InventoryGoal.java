/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 目標ドメインモデル。今年度の棚卸に設定する翌年度に向けた目標を表す。
 * 目標カテゴリ（ITスキル / 資格 / AD）に応じてマスタ参照先が異なる。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.InventoryGoalEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.domain.model;

import com.skilize.master.domain.model.AdSeminar;
import com.skilize.master.domain.model.ItSkill;
import com.skilize.master.domain.model.Qualification;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 目標設定。今年度の棚卸に設定する翌年度に向けた目標。JPA/Springに依存しない純粋なドメインモデル。
 * 目標カテゴリに応じて参照するマスタが変わる:
 *   ITスキル目標   → マスタITスキルまたはカスタム目標名
 *   資格目標       → マスタ資格またはカスタム目標名
 *   AD目標         → ADセミナーマスタ（必須。カスタム目標なし）
 *
 * 目標完了の件数条件（InventoryService.completeGoal で検証）:
 *   ITスキル + 資格 の合計 ≥1 件 AND AD ≥2 件
 */
@Getter
@NoArgsConstructor
public class InventoryGoal {

    private Integer id;
    private Integer inventoryId;
    private GoalCategory goalCategory;
    private ItSkill itSkill;
    private Qualification qualification;
    private AdSeminar adSeminar;
    private String customName;
    private LocalDate targetPeriod;
    private String reason;
    private AchievementStatus achievementStatus;
    private String reviewNote;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * 目標を新規作成する。
     */
    public static InventoryGoal create(Inventory inventory, GoalCategory goalCategory,
                                       ItSkill itSkill, Qualification qualification,
                                       AdSeminar adSeminar, String customName,
                                       LocalDate targetPeriod, String reason) {
        InventoryGoal g = new InventoryGoal();
        g.inventoryId = inventory.getId();
        g.goalCategory = goalCategory;
        g.itSkill = itSkill;
        g.qualification = qualification;
        g.adSeminar = adSeminar;
        g.customName = customName;
        g.targetPeriod = targetPeriod;
        g.reason = reason;
        return g;
    }

    /**
     * 永続化済みの状態から目標を復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static InventoryGoal reconstruct(Integer id, Integer inventoryId, GoalCategory goalCategory,
                                            ItSkill itSkill, Qualification qualification, AdSeminar adSeminar,
                                            String customName, LocalDate targetPeriod, String reason,
                                            AchievementStatus achievementStatus, String reviewNote,
                                            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        InventoryGoal g = new InventoryGoal();
        g.id = id;
        g.inventoryId = inventoryId;
        g.goalCategory = goalCategory;
        g.itSkill = itSkill;
        g.qualification = qualification;
        g.adSeminar = adSeminar;
        g.customName = customName;
        g.targetPeriod = targetPeriod;
        g.reason = reason;
        g.achievementStatus = achievementStatus;
        g.reviewNote = reviewNote;
        g.createdAt = createdAt;
        g.updatedAt = updatedAt;
        return g;
    }

    /**
     * 達成状況と振り返りコメントを更新する。翌年度に前年度目標を振り返る際に呼び出す。
     */
    public void updateReview(AchievementStatus achievementStatus, String reviewNote) {
        this.achievementStatus = achievementStatus;
        this.reviewNote = reviewNote;
    }
}

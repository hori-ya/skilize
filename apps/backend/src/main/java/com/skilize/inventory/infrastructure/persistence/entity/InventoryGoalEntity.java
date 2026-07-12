/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 目標JPAエンティティ。inventory_goals テーブルとのマッピングを担う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.InventoryGoal から分離（JPAアノテーションはこちらにのみ残す）
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.entity;

import com.skilize.inventory.domain.model.AchievementStatus;
import com.skilize.inventory.domain.model.GoalCategory;
import com.skilize.master.infrastructure.persistence.entity.AdSeminarEntity;
import com.skilize.master.infrastructure.persistence.entity.ItSkillEntity;
import com.skilize.master.infrastructure.persistence.entity.QualificationEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 目標JPAエンティティ。目標カテゴリに応じて参照するマスタが変わる。 */
@Entity
@Table(name = "inventory_goals")
@Getter
@NoArgsConstructor
public class InventoryGoalEntity {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 棚卸
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private InventoryEntity inventory;

    // 目標カテゴリ（ITスキル / 資格 / AD）
    @Enumerated(EnumType.STRING)
    @Column(name = "goal_category", nullable = false)
    private GoalCategory goalCategory;

    // ITスキル（ITスキル目標のマスタ参照。それ以外は null）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "it_skill_id")
    private ItSkillEntity itSkill;

    // 資格（資格目標のマスタ参照。それ以外は null）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qualification_id")
    private QualificationEntity qualification;

    // ADセミナー（AD目標では必須。それ以外は null）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_seminar_id")
    private AdSeminarEntity adSeminar;

    // カスタム目標名（マスタ未登録のITスキル・資格目標）
    @Column(name = "custom_name")
    private String customName;

    // 達成・予定時期（月初日で保存）
    @Column(name = "target_period", nullable = false)
    private LocalDate targetPeriod;

    // 目標理由
    private String reason;

    // 達成状況（振り返り前は null）
    @Enumerated(EnumType.STRING)
    @Column(name = "achievement_status")
    private AchievementStatus achievementStatus;

    // 振り返りコメント
    @Column(name = "review_note")
    private String reviewNote;

    // 作成日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static InventoryGoalEntity create(InventoryEntity inventory, GoalCategory goalCategory,
                                             ItSkillEntity itSkill, QualificationEntity qualification,
                                             AdSeminarEntity adSeminar, String customName,
                                             LocalDate targetPeriod, String reason) {
        InventoryGoalEntity g = new InventoryGoalEntity();
        g.inventory = inventory;
        g.goalCategory = goalCategory;
        g.itSkill = itSkill;
        g.qualification = qualification;
        g.adSeminar = adSeminar;
        g.customName = customName;
        g.targetPeriod = targetPeriod;
        g.reason = reason;
        return g;
    }

    public void updateReview(AchievementStatus achievementStatus, String reviewNote) {
        this.achievementStatus = achievementStatus;
        this.reviewNote = reviewNote;
    }
}

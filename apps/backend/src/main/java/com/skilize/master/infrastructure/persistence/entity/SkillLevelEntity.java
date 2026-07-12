/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * スキルレベルマスタJPAエンティティ。skill_levels テーブルとのマッピングを担う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.SkillLevel から分離（JPAアノテーションはこちらにのみ残す）
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** スキルレベルマスタJPAエンティティ。ITスキル採点に使用する段階評価の定義。 */
@Entity
@Table(name = "skill_levels")
@Getter
@NoArgsConstructor
public class SkillLevelEntity {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // スキルレベル値
    @Column(nullable = false)
    private Short levelValue;

    // レベル説明
    @Column(nullable = false)
    private String description;

    // グラフスコア計算用の重み値（0=スコアに寄与しない）
    @Column(name = "score_weight", nullable = false)
    private int scoreWeight;

    // 有効フラグ
    @Column(name = "is_active", nullable = false)
    private boolean active;

    // 作成日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static SkillLevelEntity create(Short levelValue, String description, int scoreWeight) {
        SkillLevelEntity s = new SkillLevelEntity();
        s.levelValue = levelValue;
        s.description = description;
        s.scoreWeight = scoreWeight;
        s.active = true;
        return s;
    }

    public void update(Short levelValue, String description, boolean active, int scoreWeight) {
        this.levelValue = levelValue;
        this.description = description;
        this.active = active;
        this.scoreWeight = scoreWeight;
    }
}

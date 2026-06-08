/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * スキルレベルマスタエンティティ。ITスキル採点に使用する段階評価（例: 1〜5）を定義する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * スキルレベルマスタ。ITスキル採点に使用する段階評価の定義。
 *
 * 項目（論理名）:
 *   スキルレベル値 - 数値で表す評価段階（例: 1〜5）
 *   レベル説明     - 各段階の説明テキスト
 *   有効フラグ     - false で一覧から除外（論理削除）
 */
@Entity
@Table(name = "skill_levels")
@Getter
@NoArgsConstructor
public class SkillLevel {

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

    /**
     * スキルレベルを新規生成する（有効状態で初期化）。
     *
     * @param levelValue  レベル値（評価段階の数値）
     * @param description レベルの説明テキスト
     * @param scoreWeight スコア集計時の重み（0 はスコアに寄与しない）
     * @return 生成したスキルレベルエンティティ
     */
    public static SkillLevel create(Short levelValue, String description, int scoreWeight) {
        SkillLevel s = new SkillLevel();
        s.levelValue = levelValue;
        s.description = description;
        s.scoreWeight = scoreWeight;
        s.active = true;
        return s;
    }

    /**
     * スキルレベルの各フィールドを更新する。
     *
     * @param levelValue  新しいレベル値
     * @param description 新しい説明テキスト
     * @param active      有効フラグ（false で論理無効化）
     * @param scoreWeight 新しいスコア重み
     */
    public void update(Short levelValue, String description, boolean active, int scoreWeight) {
        this.levelValue = levelValue;
        this.description = description;
        this.active = active;
        this.scoreWeight = scoreWeight;
    }
}

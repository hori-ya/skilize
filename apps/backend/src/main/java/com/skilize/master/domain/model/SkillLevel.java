/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * スキルレベルマスタドメインモデル。ITスキル採点に使用する段階評価（例: 1〜5）を定義する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.SkillLevelEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * スキルレベルマスタ。ITスキル採点に使用する段階評価の定義。JPA/Springに依存しない純粋なドメインモデル。
 *
 * 項目（論理名）:
 *   スキルレベル値 - 数値で表す評価段階（例: 1〜5）
 *   レベル説明     - 各段階の説明テキスト
 *   有効フラグ     - false で一覧から除外（論理削除）
 */
@Getter
@NoArgsConstructor
public class SkillLevel {

    private Integer id;
    private Short levelValue;
    private String description;
    private int scoreWeight;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * スキルレベルを新規生成する（有効状態で初期化）。
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
     * 永続化済みの状態からスキルレベルを復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static SkillLevel reconstruct(Integer id, Short levelValue, String description, int scoreWeight,
                                         boolean active, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        SkillLevel s = new SkillLevel();
        s.id = id;
        s.levelValue = levelValue;
        s.description = description;
        s.scoreWeight = scoreWeight;
        s.active = active;
        s.createdAt = createdAt;
        s.updatedAt = updatedAt;
        return s;
    }

    /**
     * スキルレベルの各フィールドを更新する。
     */
    public void update(Short levelValue, String description, boolean active, int scoreWeight) {
        this.levelValue = levelValue;
        this.description = description;
        this.active = active;
        this.scoreWeight = scoreWeight;
    }
}

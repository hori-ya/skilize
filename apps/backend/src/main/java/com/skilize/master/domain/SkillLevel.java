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

    // 有効フラグ
    @Column(name = "is_active", nullable = false)
    private boolean active;

    // 作成日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static SkillLevel create(Short levelValue, String description) {
        SkillLevel s = new SkillLevel();
        s.levelValue = levelValue;
        s.description = description;
        s.active = true;
        return s;
    }

    public void update(Short levelValue, String description, boolean active) {
        this.levelValue = levelValue;
        this.description = description;
        this.active = active;
    }
}

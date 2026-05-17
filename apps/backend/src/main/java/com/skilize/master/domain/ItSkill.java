package com.skilize.master.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * ITスキルマスタ。棚卸で採点対象となるITスキルの一覧を管理する。
 * 無効化（is_active=false）は論理削除扱い。過去棚卸の明細参照を保持するため物理削除しない。
 * カスタムスキル名からマスタ昇格する場合も create() で新規登録する（TL 操作）。
 *
 * 項目（論理名）:
 *   分類         - 所属するITスキル分類（最大3階層の ItSkillCategory）
 *   スキル名     - スキルの表示名
 *   説明         - スキルの補足説明（任意）
 *   表示順       - 一覧表示時の並び順
 *   有効フラグ   - false で一覧から除外（論理削除）
 */
@Entity
@Table(name = "it_skills")
@Getter
@NoArgsConstructor
public class ItSkill {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ITスキル分類
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ItSkillCategory category;

    // スキル名
    @Column(nullable = false)
    private String name;

    // 説明
    private String description;

    // 表示順
    @Column(nullable = false)
    private Integer sortOrder;

    // 有効フラグ
    @Column(name = "is_active", nullable = false)
    private boolean active;

    // 作成日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static ItSkill create(ItSkillCategory category, String name, String description, int sortOrder) {
        ItSkill s = new ItSkill();
        s.category = category;
        s.name = name;
        s.description = description;
        s.sortOrder = sortOrder;
        s.active = true;
        return s;
    }

    public void update(ItSkillCategory category, String name, String description, int sortOrder, boolean active) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.active = active;
    }
}

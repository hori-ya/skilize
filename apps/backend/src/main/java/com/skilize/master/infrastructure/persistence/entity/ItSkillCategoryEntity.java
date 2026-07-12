/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキル分類マスタJPAエンティティ。it_skill_categories テーブルとのマッピングを担う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.ItSkillCategory から分離（JPAアノテーションはこちらにのみ残す）
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * ITスキル分類マスタJPAエンティティ。ITスキルを階層的に分類する。最大3階層まで対応（level=1〜3）。
 * 親子関係は parentId（自己参照）で管理する。
 */
@Entity
@Table(name = "it_skill_categories")
@Getter
@NoArgsConstructor
public class ItSkillCategoryEntity {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 親分類ID（null はルートカテゴリ）
    @Column(name = "parent_id")
    private Integer parentId;

    // 親分類エンティティ（読み取り専用。parentId と同じ parent_id カラムを参照。JPQL の LEFT JOIN 用）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private ItSkillCategoryEntity parent;

    // 階層レベル（1=大分類 / 2=中分類 / 3=小分類）
    @Column(nullable = false)
    private Short level;

    // 分類名
    @Column(nullable = false)
    private String name;

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

    public static ItSkillCategoryEntity create(Integer parentId, short level, String name, int sortOrder) {
        ItSkillCategoryEntity c = new ItSkillCategoryEntity();
        c.parentId = parentId;
        c.level = level;
        c.name = name;
        c.sortOrder = sortOrder;
        c.active = true;
        return c;
    }

    public void update(String name, int sortOrder, boolean active) {
        this.name = name;
        this.sortOrder = sortOrder;
        this.active = active;
    }
}

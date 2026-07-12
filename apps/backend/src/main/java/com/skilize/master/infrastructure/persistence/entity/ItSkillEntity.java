/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキルマスタJPAエンティティ。it_skills テーブルとのマッピングを担う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.ItSkill から分離（JPAアノテーションはこちらにのみ残す）
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** ITスキルマスタJPAエンティティ。棚卸で採点対象となるITスキルの一覧を管理する。 */
@Entity
@Table(name = "it_skills")
@Getter
@NoArgsConstructor
public class ItSkillEntity {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ITスキル分類
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ItSkillCategoryEntity category;

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

    public static ItSkillEntity create(ItSkillCategoryEntity category, String name, String description, int sortOrder) {
        ItSkillEntity s = new ItSkillEntity();
        s.category = category;
        s.name = name;
        s.description = description;
        s.sortOrder = sortOrder;
        s.active = true;
        return s;
    }

    public void update(ItSkillCategoryEntity category, String name, String description, int sortOrder, boolean active) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.active = active;
    }
}

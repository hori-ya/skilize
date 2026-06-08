/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキル分類マスタエンティティ。最大3階層の自己参照構造でITスキルを分類する。
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
 * ITスキル分類マスタ。ITスキルを階層的に分類する。最大3階層まで対応（level=1〜3）。
 * 親子関係は parentId（自己参照）で管理する。ChartService がレーダーチャート集計時に
 * この階層を再帰的に辿って先祖カテゴリを解決する。
 *
 * 項目（論理名）:
 *   親分類ID     - 親カテゴリの内部ID。null はルートカテゴリ（レベル1）
 *   階層レベル   - 1=大分類 / 2=中分類 / 3=小分類
 *   分類名       - 分類の表示名
 *   表示順       - 一覧表示時の並び順
 *   有効フラグ   - false で一覧から除外（論理削除）
 */
@Entity
@Table(name = "it_skill_categories")
@Getter
@NoArgsConstructor
public class ItSkillCategory {

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
    private ItSkillCategory parent;

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

    /**
     * ITスキル分類を新規生成する（有効状態で初期化）。
     *
     * @param parentId  親分類ID（null の場合はルートカテゴリ）
     * @param level     階層レベル（1=大分類 / 2=中分類 / 3=小分類）
     * @param name      分類名
     * @param sortOrder 一覧表示時の並び順
     * @return 生成したITスキル分類エンティティ
     */
    public static ItSkillCategory create(Integer parentId, short level, String name, int sortOrder) {
        ItSkillCategory c = new ItSkillCategory();
        c.parentId = parentId;
        c.level = level;
        c.name = name;
        c.sortOrder = sortOrder;
        c.active = true;
        return c;
    }

    /**
     * ITスキル分類の名称・表示順・有効フラグを更新する。
     *
     * @param name      新しい分類名
     * @param sortOrder 新しい表示順
     * @param active    有効フラグ（false で論理無効化）
     */
    public void update(String name, int sortOrder, boolean active) {
        this.name = name;
        this.sortOrder = sortOrder;
        this.active = active;
    }
}

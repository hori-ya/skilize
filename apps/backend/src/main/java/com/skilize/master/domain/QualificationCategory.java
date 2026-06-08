/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 資格分類マスタエンティティ。資格を分類するフラットなカテゴリ（階層なし）を管理する。
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
 * 資格分類マスタ。資格を分類するフラットなカテゴリ（階層なし）。
 *
 * 項目（論理名）:
 *   分類名     - 分類の表示名
 *   表示順     - 一覧表示時の並び順
 *   有効フラグ - false で一覧から除外（論理削除）
 */
@Entity
@Table(name = "qualification_categories")
@Getter
@NoArgsConstructor
public class QualificationCategory {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

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
     * 資格分類を新規生成する（有効状態で初期化）。
     *
     * @param name      分類名
     * @param sortOrder 一覧表示時の並び順
     * @return 生成した資格分類エンティティ
     */
    public static QualificationCategory create(String name, int sortOrder) {
        QualificationCategory c = new QualificationCategory();
        c.name = name;
        c.sortOrder = sortOrder;
        c.active = true;
        return c;
    }

    /**
     * 資格分類の名称・表示順・有効フラグを更新する。
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

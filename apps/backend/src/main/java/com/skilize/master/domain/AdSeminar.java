/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ADセミナーマスタエンティティ。会社が推奨するADセミナーの一覧を管理する。
 * 棚卸セミナー明細および目標設定から参照される。
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
 * ADセミナーマスタ。会社が推奨するADセミナーの一覧を管理する。
 * 棚卸セミナー明細（SeminarDetail）・目標設定（InventoryGoal）の両方から参照される。
 *
 * 項目（論理名）:
 *   ADセミナー分類 - 所属するADセミナー分類（AdSeminarCategory）
 *   セミナー名     - セミナーの表示名
 *   説明           - セミナーの補足説明（任意）
 *   表示順         - 一覧表示時の並び順
 *   有効フラグ     - false で一覧から除外（論理削除）
 */
@Entity
@Table(name = "ad_seminars")
@Getter
@NoArgsConstructor
public class AdSeminar {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ADセミナー分類
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private AdSeminarCategory category;

    // セミナー名
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

    /**
     * ADセミナーを新規生成する（有効状態で初期化）。
     *
     * @param category    所属するADセミナー分類（未分類の場合は null）
     * @param name        セミナー名
     * @param description セミナーの補足説明（任意。null 可）
     * @param sortOrder   一覧表示時の並び順
     * @return 生成したADセミナーエンティティ
     */
    public static AdSeminar create(AdSeminarCategory category, String name, String description, int sortOrder) {
        AdSeminar a = new AdSeminar();
        a.category = category;
        a.name = name;
        a.description = description;
        a.sortOrder = sortOrder;
        a.active = true;
        return a;
    }

    /**
     * ADセミナーの各フィールドを更新する。
     *
     * @param category    新しい所属分類（null 可）
     * @param name        新しいセミナー名
     * @param description 新しい説明（null 可）
     * @param sortOrder   新しい表示順
     * @param active      有効フラグ（false で論理無効化）
     */
    public void update(AdSeminarCategory category, String name, String description, int sortOrder, boolean active) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.active = active;
    }
}

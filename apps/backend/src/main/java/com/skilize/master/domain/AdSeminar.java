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

    public static AdSeminar create(AdSeminarCategory category, String name, String description, int sortOrder) {
        AdSeminar a = new AdSeminar();
        a.category = category;
        a.name = name;
        a.description = description;
        a.sortOrder = sortOrder;
        a.active = true;
        return a;
    }

    public void update(AdSeminarCategory category, String name, String description, int sortOrder, boolean active) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.active = active;
    }
}

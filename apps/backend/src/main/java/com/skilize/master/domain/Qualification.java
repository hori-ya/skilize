package com.skilize.master.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 資格マスタ。棚卸で取得状況を記録する資格の一覧を管理する。
 * 無効化（is_active=false）は論理削除扱い。過去棚卸の明細参照を保持するため物理削除しない。
 *
 * 項目（論理名）:
 *   資格分類   - 所属する資格分類（QualificationCategory）
 *   資格名     - 資格の表示名
 *   説明       - 資格の補足説明（任意）
 *   表示順     - 一覧表示時の並び順
 *   有効フラグ - false で一覧から除外（論理削除）
 */
@Entity
@Table(name = "qualifications")
@Getter
@NoArgsConstructor
public class Qualification {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 資格分類
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private QualificationCategory category;

    // 資格名
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

    public static Qualification create(QualificationCategory category, String name, String description, int sortOrder) {
        Qualification q = new Qualification();
        q.category = category;
        q.name = name;
        q.description = description;
        q.sortOrder = sortOrder;
        q.active = true;
        return q;
    }

    public void update(QualificationCategory category, String name, String description, int sortOrder, boolean active) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.active = active;
    }
}

package com.skilize.master.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * セミナー分類マスタ。自由入力セミナー（SeminarDetail.seminarName）を分類するカテゴリ。
 * ADセミナーの分類（AdSeminarCategory）とは別テーブル。
 *
 * 項目（論理名）:
 *   分類名     - 分類の表示名
 *   表示順     - 一覧表示時の並び順
 *   有効フラグ - false で一覧から除外（論理削除）
 */
@Entity
@Table(name = "seminar_categories")
@Getter
public class SeminarCategory {

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
}

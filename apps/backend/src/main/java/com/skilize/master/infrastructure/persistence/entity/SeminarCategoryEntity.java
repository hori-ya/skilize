/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * セミナー分類マスタJPAエンティティ。seminar_categories テーブルとのマッピングを担う。
 * ADセミナー分類（AdSeminarCategoryEntity）とは別テーブル・別系統。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.SeminarCategory から分離（JPAアノテーションはこちらにのみ残す）
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;

/** セミナー分類マスタJPAエンティティ。自由入力セミナー（SeminarDetail.seminarName）を分類するカテゴリ。 */
@Entity
@Table(name = "seminar_categories")
@Getter
public class SeminarCategoryEntity {

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

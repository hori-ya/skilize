/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * セミナー棚卸明細JPAエンティティ。seminar_details テーブルとのマッピングを担う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.SeminarDetail から分離（JPAアノテーションはこちらにのみ残す）
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.entity;

import com.skilize.master.infrastructure.persistence.entity.AdSeminarEntity;
import com.skilize.master.infrastructure.persistence.entity.SeminarCategoryEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** セミナー棚卸明細JPAエンティティ。ADセミナーまたは自由入力セミナー名のいずれか一方が必ず設定される。 */
@Entity
@Table(name = "seminar_details")
@Getter
@NoArgsConstructor
public class SeminarDetailEntity {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 棚卸
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private InventoryEntity inventory;

    // ADセミナー（自由入力セミナーの場合は null）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_seminar_id")
    private AdSeminarEntity adSeminar;

    // セミナー名（adSeminar が null の場合に使用）
    @Column(name = "seminar_name")
    private String seminarName;

    // セミナー分類（adSeminar が null の場合のみ設定）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seminar_category_id")
    private SeminarCategoryEntity seminarCategory;

    // 受講年月（月初日で保存。未受講は null）
    @Column(name = "attended_year_month")
    private LocalDate attendedYearMonth;

    // 備考
    private String remarks;

    // 作成日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static SeminarDetailEntity create(InventoryEntity inventory, AdSeminarEntity adSeminar, String seminarName,
                                             SeminarCategoryEntity seminarCategory,
                                             LocalDate attendedYearMonth, String remarks) {
        SeminarDetailEntity d = new SeminarDetailEntity();
        d.inventory = inventory;
        d.adSeminar = adSeminar;
        d.seminarName = seminarName;
        d.seminarCategory = seminarCategory;
        d.attendedYearMonth = attendedYearMonth;
        d.remarks = remarks;
        return d;
    }
}

/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 資格棚卸明細JPAエンティティ。qualification_details テーブルとのマッピングを担う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.QualificationDetail から分離（JPAアノテーションはこちらにのみ残す）
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.entity;

import com.skilize.master.infrastructure.persistence.entity.QualificationEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 資格棚卸明細JPAエンティティ。マスタ資格とカスタム資格名のいずれか一方が必ず設定される。 */
@Entity
@Table(name = "qualification_details")
@Getter
@NoArgsConstructor
public class QualificationDetailEntity {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 棚卸
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private InventoryEntity inventory;

    // 資格（カスタム資格の場合は null）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qualification_id")
    private QualificationEntity qualification;

    // カスタム資格名（qualification が null の場合に使用）
    @Column(name = "custom_qualification_name")
    private String customQualificationName;

    // 取得年月（月初日で保存。未取得は null）
    @Column(name = "acquired_year_month")
    private LocalDate acquiredYearMonth;

    // 備考
    private String remarks;

    // 作成日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static QualificationDetailEntity create(InventoryEntity inventory, QualificationEntity qualification,
                                                    String customQualificationName,
                                                    LocalDate acquiredYearMonth, String remarks) {
        QualificationDetailEntity d = new QualificationDetailEntity();
        d.inventory = inventory;
        d.qualification = qualification;
        d.customQualificationName = customQualificationName;
        d.acquiredYearMonth = acquiredYearMonth;
        d.remarks = remarks;
        return d;
    }
}

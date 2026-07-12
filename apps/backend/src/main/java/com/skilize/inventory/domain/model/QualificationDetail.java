/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 資格棚卸明細ドメインモデル。1棚卸あたり複数行の資格取得状況を管理する。
 * マスタ資格とカスタム資格名のいずれか一方が必ず設定される。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.QualificationDetailEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.domain.model;

import com.skilize.master.domain.model.Qualification;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 資格棚卸明細。1棚卸あたり複数行の資格取得状況を管理する。JPA/Springに依存しない純粋なドメインモデル。
 * マスタ資格またはカスタム資格名のいずれか一方が必ず設定される（両方 null は不可）。
 */
@Getter
@NoArgsConstructor
public class QualificationDetail {

    private Integer id;
    private Integer inventoryId;
    private Qualification qualification;
    private String customQualificationName;
    private LocalDate acquiredYearMonth;
    private String remarks;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * 資格明細を新規作成する。
     */
    public static QualificationDetail create(Inventory inventory, Qualification qualification,
                                              String customQualificationName,
                                              LocalDate acquiredYearMonth, String remarks) {
        QualificationDetail d = new QualificationDetail();
        d.inventoryId = inventory.getId();
        d.qualification = qualification;
        d.customQualificationName = customQualificationName;
        d.acquiredYearMonth = acquiredYearMonth;
        d.remarks = remarks;
        return d;
    }

    /**
     * 永続化済みの状態から資格明細を復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static QualificationDetail reconstruct(Integer id, Integer inventoryId, Qualification qualification,
                                                  String customQualificationName, LocalDate acquiredYearMonth,
                                                  String remarks, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        QualificationDetail d = new QualificationDetail();
        d.id = id;
        d.inventoryId = inventoryId;
        d.qualification = qualification;
        d.customQualificationName = customQualificationName;
        d.acquiredYearMonth = acquiredYearMonth;
        d.remarks = remarks;
        d.createdAt = createdAt;
        d.updatedAt = updatedAt;
        return d;
    }
}

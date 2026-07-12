/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * セミナー棚卸明細ドメインモデル。1棚卸あたり複数行のセミナー受講履歴を管理する。
 * ADセミナーまたは自由入力セミナー名のいずれか一方が必ず設定される。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.SeminarDetailEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.domain.model;

import com.skilize.master.domain.model.AdSeminar;
import com.skilize.master.domain.model.SeminarCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * セミナー棚卸明細。1棚卸あたり複数行のセミナー受講履歴を管理する。JPA/Springに依存しない純粋なドメインモデル。
 * ADセミナーまたは自由入力セミナー名のいずれか一方が必ず設定される（両方 null は不可）。
 * セミナー分類は自由入力セミナー時のみ設定する。ADセミナー時は null。
 */
@Getter
@NoArgsConstructor
public class SeminarDetail {

    private Integer id;
    private Integer inventoryId;
    private AdSeminar adSeminar;
    private String seminarName;
    private SeminarCategory seminarCategory;
    private LocalDate attendedYearMonth;
    private String remarks;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * セミナー明細を新規作成する。
     */
    public static SeminarDetail create(Inventory inventory, AdSeminar adSeminar, String seminarName,
                                       SeminarCategory seminarCategory,
                                       LocalDate attendedYearMonth, String remarks) {
        SeminarDetail d = new SeminarDetail();
        d.inventoryId = inventory.getId();
        d.adSeminar = adSeminar;
        d.seminarName = seminarName;
        d.seminarCategory = seminarCategory;
        d.attendedYearMonth = attendedYearMonth;
        d.remarks = remarks;
        return d;
    }

    /**
     * 永続化済みの状態からセミナー明細を復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static SeminarDetail reconstruct(Integer id, Integer inventoryId, AdSeminar adSeminar, String seminarName,
                                            SeminarCategory seminarCategory, LocalDate attendedYearMonth, String remarks,
                                            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        SeminarDetail d = new SeminarDetail();
        d.id = id;
        d.inventoryId = inventoryId;
        d.adSeminar = adSeminar;
        d.seminarName = seminarName;
        d.seminarCategory = seminarCategory;
        d.attendedYearMonth = attendedYearMonth;
        d.remarks = remarks;
        d.createdAt = createdAt;
        d.updatedAt = updatedAt;
        return d;
    }
}

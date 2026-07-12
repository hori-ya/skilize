/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 年度設定JPAエンティティ（シングルトン、常に1件のみ存在）。fiscal_year_settings テーブルとのマッピングを担う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.FiscalYearSettings から分離（JPAアノテーションはこちらにのみ残す）
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** 年度設定JPAエンティティ。システム全体の年度計算に使用するシングルトン設定（常に1件のみ）。 */
@Entity
@Table(name = "fiscal_year_settings")
@Getter
@NoArgsConstructor
public class FiscalYearSettingsEntity {

    // PK（シングルトン、常に1件）
    @Id
    private Short id;

    // 年度開始月
    @Column(nullable = false)
    private Short fiscalYearStartMonth;

    // 更新日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    /**
     * 年度開始月を更新する。
     */
    public void setFiscalYearStartMonth(Short month) {
        this.fiscalYearStartMonth = month;
    }
}

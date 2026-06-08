/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 年度設定マスタエンティティ（シングルトン、常に1件のみ存在）。
 * システム全体の年度計算に使用する年度開始月を保持する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 年度設定マスタ。システム全体の年度計算に使用するシングルトン設定（常に1件のみ）。
 *
 * 項目（論理名）:
 *   年度開始月 - 年度の開始月（例: 4 = 4月始まり）
 *   更新日時   - 設定の最終更新日時
 */
@Entity
@Table(name = "fiscal_year_settings")
@Getter
@NoArgsConstructor
public class FiscalYearSettings {

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
     *
     * @param month 年度開始月（1〜12）
     */
    public void setFiscalYearStartMonth(Short month) {
        this.fiscalYearStartMonth = month;
    }
}

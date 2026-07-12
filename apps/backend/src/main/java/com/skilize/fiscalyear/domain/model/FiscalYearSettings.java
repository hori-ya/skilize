/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 年度設定ドメインモデル（シングルトン、常に1件のみ存在）。
 * システム全体の年度計算に使用する年度開始月を保持する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.FiscalYearSettingsEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 年度設定。システム全体の年度計算に使用するシングルトン設定（常に1件のみ）。
 * JPA/Springに依存しない純粋なドメインモデル。
 *
 * 項目（論理名）:
 *   年度開始月 - 年度の開始月（例: 4 = 4月始まり）
 *   更新日時   - 設定の最終更新日時
 */
@Getter
@NoArgsConstructor
public class FiscalYearSettings {

    private Short id;
    private Short fiscalYearStartMonth;
    private OffsetDateTime updatedAt;

    /**
     * 永続化済みの状態から年度設定を復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static FiscalYearSettings reconstruct(Short id, Short fiscalYearStartMonth, OffsetDateTime updatedAt) {
        FiscalYearSettings s = new FiscalYearSettings();
        s.id = id;
        s.fiscalYearStartMonth = fiscalYearStartMonth;
        s.updatedAt = updatedAt;
        return s;
    }

    /**
     * 年度開始月を更新する。
     *
     * @param month 年度開始月（1〜12）
     */
    public void setFiscalYearStartMonth(Short month) {
        this.fiscalYearStartMonth = month;
    }
}

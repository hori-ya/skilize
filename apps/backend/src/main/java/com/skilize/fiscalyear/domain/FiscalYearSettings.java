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

    public void setFiscalYearStartMonth(Short month) {
        this.fiscalYearStartMonth = month;
    }
}

package com.skilize.fiscalyear.presentation.response;

import com.skilize.fiscalyear.domain.FiscalYear;

/**
 * 年度1件のレスポンス。GET /api/fiscal-years などのレスポンスに使用する。
 *
 * @param id             年度内部PK
 * @param name           年度名（例: "2024年度"）
 * @param startDate      年度開始日（ISO-8601 形式 "yyyy-MM-dd"）
 * @param endDate        年度終了日（ISO-8601 形式 "yyyy-MM-dd"）
 * @param inputStartDate 棚卸入力受付開始日（未設定の場合は null）
 * @param inputEndDate   棚卸入力受付終了日（未設定の場合は null）
 * @param isActive       有効フラグ
 */
public record FiscalYearResponse(int id, String name, String startDate, String endDate,
                                  String inputStartDate, String inputEndDate, boolean isActive) {

    public static FiscalYearResponse from(FiscalYear f) {
        return new FiscalYearResponse(
                f.getId(), f.getName(),
                f.getStartDate().toString(),
                f.getEndDate().toString(),
                f.getInputStartDate() != null ? f.getInputStartDate().toString() : null,
                f.getInputEndDate() != null ? f.getInputEndDate().toString() : null,
                f.isActive());
    }
}

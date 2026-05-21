package com.skilize.fiscalyear.presentation.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 年度設定更新リクエスト。PUT /api/fiscal-year-settings のリクエストボディ（ADMIN のみ実行可）。
 *
 * @param fiscalYearStartMonth 年度開始月（1〜12）
 */
public record FiscalYearSettingsRequest(
        @NotNull @Min(1) @Max(12) Short fiscalYearStartMonth
) {}

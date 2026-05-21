package com.skilize.fiscalyear.presentation.response;

/**
 * 年度設定のレスポンス。GET /api/fiscal-year-settings のレスポンスに使用する。
 *
 * @param fiscalYearStartMonth 年度開始月（1〜12）
 */
public record FiscalYearSettingsResponse(short fiscalYearStartMonth) {}

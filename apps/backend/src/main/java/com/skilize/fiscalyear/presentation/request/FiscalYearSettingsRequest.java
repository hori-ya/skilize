/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 年度設定更新APIのリクエストボディ（ADMIN 専用）。
 * 年度開始月（1〜12）のみを受け付ける。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
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

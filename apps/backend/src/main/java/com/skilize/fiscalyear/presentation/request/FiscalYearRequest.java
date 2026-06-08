/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 年度登録・更新APIのリクエストボディ（ADMIN 専用）。
 * 日付文字列は ISO-8601 形式（"yyyy-MM-dd"）で受け取り、サーバー側で LocalDate に変換する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.presentation.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 年度登録・更新リクエスト。POST /api/fiscal-years および PUT /api/fiscal-years/{id} のリクエストボディ（ADMIN のみ実行可）。
 *
 * @param name           年度名（必須。例: "2024年度"）
 * @param startDate      年度開始日（必須。ISO-8601 形式 "yyyy-MM-dd"）
 * @param endDate        年度終了日（必須。ISO-8601 形式 "yyyy-MM-dd"）
 * @param inputStartDate 棚卸入力受付開始日（任意。ISO-8601 形式 "yyyy-MM-dd"）
 * @param inputEndDate   棚卸入力受付終了日（任意。ISO-8601 形式 "yyyy-MM-dd"）
 * @param active         有効フラグ（false で論理無効化）
 */
public record FiscalYearRequest(
        @NotBlank String name,
        @NotBlank String startDate,
        @NotBlank String endDate,
        String inputStartDate,
        String inputEndDate,
        Boolean active
) {}

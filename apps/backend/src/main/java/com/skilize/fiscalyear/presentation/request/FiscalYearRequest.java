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

/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 年度1件情報のレスポンス。FiscalYear エンティティから生成する静的ファクトリメソッドを提供する。
 * 日付は ISO-8601 形式の文字列（"yyyy-MM-dd"）で返す。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.presentation.response;

import com.skilize.fiscalyear.domain.model.FiscalYear;

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

    /**
     * FiscalYear エンティティからレスポンスを生成する。日付は ISO-8601 形式の文字列に変換する。
     *
     * @param f 変換元の年度エンティティ
     * @return 年度レスポンス
     */
    public static FiscalYearResponse from(FiscalYear f) {
        String inputStartDate = null;
        if (f.getInputStartDate() != null) {
            inputStartDate = f.getInputStartDate().toString();
        }
        String inputEndDate = null;
        if (f.getInputEndDate() != null) {
            inputEndDate = f.getInputEndDate().toString();
        }
        return new FiscalYearResponse(
                f.getId(), f.getName(),
                f.getStartDate().toString(),
                f.getEndDate().toString(),
                inputStartDate,
                inputEndDate,
                f.isActive());
    }
}

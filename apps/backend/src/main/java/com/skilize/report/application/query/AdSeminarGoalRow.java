/**************************************************************************************************************
 * 機能ID      ：RPT
 * 機能名      ：帳票・レポート
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ADセミナー目標行データクラス。棚卸PDF帳票（JasperReports）のADセミナー目標セクションに
 * 渡すデータ1行分を保持する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.report.application.query;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ADセミナー目標行データ。棚卸PDF帳票のADセミナー目標セクション1行分のデータを保持する。
 * JRBeanCollectionDataSource に渡すために JasperReports の Bean として使用する。
 */
@Getter
@AllArgsConstructor
public class AdSeminarGoalRow {
    /** セミナーカテゴリ名 */
    private final String category;
    /** セミナー名称 */
    private final String seminarName;
    /** 受講予定月（1〜12、未設定の場合は null） */
    private final Integer scheduledMonth;
    /** 備考・理由 */
    private final String remark;
}

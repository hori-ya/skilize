/**************************************************************************************************************
 * 機能ID      ：RPT
 * 機能名      ：帳票・レポート
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ADセミナー実績行データクラス。棚卸PDF帳票（JasperReports）のADセミナー実績セクションに
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
 * ADセミナー実績行データ。棚卸PDF帳票のADセミナー実績セクション1行分のデータを保持する。
 * JRBeanCollectionDataSource に渡すために JasperReports の Bean として使用する。
 */
@Getter
@AllArgsConstructor
public class AdSeminarActualRow {
    /** セミナーカテゴリ名 */
    private final String category;
    /** セミナー名称 */
    private final String seminarName;
    /** 実際の受講月（1〜12、未設定の場合は null） */
    private final Integer attendedMonth;
    /** 備考 */
    private final String remark;
}

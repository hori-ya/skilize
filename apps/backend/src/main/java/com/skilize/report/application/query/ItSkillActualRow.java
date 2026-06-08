/**************************************************************************************************************
 * 機能ID      ：RPT
 * 機能名      ：帳票・レポート
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキル実績行データクラス。棚卸PDF帳票（JasperReports）のITスキル実績セクションに
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
 * ITスキル実績行データ。棚卸PDF帳票のITスキル実績セクション1行分のデータを保持する。
 * JRBeanCollectionDataSource に渡すために JasperReports の Bean として使用する。
 */
@Getter
@AllArgsConstructor
public class ItSkillActualRow {
    /** 第1階層カテゴリ名（大分類） */
    private final String category1;
    /** 第2階層カテゴリ名（中分類、存在しない場合は空文字） */
    private final String category2;
    /** スキル名（カスタムスキルの場合はカスタム名） */
    private final String skillName;
    /** スキルレベル値（未採点の場合は null） */
    private final Integer level;
    /** 備考 */
    private final String remark;
}

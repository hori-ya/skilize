/**************************************************************************************************************
 * 機能ID      ：CHT
 * 機能名      ：グラフ・チャート
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * スキル成長推移チャートのクエリ結果クラス。年度別・カテゴリ別のスキルスコア合計推移を保持し、
 * GET /api/charts/growth のレスポンスとして使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.charts.application.query;

import java.util.List;

/**
 * スキル成長推移チャートのクエリ結果。GET /api/charts/growth のレスポンスに使用する。
 * 年度別・カテゴリ別のスキルスコア合計推移を折れ線グラフ形式で提供する。
 *
 * @param fiscalYears 年度名一覧（X軸ラベル、古い年度から昇順）
 * @param series      カテゴリ別の年度スコア系列一覧
 */
public record GrowthQueryResult(List<String> fiscalYears, List<GrowthSeries> series) {

    /**
     * 1カテゴリの年度別スコア推移。
     *
     * @param category1Id        第1階層カテゴリID
     * @param category1Name      第1階層カテゴリ名
     * @param yearlyTotalScores  年度ごとのスキルスコア合計（fiscalYears と同インデックス対応）
     */
    public record GrowthSeries(int category1Id, String category1Name,
                                List<Integer> yearlyTotalScores) {}
}

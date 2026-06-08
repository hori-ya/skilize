/**************************************************************************************************************
 * 機能ID      ：CHT
 * 機能名      ：グラフ・チャート
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * スキルヒートマップチャートのクエリ結果クラス。第1〜第2階層カテゴリ別のスキルレベル分布を保持し、
 * GET /api/charts/heatmap のレスポンスとして使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.charts.application.query;

import java.util.List;

/**
 * スキルヒートマップチャートのクエリ結果。GET /api/charts/heatmap のレスポンスに使用する。
 * 第1〜第2階層カテゴリ別のスキルレベル分布をヒートマップで表示するためのデータを提供する。
 *
 * @param currentFiscalYear   当年度名
 * @param hasCurrentYearData  当年度の棚卸データが存在するか
 * @param maxLevelValue       スキルレベル最大値（ヒートマップのスケール基準）
 * @param rows                第1階層カテゴリ別の行データ一覧
 */
public record HeatmapQueryResult(String currentFiscalYear, boolean hasCurrentYearData,
                                  int maxLevelValue, List<HeatmapRow> rows) {

    /**
     * ヒートマップの1行（第1階層カテゴリ単位）。
     *
     * @param category1Id    第1階層カテゴリID
     * @param category1Name  第1階層カテゴリ名
     * @param cells          第2階層カテゴリ別のセルデータ一覧
     */
    public record HeatmapRow(int category1Id, String category1Name, List<HeatmapCell> cells) {}

    /**
     * ヒートマップの1セル（第2階層カテゴリ単位）。
     *
     * @param category2Id      第2階層カテゴリID（存在しない場合は null）
     * @param category2Name    第2階層カテゴリ名（存在しない場合は null）
     * @param avgLevelValue    スキルレベル平均値（スコアありスキルが0件の場合は null）
     * @param scoredSkillCount スコアが設定されているスキル件数
     * @param skills           セル内のスキル明細一覧
     */
    public record HeatmapCell(Integer category2Id, String category2Name,
                               Double avgLevelValue, int scoredSkillCount,
                               List<HeatmapSkill> skills) {}

    /**
     * ヒートマップセル内のスキル明細。
     *
     * @param skillName  スキル名
     * @param levelValue スキルレベル値（未登録の場合は null）
     */
    public record HeatmapSkill(String skillName, Integer levelValue) {}
}

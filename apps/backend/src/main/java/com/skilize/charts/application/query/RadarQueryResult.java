package com.skilize.charts.application.query;

import java.util.List;

/**
 * スキルバランスレーダーチャートのクエリ結果。GET /api/charts/radar のレスポンスに使用する。
 * 当年度と前年度のカテゴリ別平均スコアを比較表示するためのデータを提供する。
 *
 * @param currentFiscalYear    当年度名
 * @param prevFiscalYear       前年度名（前年度データが存在しない場合は null）
 * @param hasCurrentYearData   当年度の棚卸データが存在するか
 * @param maxScoreWeight       スコア重みの最大値（チャートのスケール基準）
 * @param axes                 レーダー軸一覧（第1階層カテゴリ単位）
 */
public record RadarQueryResult(String currentFiscalYear, String prevFiscalYear,
                                boolean hasCurrentYearData, int maxScoreWeight,
                                List<RadarAxis> axes) {

    /**
     * レーダー1軸分のデータ。
     *
     * @param category1Id      第1階層カテゴリID
     * @param category1Name    第1階層カテゴリ名
     * @param currentAvgScore  当年度の平均スコア
     * @param prevAvgScore     前年度の平均スコア（前年度データなしの場合は null）
     */
    public record RadarAxis(int category1Id, String category1Name,
                             double currentAvgScore, Double prevAvgScore) {}
}

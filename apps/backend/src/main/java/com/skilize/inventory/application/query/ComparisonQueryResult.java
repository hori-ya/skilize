package com.skilize.inventory.application.query;

import java.util.List;

/**
 * 前年度との ITスキルレベル比較クエリ結果。GET /api/inventories/{id}/comparison のレスポンスに使用する。
 * 前年度棚卸が存在しない場合は hasPrevYear=false、items=[] で返す。
 *
 * @param inventoryId       現在の棚卸の内部 PK
 * @param currentFiscalYear 現在の年度名
 * @param prevFiscalYear    前年度名（hasPrevYear が false の場合は null）
 * @param hasPrevYear       前年度棚卸の有無
 * @param items             ITスキルごとの比較データ一覧（カスタムスキルは比較対象外）
 */
public record ComparisonQueryResult(int inventoryId, String currentFiscalYear, String prevFiscalYear,
                                    boolean hasPrevYear, List<ComparisonItem> items) {

    /**
     * ITスキル1件分の前年度比較データ。
     *
     * @param itSkillId       ITスキルマスタの ID
     * @param skillName       スキル名
     * @param currentDetailId 現在の棚卸明細の内部 PK
     * @param currentLevelValue 現在のスキルレベル値
     * @param currentRemarks  現在の備考
     * @param prevLevelValue  前年度のスキルレベル値（前年度に当該スキルがない場合は null）
     * @param diff            レベル差分（currentLevelValue - prevLevelValue。前年度データなしの場合は null）
     */
    public record ComparisonItem(Integer itSkillId, String skillName, int currentDetailId,
                                 int currentLevelValue, String currentRemarks,
                                 Integer prevLevelValue, Integer diff) {}
}

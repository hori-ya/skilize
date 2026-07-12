/**************************************************************************************************************
 * 機能ID      ：DSH
 * 機能名      ：ダッシュボード
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ダッシュボードサービスの参照結果。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（DashboardControllerのRepository直接参照をDashboardServiceへ移行）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.dashboard.application.query;

import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.inventory.domain.model.Inventory;

/**
 * ダッシュボード情報の参照結果。
 *
 * fiscalYear: 現在有効な年度（存在しない場合は null）
 * inventory: ログインユーザーの今年度棚卸（未作成の場合は null）
 * itSkillCount / qualificationCount / seminarCount: 棚卸が存在する場合の各明細件数
 */
public record DashboardQueryResult(FiscalYear fiscalYear, Inventory inventory,
                                    int itSkillCount, int qualificationCount, int seminarCount) {
}

/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 年度リポジトリインターフェース。永続化の実装詳細を持たない。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のFiscalYearRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.domain.repository;

import com.skilize.fiscalyear.domain.model.FiscalYear;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 年度リポジトリ。実装は infrastructure.persistence.repository.FiscalYearRepositoryImpl。 */
public interface FiscalYearRepository {

    /** 全年度を返す。 */
    List<FiscalYear> findAll();

    /** IDで年度を取得する。 */
    Optional<FiscalYear> findById(Integer id);

    /** 年度を保存する（新規作成・更新の両方に使用）。 */
    FiscalYear save(FiscalYear fiscalYear);

    /**
     * 今日の日付が有効期間（startDate〜endDate）に含まれる年度を返す。
     * BETWEEN は両端を含む（startDate ≤ today ≤ endDate）。有効フラグ（active=true）も確認する。
     */
    Optional<FiscalYear> findCurrent(LocalDate today);
}

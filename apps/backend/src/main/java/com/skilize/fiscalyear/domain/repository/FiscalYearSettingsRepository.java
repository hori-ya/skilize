/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 年度設定リポジトリインターフェース。永続化の実装詳細を持たない。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のFiscalYearSettingsRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.domain.repository;

import com.skilize.fiscalyear.domain.model.FiscalYearSettings;

import java.util.Optional;

/**
 * 年度設定リポジトリ。シングルトン（id=1 のレコード1件のみ）のため、カスタムクエリは不要。
 * 実装は infrastructure.persistence.repository.FiscalYearSettingsRepositoryImpl。
 */
public interface FiscalYearSettingsRepository {

    /** IDで年度設定を取得する。シングルトンのため findById((short) 1) で取得する。 */
    Optional<FiscalYearSettings> findById(Short id);

    /** 年度設定を保存する。 */
    FiscalYearSettings save(FiscalYearSettings settings);
}

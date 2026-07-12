/**************************************************************************************************************
 * 機能ID      ：FY
 * 機能名      ：年度管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 年度エンティティの永続化を担う Spring Data JPA リポジトリ。
 * 今日の日付から現在有効な年度を取得するカスタムクエリを提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.FiscalYearRepository から分離（Spring Data JPA実装はこちらに残す）
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.fiscalyear.infrastructure.persistence.repository;

import com.skilize.fiscalyear.infrastructure.persistence.entity.FiscalYearEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 年度 Spring Data JPA リポジトリ。他featureが直接JPA関連（@ManyToOne等）で年度エンティティを
 * 参照する必要がある場合（未移行feature側）は、このインターフェースを直接injectしてよい。
 */
public interface FiscalYearJpaRepository extends JpaRepository<FiscalYearEntity, Integer> {

    /**
     * 今日の日付が有効期間（startDate〜endDate）に含まれる年度を返す。
     * BETWEEN は両端を含む（startDate ≤ today ≤ endDate）。有効フラグ（active=true）も確認する。
     */
    @Query("SELECT f FROM FiscalYearEntity f WHERE f.active = true AND :today BETWEEN f.startDate AND f.endDate ORDER BY f.startDate DESC")
    Optional<FiscalYearEntity> findCurrent(@Param("today") LocalDate today);
}

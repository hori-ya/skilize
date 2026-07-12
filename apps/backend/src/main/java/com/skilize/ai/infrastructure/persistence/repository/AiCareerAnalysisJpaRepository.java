/**************************************************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * AI キャリア分析エンティティの永続化を担う Spring Data JPA リポジトリ。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.AiCareerAnalysisRepository から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.ai.infrastructure.persistence.repository;

import com.skilize.ai.infrastructure.persistence.entity.AiCareerAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** AI キャリア分析 Spring Data JPA リポジトリ。ユーザー × 年度の組み合わせで1件のレコードを管理する。 */
public interface AiCareerAnalysisJpaRepository extends JpaRepository<AiCareerAnalysisEntity, Integer> {

    /** 指定ユーザーの全分析結果を年度降順（新しい順）で返す。 */
    List<AiCareerAnalysisEntity> findByUserIdOrderByFiscalYearIdDesc(int userId);

    /** 指定ユーザー・指定年度の分析結果を返す。分析トリガー時の既存レコード確認に使用。 */
    Optional<AiCareerAnalysisEntity> findByUserIdAndFiscalYearId(int userId, int fiscalYearId);
}

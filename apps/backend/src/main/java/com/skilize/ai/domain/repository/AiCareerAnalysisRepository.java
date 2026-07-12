/**************************************************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * AI キャリア分析リポジトリインターフェース。永続化の実装詳細を持たない。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のAiCareerAnalysisRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.ai.domain.repository;

import com.skilize.ai.domain.model.AiCareerAnalysis;

import java.util.List;
import java.util.Optional;

/** AI キャリア分析リポジトリ。実装は infrastructure.persistence.repository.AiCareerAnalysisRepositoryImpl。 */
public interface AiCareerAnalysisRepository {

    /** 指定ユーザーの全分析結果を年度降順（新しい順）で返す。 */
    List<AiCareerAnalysis> findByUserIdOrderByFiscalYearIdDesc(int userId);

    /** 指定ユーザー・指定年度の分析結果を返す。分析トリガー時の既存レコード確認に使用。 */
    Optional<AiCareerAnalysis> findByUserIdAndFiscalYearId(int userId, int fiscalYearId);

    /** AIキャリア分析結果を保存する（新規作成・更新の両方に使用）。 */
    AiCareerAnalysis save(AiCareerAnalysis analysis);
}

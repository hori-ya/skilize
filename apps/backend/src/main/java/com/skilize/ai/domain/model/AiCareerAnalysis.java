/**************************************************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * AI キャリア分析結果ドメインモデル。ユーザー×年度で1件の分析レコードを管理し、
 * ステータス遷移（PENDING→PROCESSING→COMPLETED/FAILED）と分析結果JSONを保持する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.AiCareerAnalysisEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.ai.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * AI キャリア分析結果。ユーザー×年度で 1 件。JPA/Springに依存しない純粋なドメインモデル。
 * ステータス遷移: PENDING（待機中）→ PROCESSING（分析中）→ COMPLETED/FAILED。
 * analysisResult は JSON 文字列として保持し、API レスポンス時にパースして返す。
 */
@Getter
@NoArgsConstructor
public class AiCareerAnalysis {

    private Integer id;
    private Integer userId;
    private Integer fiscalYearId;
    private AiAnalysisStatus status;
    private String analysisResult;
    private String errorMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static AiCareerAnalysis createPending(int userId, int fiscalYearId) {
        AiCareerAnalysis a = new AiCareerAnalysis();
        a.userId = userId;
        a.fiscalYearId = fiscalYearId;
        a.status = AiAnalysisStatus.PENDING;
        return a;
    }

    /**
     * 永続化済みの状態からAIキャリア分析結果を復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static AiCareerAnalysis reconstruct(Integer id, Integer userId, Integer fiscalYearId,
                                               AiAnalysisStatus status, String analysisResult,
                                               String errorMessage, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        AiCareerAnalysis a = new AiCareerAnalysis();
        a.id = id;
        a.userId = userId;
        a.fiscalYearId = fiscalYearId;
        a.status = status;
        a.analysisResult = analysisResult;
        a.errorMessage = errorMessage;
        a.createdAt = createdAt;
        a.updatedAt = updatedAt;
        return a;
    }

    public void resetToPending() {
        // 既存レコードを再分析する場合に呼び出す（前回の結果とエラーをクリアする）
        this.status = AiAnalysisStatus.PENDING;
        this.analysisResult = null;
        this.errorMessage = null;
    }
}

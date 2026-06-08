/**************************************************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * AI キャリア分析結果エンティティ。ユーザー×年度で1件の分析レコードを管理し、
 * ステータス遷移（PENDING→PROCESSING→COMPLETED/FAILED）と分析結果JSONを保持する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.ai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * AI キャリア分析結果エンティティ。ユーザー×年度で 1 件。
 * ステータス遷移: PENDING（待機中）→ PROCESSING（分析中）→ COMPLETED/FAILED。
 * analysis_result は PostgreSQL の jsonb 型で保存し、API レスポンス時にパースして返す。
 */
@Entity
@Table(name = "ai_career_analyses")
@Getter
@NoArgsConstructor
public class AiCareerAnalysis {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ユーザー内部ID
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    // 年度内部ID
    @Column(name = "fiscal_year_id", nullable = false)
    private Integer fiscalYearId;

    // 分析ステータス
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiAnalysisStatus status;

    // 分析結果（jsonb）
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_result", columnDefinition = "jsonb")
    private String analysisResult;

    // エラーメッセージ
    @Column(name = "error_message")
    private String errorMessage;

    // 作成日時
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static AiCareerAnalysis createPending(int userId, int fiscalYearId) {
        AiCareerAnalysis a = new AiCareerAnalysis();
        a.userId = userId;
        a.fiscalYearId = fiscalYearId;
        a.status = AiAnalysisStatus.PENDING;
        return a;
    }

    public void resetToPending() {
        // 既存レコードを再分析する場合に呼び出す（前回の結果とエラーをクリアする）
        this.status = AiAnalysisStatus.PENDING;
        this.analysisResult = null;
        this.errorMessage = null;
    }
}

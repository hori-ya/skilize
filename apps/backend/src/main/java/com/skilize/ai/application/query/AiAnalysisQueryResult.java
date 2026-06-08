/**************************************************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * AI キャリア分析結果クエリ結果クラス。分析ステータス・LLM出力JSON・エラーメッセージを保持し、
 * GET /api/users/me/ai-analyses などのレスポンスとして使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.ai.application.query;

import java.time.OffsetDateTime;

/**
 * AI キャリア分析結果クエリ結果。GET /api/users/me/ai-analyses などのレスポンスに使用する。
 * analysisResult は DB に jsonb 型で保存された LLM 出力をそのままオブジェクトとして返す。
 * Object 型を使用することで、フロントエンドが JSON を直接受け取れる（Map や JSONNode にしない設計）。
 * status が COMPLETED のときのみ analysisResult に値が入り、FAILED のときは errorMessage に内容が入る。
 *
 * @param id             AI分析内部PK
 * @param fiscalYearId   分析対象年度ID
 * @param status         分析ステータス（PENDING / RUNNING / COMPLETED / FAILED）
 * @param analysisResult LLM が出力した分析結果JSON（status=COMPLETED のときのみ設定）
 * @param errorMessage   エラーメッセージ（status=FAILED のときのみ設定）
 * @param createdAt      分析リクエスト日時
 * @param updatedAt      最終更新日時
 */
public record AiAnalysisQueryResult(
        int id,
        int fiscalYearId,
        String status,
        Object analysisResult,  // jsonb 型の JSON データを Object として返す（構造はフロントが解釈する）
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

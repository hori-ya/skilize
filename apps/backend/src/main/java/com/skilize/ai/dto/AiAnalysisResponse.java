package com.skilize.ai.dto;

import java.time.OffsetDateTime;

/**
 * AI キャリア分析結果レスポンス。
 * analysisResult は DB に jsonb 型で保存された LLM 出力をそのままオブジェクトとして返す。
 * Object 型を使用することで、フロントエンドが JSON を直接受け取れる（Map や JSONNode にしない設計）。
 * status が COMPLETED のときのみ analysisResult に値が入り、FAILED のときは errorMessage に内容が入る。
 */
public record AiAnalysisResponse(
        int id,
        int fiscalYearId,
        String status,
        Object analysisResult,  // jsonb 型の JSON データを Object として返す（構造はフロントが解釈する）
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

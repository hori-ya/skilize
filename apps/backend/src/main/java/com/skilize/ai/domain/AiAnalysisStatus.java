package com.skilize.ai.domain;

/**
 * AI キャリア分析のステータス遷移。
 * PENDING    = 分析待機中（レコード作成直後）
 * PROCESSING = Python AI サービスが LLM 呼び出し中
 * COMPLETED  = 分析成功（analysisResult に JSON が格納済み）
 * FAILED     = 分析失敗（errorMessage にエラー内容が格納される）
 */
public enum AiAnalysisStatus {
    PENDING, PROCESSING, COMPLETED, FAILED
}

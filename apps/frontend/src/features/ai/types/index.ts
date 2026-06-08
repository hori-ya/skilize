/*******************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * AI機能で使用する型定義。
 * AIチャット（モード・メッセージ・レスポンス）と
 * AIキャリア分析（状態・結果）の型を定義する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
/** AIチャットのモード。NORMAL=通常・PROOFREADING=文章校正・CAREER=キャリア相談・HELP=使い方案内 */
export type AiMode = 'NORMAL' | 'PROOFREADING' | 'CAREER' | 'HELP';

/**
 * チャット履歴のメッセージ1件。
 */
export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

/**
 * AIチャットリクエスト。送信メッセージ・モード・会話履歴を含む。
 */
export interface AiChatRequest {
  message: string;
  mode: AiMode;
  history: ChatMessage[];
}

/**
 * AIチャットレスポンス。AIからの返答テキストとモードを含む。
 */
export interface AiChatResponse {
  response: string;
  mode: AiMode;
}

/** PENDING=待機中 / PROCESSING=分析中 / COMPLETED=完了 / FAILED=失敗 */
export type AiAnalysisStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

/**
 * AI分析結果の詳細。要約・強み・成長領域・期待との適合度・推奨アクションを含む。
 */
export interface AiAnalysisResult {
  summary: string;
  strengths: string[];
  growth_areas: string[];
  expectation_fit: string;
  recommended_actions: string[];
}

/**
 * AIキャリア分析エンティティ。分析状態と結果を保持する。
 */
export interface AiAnalysis {
  id: number;
  fiscalYearId: number;
  status: AiAnalysisStatus;
  analysisResult: AiAnalysisResult | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

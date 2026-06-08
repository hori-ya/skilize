/**************************************************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * AIチャットリクエストクラス。POST /api/ai/chat のリクエスト本文を定義する。
 * チャットメッセージ・モード・会話履歴を受け取り、Python AIサービスに転送する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.ai.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * AIチャットリクエスト。POST /api/ai/chat のリクエスト本文。
 * mode は NORMAL（通常会話）/ PROOFREADING（文章校正）/ CAREER（キャリア相談）/ HELP（使い方案内）の4種類。
 * history は null 許容（初回メッセージの場合は空リスト扱い）。
 *
 * @param message チャットメッセージ本文（最大4000文字、必須）
 * @param mode    チャットモード（NORMAL / PROOFREADING / CAREER / HELP）
 * @param history 会話履歴（null または空リストは初回メッセージ扱い）
 */
public record AiChatRequest(
        @NotBlank @Size(max = 4000) String message,
        @NotNull @Pattern(regexp = "NORMAL|PROOFREADING|CAREER|HELP") String mode,
        List<ChatHistoryItem> history
) {
    /**
     * 会話履歴1件。フロントエンドが保持する past ターンを Python AIサービスに渡すために使用する。
     *
     * @param role    発言者（"user" または "assistant"）
     * @param content メッセージ内容（最大8000文字）
     */
    public record ChatHistoryItem(
            @NotBlank @Pattern(regexp = "user|assistant") String role,
            @NotBlank @Size(max = 8000) String content
    ) {}
}

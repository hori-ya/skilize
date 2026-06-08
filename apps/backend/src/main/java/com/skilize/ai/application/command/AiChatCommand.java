/**************************************************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * AIチャットサービスへの命令オブジェクト。プレゼンテーション層のリクエストをアプリケーション層に
 * 伝達するための中間オブジェクト（Request→Command 変換により依存を分離する）。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.ai.application.command;

import java.util.List;

/**
 * AIチャットコマンド。AiChatService.chat() に渡すアプリケーション層の命令オブジェクト。
 * プレゼンテーション層（AiChatRequest）をアプリケーション層から分離するために使用する。
 *
 * @param message チャットメッセージ本文
 * @param mode    チャットモード（NORMAL / PROOFREADING / CAREER / HELP）
 * @param userId  リクエストユーザーの内部ID（CAREER モード時に Python 側がDB参照するために使用）
 * @param history 会話履歴（空リストは初回メッセージ扱い）
 */
public record AiChatCommand(
        String message,
        String mode,
        int userId,
        List<ChatHistoryItem> history
) {
    /**
     * 会話履歴1件の内部表現。
     *
     * @param role    発言者（"user" または "assistant"）
     * @param content メッセージ内容
     */
    public record ChatHistoryItem(String role, String content) {}
}

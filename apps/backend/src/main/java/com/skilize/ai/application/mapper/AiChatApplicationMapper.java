/**************************************************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * AIチャットのアプリケーション層マッパー。プレゼンテーション層のリクエスト（AiChatRequest）を
 * アプリケーション層のコマンド（AiChatCommand）に変換する責務を担う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.ai.application.mapper;

import com.skilize.ai.application.command.AiChatCommand;
import com.skilize.ai.presentation.request.AiChatRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AIチャットのアプリケーション層マッパー。プレゼンテーション層とアプリケーション層の間の
 * オブジェクト変換（Request → Command）を担う。
 */
@Component
public class AiChatApplicationMapper {

    /**
     * AIチャットリクエストをアプリケーションコマンドに変換する。
     * history が null の場合は空リストに変換する。
     *
     * @param request リクエストオブジェクト（プレゼンテーション層）
     * @param userId  リクエストユーザーの内部ID
     * @return AiChatCommand（アプリケーション層）
     */
    public AiChatCommand toCommand(AiChatRequest request, int userId) {
        List<AiChatCommand.ChatHistoryItem> history = new ArrayList<>();
        if (request.history() != null) {
            for (AiChatRequest.ChatHistoryItem item : request.history()) {
                history.add(new AiChatCommand.ChatHistoryItem(item.role(), item.content()));
            }
        }
        return new AiChatCommand(request.message(), request.mode(), userId, history);
    }
}

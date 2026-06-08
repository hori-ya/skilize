/*******************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * AIチャット API。ユーザーのメッセージとモード・会話履歴をバックエンド経由で
 * AI サービスに送信し、AIからの返答を取得する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import client from '../../../shared/api/client';
import type { AiChatRequest, AiChatResponse } from '../types';

/**
 * AIチャットメッセージ送信。
 *
 * @param req メッセージ・モード・会話履歴を含むリクエスト
 * @returns AI からの返答テキストとモード
 */
export async function postAiChat(req: AiChatRequest): Promise<AiChatResponse> {
  const { data } = await client.post<AiChatResponse>('/ai/chat', req);
  return data;
}

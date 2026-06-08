# ******************************************************************************
# 機能ID      ：AI
# 機能名      ：AI機能
# 作成日      ：2026/06/08
# 作成者      ：hori-ya
# ------------------------------------------------------------------------------
# 機能概要：
# AIチャットエンドポイントのリクエスト/レスポンススキーマ定義。
# 会話履歴を含むチャットリクエストと、LLM 応答を包むレスポンスの Pydantic モデル。
# ------------------------------------------------------------------------------
# 更新履歴：
# 2026/06/08 hori-ya 初版作成
# ------------------------------------------------------------------------------
# Copyright (C) 2026 Skilize Project. All Rights Reserved.
# ******************************************************************************
from pydantic import BaseModel


class ChatMessage(BaseModel):
    """
    会話履歴の1メッセージ。

    role: メッセージの送信者ロール（"user" または "assistant"）
    content: メッセージ本文
    """
    role: str
    content: str


class ChatRequest(BaseModel):
    """
    AIチャットリクエスト。

    message: ユーザーが送信したメッセージ
    mode: チャットモード（NORMAL / PROOFREADING / CAREER / HELP）
    userId: リクエスト元ユーザーの ID
    history: これまでの会話履歴（古い順）
    """
    message: str
    mode: str
    userId: int
    history: list[ChatMessage] = []


class ChatResponse(BaseModel):
    """
    AIチャットレスポンス。

    response: LLM が生成した応答テキスト
    """
    response: str

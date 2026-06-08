# ******************************************************************************
# 機能ID      ：AI
# 機能名      ：AI機能
# 作成日      ：2026/06/08
# 作成者      ：hori-ya
# ------------------------------------------------------------------------------
# 機能概要：
# AIチャットのエンドポイント定義。
# Spring Boot バックエンドからの HTTP プロキシリクエストを受け付け、
# モード別プロンプトで LLM を呼び出し、応答を返す。
# ------------------------------------------------------------------------------
# 更新履歴：
# 2026/06/08 hori-ya 初版作成
# ------------------------------------------------------------------------------
# Copyright (C) 2026 Skilize Project. All Rights Reserved.
# ******************************************************************************
import logging

from fastapi import APIRouter, Depends, HTTPException, status

from app.api.dependencies import verify_internal_key
from app.schemas.chat import ChatRequest, ChatResponse
from app.services.chat_service import process_chat

logger = logging.getLogger(__name__)
router = APIRouter()


@router.post("/chat", dependencies=[Depends(verify_internal_key)])
def chat(req: ChatRequest) -> ChatResponse:
    """
    AIチャットエンドポイント。

    モード（NORMAL / PROOFREADING / CAREER / HELP）に応じたシステムプロンプトで
    LLM を呼び出し、応答テキストを返す。
    LLM 処理中に例外が発生した場合は 500 Internal Server Error を返す。

    Args:
        req: チャットリクエスト（message・mode・userId・history）

    Returns:
        ChatResponse（LLM の応答テキスト）

    Raises:
        HTTPException: LLM 処理失敗時に 500 を送出
    """
    logger.info("Chat request: user=%s mode=%s", req.userId, req.mode)
    try:
        history = [{"role": m.role, "content": m.content} for m in req.history]
        response = process_chat(req.message, req.mode, req.userId, history)
        return ChatResponse(response=response)
    except Exception:
        logger.exception("Chat failed: user=%s mode=%s", req.userId, req.mode)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="AI処理中にエラーが発生しました"
        )

# ******************************************************************************
# 機能ID      ：AI
# 機能名      ：AI機能
# 作成日      ：2026/06/08
# 作成者      ：hori-ya
# ------------------------------------------------------------------------------
# 機能概要：
# FastAPI 依存性注入モジュール。
# X-Internal-Key ヘッダーによる内部サービス認証を提供する。
# Spring Boot バックエンドからの内部通信のみを許可する。
# ------------------------------------------------------------------------------
# 更新履歴：
# 2026/06/08 hori-ya 初版作成
# ------------------------------------------------------------------------------
# Copyright (C) 2026 Skilize Project. All Rights Reserved.
# ******************************************************************************
from fastapi import Header, HTTPException, status

from app.core.config import settings


async def verify_internal_key(x_internal_key: str = Header(default="", alias="X-Internal-Key")):
    """
    内部サービス認証の依存性関数。

    リクエストヘッダー X-Internal-Key と環境変数 AI_SECRET_KEY を照合する。
    AI_SECRET_KEY が未設定の場合は認証をスキップする（ローカル開発環境向け）。
    不一致の場合は 403 Forbidden を返す。

    Args:
        x_internal_key: リクエストヘッダーから取得した内部認証キー

    Raises:
        HTTPException: キーが一致しない場合に 403 を送出
    """
    if settings.ai_secret_key and x_internal_key != settings.ai_secret_key:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden")

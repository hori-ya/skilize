# ******************************************************************************
# 機能ID      ：AI
# 機能名      ：AI機能
# 作成日      ：2026/06/08
# 作成者      ：hori-ya
# ------------------------------------------------------------------------------
# 機能概要：
# FastAPI アプリケーションのエントリーポイント。
# ルーター登録・バリデーションエラーハンドラー・ヘルスチェックエンドポイントを定義する。
# ------------------------------------------------------------------------------
# 更新履歴：
# 2026/06/08 hori-ya 初版作成
# ------------------------------------------------------------------------------
# Copyright (C) 2026 Skilize Project. All Rights Reserved.
# ******************************************************************************
import logging

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api.v1 import career_analysis, chat

logging.basicConfig(level=logging.INFO)

app = FastAPI(title="Skilize AI Service", version="1.0.0")
app.include_router(career_analysis.router)
app.include_router(chat.router)


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """
    リクエストバリデーションエラーのハンドラー。

    422 エラー発生時にリクエストボディとエラー詳細をログ出力し、
    クライアントに JSON 形式でエラー内容を返す。

    Args:
        request: FastAPI リクエストオブジェクト
        exc: バリデーションエラー例外

    Returns:
        JSONResponse（ステータス 422・エラー詳細）
    """
    logging.getLogger(__name__).error(
        "422 Validation error: body=%s errors=%s", await request.body(), exc.errors()
    )
    return JSONResponse(status_code=422, content={"detail": exc.errors()})


@app.get("/health")
def health():
    """
    ヘルスチェックエンドポイント。

    サービスの死活監視用。常に {"status": "ok"} を返す。

    Returns:
        {"status": "ok"}
    """
    return {"status": "ok"}

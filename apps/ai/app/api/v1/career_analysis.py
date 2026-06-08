# ******************************************************************************
# 機能ID      ：AI
# 機能名      ：AI機能
# 作成日      ：2026/06/08
# 作成者      ：hori-ya
# ------------------------------------------------------------------------------
# 機能概要：
# AIキャリア分析のエンドポイント定義。
# バックグラウンドタスクで分析を非同期起動し、即座に 202 Accepted を返す。
# 分析完了後は DB（ai_career_analyses）にステータスと結果を保存する。
# ------------------------------------------------------------------------------
# 更新履歴：
# 2026/06/08 hori-ya 初版作成
# ------------------------------------------------------------------------------
# Copyright (C) 2026 Skilize Project. All Rights Reserved.
# ******************************************************************************
import logging

from fastapi import APIRouter, BackgroundTasks, Depends

from app.api.dependencies import verify_internal_key
from app.schemas.career_analysis import AnalyzeRequest
from app.services.career_analysis_service import process_analysis

logger = logging.getLogger(__name__)
router = APIRouter()


@router.post("/analyze", status_code=202, dependencies=[Depends(verify_internal_key)])
def analyze(req: AnalyzeRequest, background_tasks: BackgroundTasks):
    """
    AIキャリア分析トリガーエンドポイント。

    バックグラウンドタスクで分析処理を起動し、即座に 202 Accepted を返す。
    分析はバックグラウンドで非同期に実行され、完了後は DB にステータスと結果を保存する。

    Args:
        req: 分析要求（userId・fiscalYearId）
        background_tasks: FastAPI バックグラウンドタスク

    Returns:
        {"accepted": True}（HTTP 202 Accepted）
    """
    logger.info("Received /analyze request: user=%s fiscalYear=%s", req.userId, req.fiscalYearId)
    background_tasks.add_task(process_analysis, req.userId, req.fiscalYearId)
    return {"accepted": True}

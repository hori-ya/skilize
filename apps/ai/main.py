"""
AI キャリア分析サービスのエントリーポイント。
Spring Boot バックエンドからの内部 HTTP 呼び出しのみを受け付ける（外部公開しない）。
/analyze は即時 202 を返し、LLM 呼び出しをバックグラウンドタスクで非同期処理する。
"""
import logging
import os

from fastapi import FastAPI, Header, HTTPException, BackgroundTasks, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from analyzer import run_analysis
from db import fetch_analysis_data, update_status

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI()


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    logger.error("422 Validation error: body=%s errors=%s", await request.body(), exc.errors())
    return JSONResponse(status_code=422, content={"detail": exc.errors()})

# 環境変数 AI_SECRET_KEY が設定されている場合、X-Internal-Key ヘッダーで内部認証を行う
AI_SECRET_KEY = os.environ.get("AI_SECRET_KEY", "")


class AnalyzeRequest(BaseModel):
    userId: int
    fiscalYearId: int


def _process(user_id: int, fiscal_year_id: int):
    """分析処理本体。DB から棚卸データを取得し LLM で分析して結果を保存する。"""
    update_status(user_id, fiscal_year_id, "PROCESSING")
    try:
        data = fetch_analysis_data(user_id, fiscal_year_id)
        result_json = run_analysis(data)
        update_status(user_id, fiscal_year_id, "COMPLETED", analysis_result=result_json)
        logger.info("Analysis completed for user=%s fiscal_year=%s", user_id, fiscal_year_id)
    except Exception as e:
        logger.exception("Analysis failed for user=%s fiscal_year=%s", user_id, fiscal_year_id)
        update_status(user_id, fiscal_year_id, "FAILED", error_message=str(e)[:500])


@app.post("/analyze", status_code=202)
def analyze(
    req: AnalyzeRequest,
    background_tasks: BackgroundTasks,
    x_internal_key: str = Header(default="", alias="X-Internal-Key"),
):
    logger.info("Received /analyze request: user=%s fiscalYear=%s", req.userId, req.fiscalYearId)
    if AI_SECRET_KEY and x_internal_key != AI_SECRET_KEY:
        logger.warning("Forbidden: invalid X-Internal-Key")
        raise HTTPException(status_code=403, detail="Forbidden")
    # バックグラウンドタスクで分析を実行し、202 Accepted を即時返す（fire-and-forget）
    background_tasks.add_task(_process, req.userId, req.fiscalYearId)
    return {"accepted": True}


@app.get("/health")
def health():
    return {"status": "ok"}

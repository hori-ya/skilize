import logging

from fastapi import APIRouter, BackgroundTasks, Depends

from app.api.dependencies import verify_internal_key
from app.schemas.career_analysis import AnalyzeRequest
from app.services.career_analysis_service import process_analysis

logger = logging.getLogger(__name__)
router = APIRouter()


@router.post("/analyze", status_code=202, dependencies=[Depends(verify_internal_key)])
def analyze(req: AnalyzeRequest, background_tasks: BackgroundTasks):
    logger.info("Received /analyze request: user=%s fiscalYear=%s", req.userId, req.fiscalYearId)
    background_tasks.add_task(process_analysis, req.userId, req.fiscalYearId)
    return {"accepted": True}

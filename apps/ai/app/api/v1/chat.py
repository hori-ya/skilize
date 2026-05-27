import logging

from fastapi import APIRouter, Depends, HTTPException, status

from app.api.dependencies import verify_internal_key
from app.schemas.chat import ChatRequest, ChatResponse
from app.services.chat_service import process_chat

logger = logging.getLogger(__name__)
router = APIRouter()


@router.post("/chat", dependencies=[Depends(verify_internal_key)])
def chat(req: ChatRequest) -> ChatResponse:
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

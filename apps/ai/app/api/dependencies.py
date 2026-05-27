from fastapi import Header, HTTPException, status

from app.core.config import settings


async def verify_internal_key(x_internal_key: str = Header(default="", alias="X-Internal-Key")):
    if settings.ai_secret_key and x_internal_key != settings.ai_secret_key:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden")

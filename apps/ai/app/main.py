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
    logging.getLogger(__name__).error(
        "422 Validation error: body=%s errors=%s", await request.body(), exc.errors()
    )
    return JSONResponse(status_code=422, content={"detail": exc.errors()})


@app.get("/health")
def health():
    return {"status": "ok"}

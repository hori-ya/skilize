FROM python:3.12-slim

# 社内プロキシ対応: docker compose build --build-arg HTTP_PROXY=... で渡す
ARG HTTP_PROXY
ARG HTTPS_PROXY
ARG NO_PROXY

WORKDIR /app

COPY apps/ai/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY apps/ai/ .

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]

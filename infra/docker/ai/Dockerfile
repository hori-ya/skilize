FROM python:3.12-slim

# 社内プロキシ対応: docker compose build --build-arg HTTP_PROXY=... で渡す
ARG HTTP_PROXY
ARG HTTPS_PROXY
ARG NO_PROXY
# 社内CA証明書対応: CA_CERT_ENABLED=true かつ infra/certs/company-root-ca.cer が存在する場合に組み込む
ARG CA_CERT_ENABLED=false

# CA証明書のインストール（CA_CERT_ENABLED=true のときのみ実行）
COPY infra/certs/ /tmp/certs/
RUN if [ "$CA_CERT_ENABLED" = "true" ] && [ -f /tmp/certs/company-root-ca.cer ]; then \
      cp /tmp/certs/company-root-ca.cer /usr/local/share/ca-certificates/company-root-ca.crt && \
      update-ca-certificates; \
    fi

# Python の requests・pip がシステム CA バンドルを参照するよう設定
# python:slim は ca-certificates 組み込み済みのため常に安全に参照できる
ENV REQUESTS_CA_BUNDLE=/etc/ssl/certs/ca-certificates.crt

WORKDIR /app

COPY apps/ai/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY apps/ai/ .

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]

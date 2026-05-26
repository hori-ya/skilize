# NODE_VERSION は .env の NODE_VERSION 変数から docker-compose 経由で渡される（デフォルト: 20）
ARG NODE_VERSION=20
FROM node:${NODE_VERSION}-slim

# 社内プロキシ対応: docker compose build --build-arg HTTP_PROXY=... で渡す
ARG HTTP_PROXY
ARG HTTPS_PROXY
ARG NO_PROXY
# 社内CA証明書対応: CA_CERT_ENABLED=true かつ infra/certs/company-root-ca.cer が存在する場合に組み込む
ARG CA_CERT_ENABLED=false

# CA証明書のインストール（CA_CERT_ENABLED=true のときのみ実行）
COPY infra/certs/ /tmp/certs/
#RUN apk add --no-cache ca-certificates
#RUN if [ "$CA_CERT_ENABLED" = "true" ] && [ -f /tmp/certs/company-root-ca.cer ]; then \
#      mkdir -p /usr/local/share/ca-certificates && \
#      cp /tmp/certs/company-root-ca.cer /usr/local/share/ca-certificates/company-root-ca.crt && \
#      update-ca-certificates; \
#    fi
RUN apt-get update && \
    apt-get install -y ca-certificates && \
    if [ "$CA_CERT_ENABLED" = "true" ] && [ -f /tmp/certs/company-root-ca.cer ]; then \
      cp /tmp/certs/company-root-ca.cer /usr/local/share/ca-certificates/company-root-ca.crt && \
      update-ca-certificates; \
    fi
    
# Node.js が OS の CA バンドル（会社証明書を含む）を追加で信頼するよう設定
# node:alpine は ca-certificates 組み込み済みのため常に安全に参照できる
ENV NODE_EXTRA_CA_CERTS=/etc/ssl/certs/ca-certificates.crt

WORKDIR /app
COPY apps/frontend/package*.json ./
# npm は ARG で渡されたプロキシを自動では読まないため、設定されている場合のみ明示的に指定する
RUN if [ -n "$HTTP_PROXY" ]; then npm config set proxy "$HTTP_PROXY"; fi && \
    if [ -n "$HTTPS_PROXY" ]; then npm config set https-proxy "$HTTPS_PROXY"; fi && \
    if [ "$CA_CERT_ENABLED" = "true" ]; then npm config set cafile /etc/ssl/certs/ca-certificates.crt; fi && \
    npm install

FROM node:20-alpine

# 社内プロキシ対応: docker compose build --build-arg HTTP_PROXY=... で渡す
ARG HTTP_PROXY
ARG HTTPS_PROXY
ARG NO_PROXY

WORKDIR /app
COPY apps/frontend/package*.json ./
# npm は ARG で渡されたプロキシを自動では読まないため、設定されている場合のみ明示的に指定する
RUN if [ -n "$HTTP_PROXY" ]; then npm config set proxy "$HTTP_PROXY"; fi && \
    if [ -n "$HTTPS_PROXY" ]; then npm config set https-proxy "$HTTPS_PROXY"; fi && \
    npm install

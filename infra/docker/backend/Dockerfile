FROM eclipse-temurin:21-jdk-alpine

# 社内プロキシ対応: docker compose build --build-arg HTTP_PROXY=... で渡す
ARG HTTP_PROXY
ARG HTTPS_PROXY
ARG NO_PROXY
# 社内CA証明書対応: CA_CERT_ENABLED=true かつ infra/certs/company-root-ca.cer が存在する場合に組み込む
ARG CA_CERT_ENABLED=false

# CA証明書を最初にインストール（後続の wget HTTPS 通信に必要）
COPY infra/certs/ /tmp/certs/
RUN apk add --no-cache ca-certificates && \
    if [ "$CA_CERT_ENABLED" = "true" ] && [ -f /tmp/certs/company-root-ca.cer ]; then \
      cp /tmp/certs/company-root-ca.cer /usr/local/share/ca-certificates/company-root-ca.crt && \
      update-ca-certificates; \
    fi

ENV GRADLE_VERSION=9.4.1

RUN apk add --no-cache wget unzip && \
    wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    rm /tmp/gradle.zip && \
    ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle

# Java キーストアへのCA証明書インポート（Gradle 依存解決・Spring Boot 外部 HTTPS 通信に必要）
RUN if [ "$CA_CERT_ENABLED" = "true" ] && [ -f /tmp/certs/company-root-ca.cer ]; then \
      keytool -importcert -noprompt \
        -alias company-root-ca \
        -file /tmp/certs/company-root-ca.cer \
        -keystore "$JAVA_HOME/lib/security/cacerts" \
        -storepass changeit; \
    fi

WORKDIR /app

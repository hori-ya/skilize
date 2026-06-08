# ******************************************************************************
# 機能ID      ：AI
# 機能名      ：AI機能
# 作成日      ：2026/06/08
# 作成者      ：hori-ya
# ------------------------------------------------------------------------------
# 機能概要：
# pydantic-settings による環境変数管理。
# .env ファイルまたは OS 環境変数から設定値を読み込み、アプリ全体に提供する。
# ------------------------------------------------------------------------------
# 更新履歴：
# 2026/06/08 hori-ya 初版作成
# ------------------------------------------------------------------------------
# Copyright (C) 2026 Skilize Project. All Rights Reserved.
# ******************************************************************************
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """
    アプリケーション設定クラス。

    pydantic-settings により .env ファイルおよび環境変数から自動的に値を読み込む。

    database_url: PostgreSQL 接続 URL
    ai_secret_key: 内部認証用シークレットキー（X-Internal-Key ヘッダーと照合）
    llm_provider: 使用する LLM プロバイダー（"openai" または "anthropic"）
    llm_model: 使用するモデル名（例: "gpt-4o", "claude-3-5-sonnet-20241022"）
    openai_api_key: OpenAI API キー
    anthropic_api_key: Anthropic API キー
    """
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    database_url: str
    ai_secret_key: str = ""
    llm_provider: str = "openai"
    llm_model: str = "gpt-4o"
    openai_api_key: str = ""
    anthropic_api_key: str = ""


settings = Settings()

# ******************************************************************************
# 機能ID      ：AI
# 機能名      ：AI機能
# 作成日      ：2026/06/08
# 作成者      ：hori-ya
# ------------------------------------------------------------------------------
# 機能概要：
# LLM インスタンスのファクトリモジュール。
# LLM_PROVIDER 環境変数により OpenAI / Anthropic を切り替える。
# LangChain の統一インターフェースを通じて各プロバイダーを抽象化する。
# ------------------------------------------------------------------------------
# 更新履歴：
# 2026/06/08 hori-ya 初版作成
# ------------------------------------------------------------------------------
# Copyright (C) 2026 Skilize Project. All Rights Reserved.
# ******************************************************************************
from app.core.config import settings


def build_llm():
    """LLM_PROVIDER 環境変数に応じて OpenAI / Anthropic の LLM インスタンスを返す。"""
    if settings.llm_provider == "anthropic":
        from langchain_anthropic import ChatAnthropic
        return ChatAnthropic(model=settings.llm_model)
    from langchain_openai import ChatOpenAI
    return ChatOpenAI(model=settings.llm_model)

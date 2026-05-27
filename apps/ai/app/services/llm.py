from app.core.config import settings


def build_llm():
    """LLM_PROVIDER 環境変数に応じて OpenAI / Anthropic の LLM インスタンスを返す。"""
    if settings.llm_provider == "anthropic":
        from langchain_anthropic import ChatAnthropic
        return ChatAnthropic(model=settings.llm_model)
    from langchain_openai import ChatOpenAI
    return ChatOpenAI(model=settings.llm_model)

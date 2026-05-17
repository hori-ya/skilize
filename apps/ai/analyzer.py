"""
LangChain を使った LLM 分析ロジック。
LLM_PROVIDER 環境変数で OpenAI / Anthropic を切り替え可能にしている。
プロンプトは prompts/career_analysis.py で管理し、出力は JSON パーサーで構造化する。
"""
import json
import os

from langchain_core.output_parsers import JsonOutputParser
from langchain_core.prompts import ChatPromptTemplate

from prompts.career_analysis import SYSTEM_PROMPT, USER_PROMPT_TEMPLATE


def build_llm():
    """LLM_PROVIDER 環境変数に応じて OpenAI / Anthropic の LLM インスタンスを返す。"""
    provider = os.getenv("LLM_PROVIDER", "openai")
    model = os.getenv("LLM_MODEL", "gpt-4o")

    if provider == "anthropic":
        from langchain_anthropic import ChatAnthropic
        return ChatAnthropic(model=model)
    else:
        from langchain_openai import ChatOpenAI
        return ChatOpenAI(model=model)


def _format_it_skills(items: list[dict]) -> str:
    """ITスキルリストをプロンプト用のテキストに変換する。空の場合は「（データなし）」を返す。"""
    if not items:
        return "（データなし）"
    lines = [f"- {r['skill_name']}: レベル {r['level_value']}" +
             (f"（{r['remarks']}）" if r.get('remarks') else "")
             for r in items]
    return "\n".join(lines)


def _format_qualifications(items: list[dict]) -> str:
    """資格リストをプロンプト用のテキストに変換する。取得年月は YYYY-MM の形式で表示する。"""
    if not items:
        return "（データなし）"
    lines = [f"- {r['qualification_name']}" +
             # acquired_year_month は date 型で YYYY-MM-DD で渡るため、先頭7文字（YYYY-MM）のみ使う
             (f"（取得: {str(r['acquired_year_month'])[:7]}）" if r.get('acquired_year_month') else "")
             for r in items]
    return "\n".join(lines)


def _format_seminars(items: list[dict]) -> str:
    """セミナー受講リストをプロンプト用のテキストに変換する。受講年月は YYYY-MM 形式。"""
    if not items:
        return "（データなし）"
    lines = [f"- {r['seminar_name']}" +
             (f"（{str(r['attended_year_month'])[:7]}）" if r.get('attended_year_month') else "")
             for r in items]
    return "\n".join(lines)


def _format_goals(items: list[dict]) -> str:
    """目標リストをプロンプト用のテキストに変換する。goal_category を日本語に変換して表示する。"""
    if not items:
        return "（データなし）"
    # Enum 値（英語）を日本語表示名にマッピングする
    category_map = {"IT_SKILL": "ITスキル", "QUALIFICATION": "資格", "AD": "AD"}
    lines = [f"- [{category_map.get(r['goal_category'], r['goal_category'])}] {r['target_name']}" +
             (f": {r['reason']}" if r.get('reason') else "")
             for r in items]
    return "\n".join(lines)


def _format_expectations(exp: dict) -> str:
    """TL期待・会社期待をプロンプト用のテキストに変換する。どちらかが未設定の場合はそのフィールドを省略する。"""
    if not exp:
        return "（データなし）"
    parts = []
    if exp.get("tl_expectation"):
        parts.append(f"TLからの期待: {exp['tl_expectation']}")
    if exp.get("company_expectation"):
        parts.append(f"会社からの期待: {exp['company_expectation']}")
    return "\n".join(parts) if parts else "（データなし）"


def run_analysis(data: dict) -> str:
    """棚卸データを LLM に渡して分析し、JSON 文字列で返す。"""
    llm = build_llm()
    # JsonOutputParser: LLM の出力文字列を自動的に JSON にパースする
    parser = JsonOutputParser()

    prompt = ChatPromptTemplate.from_messages([
        ("system", SYSTEM_PROMPT),
        ("human", USER_PROMPT_TEMPLATE),
    ])

    # LCEL（LangChain Expression Language）のパイプ演算子（|）でチェーンを構成する
    # prompt → llm → parser の順に処理が流れる（Unix パイプと同じ概念）
    chain = prompt | llm | parser

    # プロンプトテンプレートの変数を棚卸データで埋めて LLM に送信する
    result = chain.invoke({
        "it_skills": _format_it_skills(data["it_skills"]),
        "qualifications": _format_qualifications(data["qualifications"]),
        "seminars": _format_seminars(data["seminars"]),
        "goals": _format_goals(data["goals"]),
        "expectations": _format_expectations(data["expectation"]),
    })

    # ensure_ascii=False: 日本語をエスケープせずそのまま JSON に含める
    return json.dumps(result, ensure_ascii=False)

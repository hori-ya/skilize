import json
import os

from langchain_core.output_parsers import JsonOutputParser
from langchain_core.prompts import ChatPromptTemplate

from prompts.career_analysis import SYSTEM_PROMPT, USER_PROMPT_TEMPLATE


def build_llm():
    provider = os.getenv("LLM_PROVIDER", "openai")
    model = os.getenv("LLM_MODEL", "gpt-4o")

    if provider == "anthropic":
        from langchain_anthropic import ChatAnthropic
        return ChatAnthropic(model=model)
    else:
        from langchain_openai import ChatOpenAI
        return ChatOpenAI(model=model)


def _format_it_skills(items: list[dict]) -> str:
    if not items:
        return "（データなし）"
    lines = [f"- {r['skill_name']}: レベル {r['level_value']}" +
             (f"（{r['remarks']}）" if r.get('remarks') else "")
             for r in items]
    return "\n".join(lines)


def _format_qualifications(items: list[dict]) -> str:
    if not items:
        return "（データなし）"
    lines = [f"- {r['qualification_name']}" +
             (f"（取得: {str(r['acquired_year_month'])[:7]}）" if r.get('acquired_year_month') else "")
             for r in items]
    return "\n".join(lines)


def _format_seminars(items: list[dict]) -> str:
    if not items:
        return "（データなし）"
    lines = [f"- {r['seminar_name']}" +
             (f"（{str(r['attended_year_month'])[:7]}）" if r.get('attended_year_month') else "")
             for r in items]
    return "\n".join(lines)


def _format_goals(items: list[dict]) -> str:
    if not items:
        return "（データなし）"
    category_map = {"IT_SKILL": "ITスキル", "QUALIFICATION": "資格", "AD": "AD"}
    lines = [f"- [{category_map.get(r['goal_category'], r['goal_category'])}] {r['target_name']}" +
             (f": {r['reason']}" if r.get('reason') else "")
             for r in items]
    return "\n".join(lines)


def _format_expectations(exp: dict) -> str:
    if not exp:
        return "（データなし）"
    parts = []
    if exp.get("tl_expectation"):
        parts.append(f"TLからの期待: {exp['tl_expectation']}")
    if exp.get("company_expectation"):
        parts.append(f"会社からの期待: {exp['company_expectation']}")
    return "\n".join(parts) if parts else "（データなし）"


def run_analysis(data: dict) -> str:
    llm = build_llm()
    parser = JsonOutputParser()

    prompt = ChatPromptTemplate.from_messages([
        ("system", SYSTEM_PROMPT),
        ("human", USER_PROMPT_TEMPLATE),
    ])

    chain = prompt | llm | parser

    result = chain.invoke({
        "it_skills": _format_it_skills(data["it_skills"]),
        "qualifications": _format_qualifications(data["qualifications"]),
        "seminars": _format_seminars(data["seminars"]),
        "goals": _format_goals(data["goals"]),
        "expectations": _format_expectations(data["expectation"]),
    })

    return json.dumps(result, ensure_ascii=False)

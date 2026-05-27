import logging

import psycopg2
import psycopg2.extras
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

from app.core.config import settings
from app.services.llm import build_llm
from app.services.prompts.chat_prompts import (
    CAREER_SYSTEM_PROMPT,
    CAREER_SYSTEM_PROMPT_NO_DATA,
    HELP_SYSTEM_PROMPT,
    NORMAL_SYSTEM_PROMPT,
    PROOFREADING_SYSTEM_PROMPT,
)

logger = logging.getLogger(__name__)

# 会話履歴の最大件数（古いものから切り捨て）
MAX_HISTORY = 20


def process_chat(message: str, mode: str, user_id: int, history: list[dict]) -> str:
    """モードに応じたシステムプロンプトで LLM に問い合わせ、応答テキストを返す。"""
    llm = build_llm()
    system_prompt = _build_system_prompt(mode, user_id)

    messages = [SystemMessage(content=system_prompt)]
    for item in history[-MAX_HISTORY:]:
        if item["role"] == "user":
            messages.append(HumanMessage(content=item["content"]))
        elif item["role"] == "assistant":
            messages.append(AIMessage(content=item["content"]))
    messages.append(HumanMessage(content=message))

    response = llm.invoke(messages)
    return response.content


def _build_system_prompt(mode: str, user_id: int) -> str:
    if mode == "NORMAL":
        return NORMAL_SYSTEM_PROMPT
    if mode == "PROOFREADING":
        return PROOFREADING_SYSTEM_PROMPT
    if mode == "CAREER":
        return _build_career_prompt(user_id)
    if mode == "HELP":
        return HELP_SYSTEM_PROMPT
    return NORMAL_SYSTEM_PROMPT


def _build_career_prompt(user_id: int) -> str:
    """ユーザーのスキル・目標データを取得してキャリアプロンプトを組み立てる。
    データ取得に失敗した場合はデータなしプロンプトにフォールバックする。"""
    try:
        context = _fetch_career_context(user_id)
        if not context:
            return CAREER_SYSTEM_PROMPT_NO_DATA
        return CAREER_SYSTEM_PROMPT.format(inventory_context=context)
    except Exception:
        logger.warning("Failed to fetch career context for user=%s, using no-data prompt", user_id)
        return CAREER_SYSTEM_PROMPT_NO_DATA


def _fetch_career_context(user_id: int) -> str:
    """現在年度の IT スキル・目標・期待コメントを取得してテキスト形式で返す。"""
    conn = psycopg2.connect(settings.database_url)
    try:
        cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)

        # 最新年度の棚卸 ID を取得
        cur.execute("""
            SELECT inv.id, fy.year
            FROM inventories inv
            JOIN fiscal_years fy ON fy.id = inv.fiscal_year_id
            WHERE inv.user_id = %s
            ORDER BY fy.year DESC
            LIMIT 1
        """, (user_id,))
        latest = cur.fetchone()
        if not latest:
            cur.close()
            return ""

        inventory_id = latest["id"]
        year = latest["year"]

        # IT スキル
        cur.execute("""
            SELECT COALESCE(its.name, isd.custom_skill_name) AS skill_name, sl.level_value
            FROM it_skill_details isd
            JOIN skill_levels sl ON sl.id = isd.skill_level_id
            LEFT JOIN it_skills its ON its.id = isd.it_skill_id
            WHERE isd.inventory_id = %s
            ORDER BY skill_name
        """, (inventory_id,))
        skills = cur.fetchall()

        # 目標
        cur.execute("""
            SELECT ig.goal_category, COALESCE(its.name, q.name, ads.name, ig.custom_name) AS target_name
            FROM inventory_goals ig
            LEFT JOIN it_skills its ON its.id = ig.it_skill_id
            LEFT JOIN qualifications q ON q.id = ig.qualification_id
            LEFT JOIN ad_seminars ads ON ads.id = ig.ad_seminar_id
            WHERE ig.inventory_id = %s
        """, (inventory_id,))
        goals = cur.fetchall()

        # 期待コメント
        cur.execute("""
            SELECT tl_expectation, company_expectation
            FROM user_expectations
            WHERE user_id = %s
        """, (user_id,))
        expectation = cur.fetchone()

        cur.close()
        return _format_career_context(year, skills, goals, expectation)
    finally:
        conn.close()


def _format_career_context(year: int, skills: list, goals: list, expectation) -> str:
    lines = [f"【{year}年度 棚卸サマリー】"]

    if skills:
        lines.append("\n■ ITスキル")
        for s in skills:
            lines.append(f"  - {s['skill_name']}: レベル {s['level_value']}")
    else:
        lines.append("\n■ ITスキル: （未登録）")

    if goals:
        category_map = {"IT_SKILL": "ITスキル", "QUALIFICATION": "資格", "AD": "AD"}
        lines.append("\n■ 今年度の目標")
        for g in goals:
            cat = category_map.get(g["goal_category"], g["goal_category"])
            lines.append(f"  - [{cat}] {g['target_name']}")
    else:
        lines.append("\n■ 今年度の目標: （未設定）")

    if expectation:
        if expectation.get("tl_expectation"):
            lines.append(f"\n■ TLからの期待: {expectation['tl_expectation']}")
        if expectation.get("company_expectation"):
            lines.append(f"\n■ 会社からの期待: {expectation['company_expectation']}")

    return "\n".join(lines)

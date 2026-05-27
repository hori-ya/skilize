import json
import logging

import psycopg2
import psycopg2.extras
from langchain_core.output_parsers import JsonOutputParser
from langchain_core.prompts import ChatPromptTemplate

from app.core.config import settings
from app.services.llm import build_llm
from app.services.prompts.career_analysis_prompt import SYSTEM_PROMPT, USER_PROMPT_TEMPLATE

logger = logging.getLogger(__name__)


def process_analysis(user_id: int, fiscal_year_id: int) -> None:
    """分析処理本体。DB から棚卸データを取得し LLM で分析して結果を保存する。"""
    _update_status(user_id, fiscal_year_id, "PROCESSING")
    try:
        data = _fetch_analysis_data(user_id, fiscal_year_id)
        result_json = _run_analysis(data)
        _update_status(user_id, fiscal_year_id, "COMPLETED", analysis_result=result_json)
        logger.info("Analysis completed for user=%s fiscal_year=%s", user_id, fiscal_year_id)
    except Exception:
        logger.exception("Analysis failed for user=%s fiscal_year=%s", user_id, fiscal_year_id)
        _update_status(user_id, fiscal_year_id, "FAILED")


def _get_connection():
    return psycopg2.connect(settings.database_url)


def _fetch_analysis_data(user_id: int, fiscal_year_id: int) -> dict:
    """指定ユーザー・年度の棚卸データ（ITスキル・資格・セミナー・目標・期待コメント）を取得する。"""
    conn = _get_connection()
    try:
        cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)

        # ITスキル実績
        cur.execute("""
            SELECT COALESCE(its.name, isd.custom_skill_name) AS skill_name,
                   sl.level_value, isd.remarks
            FROM it_skill_details isd
            JOIN inventories inv ON inv.id = isd.inventory_id
            JOIN skill_levels sl ON sl.id = isd.skill_level_id
            LEFT JOIN it_skills its ON its.id = isd.it_skill_id
            WHERE inv.user_id = %s AND inv.fiscal_year_id = %s
            ORDER BY skill_name
        """, (user_id, fiscal_year_id))
        it_skills = cur.fetchall()

        # 資格実績
        cur.execute("""
            SELECT COALESCE(q.name, qd.custom_qualification_name) AS qualification_name,
                   qd.acquired_year_month
            FROM qualification_details qd
            JOIN inventories inv ON inv.id = qd.inventory_id
            LEFT JOIN qualifications q ON q.id = qd.qualification_id
            WHERE inv.user_id = %s AND inv.fiscal_year_id = %s
            ORDER BY qd.acquired_year_month DESC NULLS LAST
        """, (user_id, fiscal_year_id))
        qualifications = cur.fetchall()

        # セミナー受講実績
        cur.execute("""
            SELECT COALESCE(ads.name, sd.seminar_name) AS seminar_name,
                   sd.attended_year_month
            FROM seminar_details sd
            JOIN inventories inv ON inv.id = sd.inventory_id
            LEFT JOIN ad_seminars ads ON ads.id = sd.ad_seminar_id
            WHERE inv.user_id = %s AND inv.fiscal_year_id = %s
            ORDER BY sd.attended_year_month DESC NULLS LAST
        """, (user_id, fiscal_year_id))
        seminars = cur.fetchall()

        # 今年度の目標
        cur.execute("""
            SELECT ig.goal_category,
                   COALESCE(its.name, q.name, ads.name, ig.custom_name) AS target_name,
                   ig.target_period, ig.reason
            FROM inventory_goals ig
            JOIN inventories inv ON inv.id = ig.inventory_id
            LEFT JOIN it_skills its ON its.id = ig.it_skill_id
            LEFT JOIN qualifications q ON q.id = ig.qualification_id
            LEFT JOIN ad_seminars ads ON ads.id = ig.ad_seminar_id
            WHERE inv.user_id = %s AND inv.fiscal_year_id = %s
            ORDER BY ig.goal_category
        """, (user_id, fiscal_year_id))
        goals = cur.fetchall()

        # 前年度の期待コメント
        cur.execute("""
            SELECT ue.tl_expectation, ue.company_expectation
            FROM user_expectations ue
            WHERE ue.user_id = %s
        """, (user_id,))
        expectation = cur.fetchone()

        cur.close()
        return {
            "it_skills": [dict(r) for r in it_skills],
            "qualifications": [dict(r) for r in qualifications],
            "seminars": [dict(r) for r in seminars],
            "goals": [dict(r) for r in goals],
            "expectation": dict(expectation) if expectation else {},
        }
    finally:
        conn.close()


def _update_status(user_id: int, fiscal_year_id: int, status: str,
                   analysis_result: str | None = None, error_message: str | None = None):
    """ai_career_analyses のステータスを更新する。引数の有無によって更新カラムを切り替える。"""
    conn = _get_connection()
    try:
        cur = conn.cursor()
        if analysis_result is not None:
            cur.execute("""
                UPDATE ai_career_analyses
                SET status = %s, analysis_result = %s::jsonb, error_message = NULL, updated_at = NOW()
                WHERE user_id = %s AND fiscal_year_id = %s
            """, (status, analysis_result, user_id, fiscal_year_id))
        elif error_message is not None:
            cur.execute("""
                UPDATE ai_career_analyses
                SET status = %s, error_message = %s, updated_at = NOW()
                WHERE user_id = %s AND fiscal_year_id = %s
            """, (status, error_message, user_id, fiscal_year_id))
        else:
            cur.execute("""
                UPDATE ai_career_analyses
                SET status = %s, updated_at = NOW()
                WHERE user_id = %s AND fiscal_year_id = %s
            """, (status, user_id, fiscal_year_id))
        conn.commit()
        cur.close()
    finally:
        conn.close()


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
    # acquired_year_month は date 型で YYYY-MM-DD で渡るため、先頭7文字（YYYY-MM）のみ使う
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
    # Enum 値（英語）を日本語表示名にマッピングする
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


def _run_analysis(data: dict) -> str:
    """棚卸データを LLM に渡して分析し、JSON 文字列で返す。"""
    llm = build_llm()
    # JsonOutputParser: LLM の出力文字列を自動的に JSON にパースする
    parser = JsonOutputParser()

    prompt = ChatPromptTemplate.from_messages([
        ("system", SYSTEM_PROMPT),
        ("human", USER_PROMPT_TEMPLATE),
    ])

    # LCEL（LangChain Expression Language）のパイプ演算子（|）でチェーンを構成する
    chain = prompt | llm | parser

    result = chain.invoke({
        "it_skills": _format_it_skills(data["it_skills"]),
        "qualifications": _format_qualifications(data["qualifications"]),
        "seminars": _format_seminars(data["seminars"]),
        "goals": _format_goals(data["goals"]),
        "expectations": _format_expectations(data["expectation"]),
    })

    # ensure_ascii=False: 日本語をエスケープせずそのまま JSON に含める
    return json.dumps(result, ensure_ascii=False)

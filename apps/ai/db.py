"""
AI モジュール用 DB アクセス。
Spring Boot API を経由せず直接 PostgreSQL に接続する（内部サービスのため API 呼び出しのオーバーヘッドを避ける）。
"""
import os
import psycopg2
import psycopg2.extras


def get_connection():
    return psycopg2.connect(os.environ["DATABASE_URL"])


def fetch_analysis_data(user_id: int, fiscal_year_id: int) -> dict:
    """指定ユーザー・年度の棚卸データ（ITスキル・資格・セミナー・目標・期待コメント）を取得する。"""
    conn = get_connection()
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
            SELECT
                COALESCE(ads.name, sd.seminar_name) AS seminar_name,
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
            SELECT
                ig.goal_category,
                COALESCE(its.name, q.name, ads.name, ig.custom_name) AS target_name,
                ig.target_period,
                ig.reason
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


def update_status(user_id: int, fiscal_year_id: int, status: str,
                  analysis_result: str | None = None, error_message: str | None = None):
    """ai_career_analyses のステータスを更新する。引数の有無によって更新カラムを切り替える。"""
    conn = get_connection()
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

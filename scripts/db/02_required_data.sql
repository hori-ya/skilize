-- =============================================================================
-- 02_required_data.sql — 必須データ（ローカル開発用）
--
-- システム起動に必須のデータのみを登録する。
-- 本番環境相当の Flyway V2 との差異:
--   - admin の is_initial_password = FALSE（ローカルでは初回変更不要）
-- =============================================================================

-- 年度設定（シングルトン / 4月始まり）
INSERT INTO fiscal_year_settings (id, fiscal_year_start_month)
VALUES (1, 4)
ON CONFLICT (id) DO NOTHING;

-- レベルマスタ（1〜5）
INSERT INTO skill_levels (level_value, description, score_weight) VALUES
    (1, '知識なし／未経験',         0),
    (2, '概念を理解している',        1),
    (3, '指導があれば実務で使える',  2),
    (4, '独力で実務に適用できる',    3),
    (5, '他者に指導・展開できる',    4);

-- 管理者ユーザー
-- 初期PW: admin（BCrypt cost=12）
-- ローカル開発用のため is_initial_password=FALSE（ログイン後のパスワード変更不要）
INSERT INTO users (user_id, name, password_hash, role, is_initial_password, is_active)
VALUES (
    'admin',
    '管理者',
    '$2b$12$6zPU82VzWT9rZ7jLF.3yp.qm815BLk3o6j47RjxRu1ZN7CQBPo0Li',
    'ADMIN',
    FALSE,
    TRUE
);

-- =============================================================================
-- V2: 必須データ登録
--
-- システム起動に必須のデータのみを登録する。
-- ローカル開発用（02_required_data.sql）との差異:
--   - admin の is_initial_password = TRUE（本番では初回ログイン時にパスワード変更を強制）
-- =============================================================================

-- 年度設定（シングルトン / 4月始まり）
INSERT INTO fiscal_year_settings (id, fiscal_year_start_month)
VALUES (1, 4)
ON CONFLICT (id) DO NOTHING;

-- レベルマスタ（1〜5）
INSERT INTO skill_levels (level_value, description) VALUES
    (1, '知識なし／未経験'),
    (2, '概念を理解している'),
    (3, '指導があれば実務で使える'),
    (4, '独力で実務に適用できる'),
    (5, '他者に指導・展開できる');

-- 管理者ユーザー
-- 初期PW: admin（BCrypt cost=12）
-- is_initial_password=TRUE → 初回ログイン後に必ずパスワードを変更すること
INSERT INTO users (user_id, name, password_hash, role, is_initial_password, is_active)
VALUES (
    'admin',
    '管理者',
    '$2b$12$6zPU82VzWT9rZ7jLF.3yp.qm815BLk3o6j47RjxRu1ZN7CQBPo0Li',
    'ADMIN',
    TRUE,
    TRUE
);

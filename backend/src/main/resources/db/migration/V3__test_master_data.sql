-- =============================================================================
-- V3: テスト用最小マスタデータ
-- =============================================================================

-- 年度
INSERT INTO fiscal_years (name, start_date, end_date, input_start_date, input_end_date, is_active)
VALUES ('2025年度', '2025-04-01', '2026-03-31', '2025-04-01', '2025-06-30', TRUE);

-- =============================================================================
-- ITスキル分類（3階層のうち Lv1 / Lv2 のみ）
-- =============================================================================

INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    (NULL, 1, 'インフラ',           1),
    (NULL, 1, 'アプリケーション',   2),
    (NULL, 1, 'マネジメント',       3);

INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'インフラ'         AND level = 1), 2, 'OS / サーバー',     1),
    ((SELECT id FROM it_skill_categories WHERE name = 'インフラ'         AND level = 1), 2, 'ネットワーク',       2),
    ((SELECT id FROM it_skill_categories WHERE name = 'インフラ'         AND level = 1), 2, 'クラウド',           3),
    ((SELECT id FROM it_skill_categories WHERE name = 'アプリケーション' AND level = 1), 2, 'フロントエンド',     1),
    ((SELECT id FROM it_skill_categories WHERE name = 'アプリケーション' AND level = 1), 2, 'バックエンド',       2),
    ((SELECT id FROM it_skill_categories WHERE name = 'アプリケーション' AND level = 1), 2, 'データベース',       3),
    ((SELECT id FROM it_skill_categories WHERE name = 'マネジメント'     AND level = 1), 2, 'プロジェクト管理',   1),
    ((SELECT id FROM it_skill_categories WHERE name = 'マネジメント'     AND level = 1), 2, 'プロセス改善',       2);

-- =============================================================================
-- ITスキル
-- =============================================================================

INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'OS / サーバー'),   'Linux',                      1),
    ((SELECT id FROM it_skill_categories WHERE name = 'OS / サーバー'),   'Windows Server',             2),
    ((SELECT id FROM it_skill_categories WHERE name = 'ネットワーク'),    'TCP/IP・ネットワーク基礎',   1),
    ((SELECT id FROM it_skill_categories WHERE name = 'ネットワーク'),    'ルーター / スイッチ設定',    2),
    ((SELECT id FROM it_skill_categories WHERE name = 'クラウド'),        'AWS',                        1),
    ((SELECT id FROM it_skill_categories WHERE name = 'クラウド'),        'Azure',                      2),
    ((SELECT id FROM it_skill_categories WHERE name = 'フロントエンド'),  'HTML / CSS',                 1),
    ((SELECT id FROM it_skill_categories WHERE name = 'フロントエンド'),  'JavaScript',                 2),
    ((SELECT id FROM it_skill_categories WHERE name = 'フロントエンド'),  'React',                      3),
    ((SELECT id FROM it_skill_categories WHERE name = 'バックエンド'),    'Java',                       1),
    ((SELECT id FROM it_skill_categories WHERE name = 'バックエンド'),    'Python',                     2),
    ((SELECT id FROM it_skill_categories WHERE name = 'バックエンド'),    'Spring Boot',                3),
    ((SELECT id FROM it_skill_categories WHERE name = 'データベース'),    'SQL基礎',                    1),
    ((SELECT id FROM it_skill_categories WHERE name = 'データベース'),    'PostgreSQL',                 2),
    ((SELECT id FROM it_skill_categories WHERE name = 'データベース'),    'MySQL',                      3),
    ((SELECT id FROM it_skill_categories WHERE name = 'プロジェクト管理'),'WBS / スケジュール管理',     1),
    ((SELECT id FROM it_skill_categories WHERE name = 'プロジェクト管理'),'リスク管理',                 2),
    ((SELECT id FROM it_skill_categories WHERE name = 'プロセス改善'),   'アジャイル / スクラム',       1);

-- =============================================================================
-- 資格分類・資格
-- =============================================================================

INSERT INTO qualification_categories (name, sort_order) VALUES
    ('国家資格（IT系）', 1),
    ('ベンダー資格',     2),
    ('その他',           3);

INSERT INTO qualifications (category_id, name, sort_order) VALUES
    ((SELECT id FROM qualification_categories WHERE name = '国家資格（IT系）'), '基本情報技術者（FE）',                          1),
    ((SELECT id FROM qualification_categories WHERE name = '国家資格（IT系）'), '応用情報技術者（AP）',                          2),
    ((SELECT id FROM qualification_categories WHERE name = '国家資格（IT系）'), '情報処理安全確保支援士（SC）',                   3),
    ((SELECT id FROM qualification_categories WHERE name = '国家資格（IT系）'), 'データベーススペシャリスト（DB）',               4),
    ((SELECT id FROM qualification_categories WHERE name = 'ベンダー資格'),     'AWS認定ソリューションアーキテクト - アソシエイト', 1),
    ((SELECT id FROM qualification_categories WHERE name = 'ベンダー資格'),     'Oracle Java SE 認定',                           2),
    ((SELECT id FROM qualification_categories WHERE name = 'ベンダー資格'),     'Microsoft Azure Administrator（AZ-104）',        3);

-- =============================================================================
-- ADセミナー分類・ADセミナー
-- =============================================================================

INSERT INTO ad_seminar_categories (name, sort_order) VALUES
    ('技術研修',           1),
    ('ビジネス研修',       2),
    ('コンプライアンス研修', 3);

INSERT INTO ad_seminars (category_id, name, sort_order) VALUES
    ((SELECT id FROM ad_seminar_categories WHERE name = '技術研修'),           'クラウド入門',       1),
    ((SELECT id FROM ad_seminar_categories WHERE name = '技術研修'),           'セキュリティ基礎',   2),
    ((SELECT id FROM ad_seminar_categories WHERE name = '技術研修'),           'Java中級',           3),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'ビジネス研修'),       'ビジネスマナー研修', 1),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'ビジネス研修'),       'プロジェクト管理基礎', 2),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'コンプライアンス研修'), '情報セキュリティ教育', 1),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'コンプライアンス研修'), '個人情報保護研修',   2);

-- =============================================================================
-- 社外セミナー分類
-- =============================================================================

INSERT INTO seminar_categories (name, sort_order) VALUES
    ('技術',   1),
    ('ビジネス', 2),
    ('その他', 3);

-- =============================================================================
-- テストユーザー
-- 初期パスワード = ユーザーID（ログイン後に変更が必要）
-- =============================================================================

-- tl01 / 初期PW: tl01
INSERT INTO users (user_id, name, password_hash, role, is_initial_password, is_active)
VALUES ('tl01', 'テストTL', '$2a$12$EePvdSwevAErJKjplTw7nOQUbNuclA0YAzG5DLm7.zLWsw51jJxXW', 'TL', TRUE, TRUE);

-- user01, user02 / 初期PW: 各ユーザーID
INSERT INTO users (user_id, name, password_hash, role, tl_user_id, is_initial_password, is_active) VALUES
    ('user01', 'テストユーザー01', '$2a$12$5eDTZtPIqIRxeGm7bOBcZ.D2lFBxREG.PM9LDzJuM3ipvL.CSfThm', 'GENERAL', (SELECT id FROM users WHERE user_id = 'tl01'), TRUE, TRUE),
    ('user02', 'テストユーザー02', '$2a$12$dLqToObRmzZ1J/PsK.08x.Xq6ttTLieZNFHqJw411mF/6nS1vGl/S', 'GENERAL', (SELECT id FROM users WHERE user_id = 'tl01'), TRUE, TRUE);

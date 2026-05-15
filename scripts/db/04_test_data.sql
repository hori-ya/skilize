-- =============================================================================
-- 04_test_data.sql — テストデータ（ローカル開発専用）
--
-- 【注意】本番環境・ステージング環境には適用しないこと。
--
-- 登録内容:
--   - テストユーザー: tl01 / user01 / user02（初期PW = ユーザーID）
--   - 前年度棚卸データ: tl01・user01・user02 の 2024年度 棚卸実績・目標
-- =============================================================================

-- =============================================================================
-- テストユーザー
-- 初期PW = ユーザーID（BCrypt cost=12）
-- is_initial_password=TRUE → ログイン後にパスワード変更が必要
-- =============================================================================

-- tl01 / 初期PW: tl01
INSERT INTO users (user_id, name, password_hash, role, is_initial_password, is_active)
VALUES ('tl01', 'テストTL', '$2a$12$EePvdSwevAErJKjplTw7nOQUbNuclA0YAzG5DLm7.zLWsw51jJxXW', 'TL', TRUE, TRUE);

-- user01 / 初期PW: user01
-- user02 / 初期PW: user02
INSERT INTO users (user_id, name, password_hash, role, tl_user_id, is_initial_password, is_active) VALUES
    ('user01', 'テストユーザー01', '$2a$12$5eDTZtPIqIRxeGm7bOBcZ.D2lFBxREG.PM9LDzJuM3ipvL.CSfThm', 'GENERAL', (SELECT id FROM users WHERE user_id = 'tl01'), TRUE, TRUE),
    ('user02', 'テストユーザー02', '$2a$12$dLqToObRmzZ1J/PsK.08x.Xq6ttTLieZNFHqJw411mF/6nS1vGl/S', 'GENERAL', (SELECT id FROM users WHERE user_id = 'tl01'), TRUE, TRUE);

-- =============================================================================
-- 前年度（2024年度）棚卸データ
-- 2025年度の SCR-019（目標振り返り）動作確認に使用
-- =============================================================================

INSERT INTO fiscal_years (name, start_date, end_date, input_start_date, input_end_date, is_active)
VALUES ('2024年度', '2024-04-01', '2025-03-31', '2024-04-01', '2024-06-30', TRUE);

-- ─── user01 / 2024年度（COMPLETED） ─────────────────────────────────────────

INSERT INTO inventories (user_id, fiscal_year_id, status, submitted_at, goal_review_completed_at, goal_completed_at)
VALUES (
    (SELECT id FROM users WHERE user_id = 'user01'),
    (SELECT id FROM fiscal_years WHERE name = '2024年度'),
    'COMPLETED',
    '2024-05-08 10:00:00+09',
    '2024-05-09 11:00:00+09',
    '2024-05-10 15:00:00+09'
);

-- レベル: 4=独力で実務に適用できる / 3=指導があれば実務で使える
INSERT INTO it_skill_details (inventory_id, it_skill_id, skill_level_id, remarks)
SELECT
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'user01'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
    s.id,
    (SELECT id FROM skill_levels WHERE level_value = v.lv),
    v.rem
FROM (VALUES
    ('詳細設計書を読み、PGの作成、改修をすることができる。',                          4, '実務での主力作業'),
    ('PL/SQLを作成、改修することができる。',                                          3, 'Oracle DB での開発経験あり'),
    ('SQLの基本的な操作をすることができる。',                                          4, 'サブクエリ・JOINなど複雑なクエリも可'),
    ('特定のフレームワークを理解するように意識して実践している。',                    3, 'Spring Boot を中心に習熟中'),
    ('サーバー（Linux、Windowsなど）の設定をすることができる。',                      3, 'Linux サーバーの基本的な設定が可能'),
    ('コーディング規約を理解し、遵守するように意識して実践している。',                4, NULL),
    ('可読性、保守性、拡張性の高いコード、SQL等を作成するように意識して実践している。', 3, NULL),
    ('単体テスト仕様書をもとにテストを実施することができる。',                        4, NULL),
    ('結合テスト仕様書をもとにテストを実施することができる。',                        3, NULL),
    ('バージョン管理システム（Gitなど）を使用することができる。',                      4, 'Git / GitHub を日常的に使用'),
    ('作業の進捗について報告・連絡・相談をするように意識して実践している。',          4, NULL)
) AS v(skill_name, lv, rem)
JOIN it_skills s ON s.name = v.skill_name;

-- 資格明細
INSERT INTO qualification_details (inventory_id, qualification_id, acquired_year_month, remarks)
VALUES
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        (SELECT id FROM qualifications WHERE name = '基本情報技術者試験'),
        '2020-06-01', '在学中に取得'
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        (SELECT id FROM qualifications WHERE name = 'AWS認定資格 Associate Solutions Architect'),
        '2023-08-01', 'SAA-C03で合格'
    );

-- セミナー明細
INSERT INTO seminar_details (inventory_id, ad_seminar_id, seminar_name, seminar_category_id, attended_year_month, remarks)
VALUES
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        (SELECT id FROM ad_seminars WHERE name = 'プロジェクトマネジメントの全体像'),
        NULL, NULL, '2024-04-01', NULL
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        (SELECT id FROM ad_seminars WHERE name = 'ビジネスマナー研修1＜良好な人間関係を築く5要素＞'),
        NULL, NULL, '2024-05-01', NULL
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        NULL, 'Python勉強会',
        (SELECT id FROM seminar_categories WHERE name = '技術'),
        '2024-03-01', '社内有志勉強会'
    );

-- 目標（2025年度の SCR-019 振り返りで参照される）
INSERT INTO inventory_goals (inventory_id, goal_category, it_skill_id, qualification_id, ad_seminar_id, custom_name, target_period, reason)
VALUES
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        'IT_SKILL',
        (SELECT id FROM it_skills WHERE name = '基本設計書を作成することができる。'),
        NULL, NULL, NULL,
        '2025-03-01',
        'SE へのキャリアアップに向けて、上流工程のスキルを習得したい'
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        'QUALIFICATION',
        NULL,
        (SELECT id FROM qualifications WHERE name = '応用情報技術者試験'),
        NULL, NULL,
        '2025-10-01',
        'キャリアアップのために秋期試験での取得を目指す'
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        'AD',
        NULL, NULL,
        (SELECT id FROM ad_seminars WHERE name = '［IT業向け］はじめてのプロジェクト管理シリーズ＜WBS作成編＞'),
        NULL,
        '2025-02-01',
        'プロジェクト管理の基礎を身につけ、SE ロールへのステップアップに活かしたい'
    );

-- ─── user02 / 2024年度（COMPLETED） ─────────────────────────────────────────

INSERT INTO inventories (user_id, fiscal_year_id, status, submitted_at, goal_review_completed_at, goal_completed_at)
VALUES (
    (SELECT id FROM users WHERE user_id = 'user02'),
    (SELECT id FROM fiscal_years WHERE name = '2024年度'),
    'COMPLETED',
    '2024-05-12 14:00:00+09',
    '2024-05-13 10:00:00+09',
    '2024-05-14 16:00:00+09'
);

-- レベル: 3=指導があれば実務で使える / 2=概念を理解している
INSERT INTO it_skill_details (inventory_id, it_skill_id, skill_level_id, remarks)
SELECT
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'user02'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
    s.id,
    (SELECT id FROM skill_levels WHERE level_value = v.lv),
    v.rem
FROM (VALUES
    ('詳細設計書を読み、PGの作成、改修をすることができる。',                3, NULL),
    ('SQLの基本的な操作をすることができる。',                                3, NULL),
    ('特定のフレームワークを理解するように意識して実践している。',          2, NULL),
    ('データベース（Oracle、PostgreSQL、SQL Serverなど）の設定をすることができる。', 2, 'Oracle DB の基本設定経験あり'),
    ('コーディング規約を理解し、遵守するように意識して実践している。',      3, NULL),
    ('詳細設計書をもとに単体テスト仕様書を作成することができる。',          2, NULL),
    ('単体テスト仕様書をもとにテストを実施することができる。',              3, NULL),
    ('結合テスト仕様書をもとにテストを実施することができる。',              2, NULL),
    ('バージョン管理システム（Gitなど）を使用することができる。',            3, NULL),
    ('WBS、ガントチャートなどを作成し、作業を管理することができる。',       2, 'サブリーダーとして補佐経験あり'),
    ('作業の進捗について報告・連絡・相談をするように意識して実践している。', 3, NULL)
) AS v(skill_name, lv, rem)
JOIN it_skills s ON s.name = v.skill_name;

INSERT INTO qualification_details (inventory_id, qualification_id, acquired_year_month, remarks)
VALUES (
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'user02'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
    (SELECT id FROM qualifications WHERE name = '基本情報技術者試験'),
    '2022-05-01', NULL
);

INSERT INTO seminar_details (inventory_id, ad_seminar_id, seminar_name, seminar_category_id, attended_year_month, remarks)
VALUES (
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'user02'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
    (SELECT id FROM ad_seminars WHERE name = '新入社員研修'),
    NULL, NULL, '2024-04-01', NULL
);

INSERT INTO inventory_goals (inventory_id, goal_category, it_skill_id, qualification_id, ad_seminar_id, custom_name, target_period, reason)
VALUES
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user02'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        'IT_SKILL',
        (SELECT id FROM it_skills WHERE name = 'クラウドサービス（AWS、Azure、GCPなど）の設定をすることができる。'),
        NULL, NULL, NULL,
        '2025-03-01',
        'クラウド活用案件への参加に備えて、クラウド基盤の設定スキルを習得したい'
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user02'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        'QUALIFICATION',
        NULL,
        (SELECT id FROM qualifications WHERE name = '応用情報技術者試験'),
        NULL, NULL,
        '2025-10-01',
        '昇格要件として取得を目指す'
    );

-- ─── tl01 / 2024年度（COMPLETED） ────────────────────────────────────────────

INSERT INTO inventories (user_id, fiscal_year_id, status, submitted_at, goal_review_completed_at, goal_completed_at)
VALUES (
    (SELECT id FROM users WHERE user_id = 'tl01'),
    (SELECT id FROM fiscal_years WHERE name = '2024年度'),
    'COMPLETED',
    '2024-05-07 09:00:00+09',
    '2024-05-07 10:00:00+09',
    '2024-05-08 09:30:00+09'
);

-- レベル: 4=独力で実務に適用できる / 3=指導があれば実務で使える
INSERT INTO it_skill_details (inventory_id, it_skill_id, skill_level_id, remarks)
SELECT
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'tl01'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
    s.id,
    (SELECT id FROM skill_levels WHERE level_value = v.lv),
    v.rem
FROM (VALUES
    ('詳細設計書を作成することができる。',                                                  4, NULL),
    ('PL/SQLを作成、改修することができる。',                                                4, NULL),
    ('新しい技術やツールの調査、評価をすることができる。',                                  4, NULL),
    ('クラウドサービス（AWS、Azure、GCPなど）の設定をすることができる。',                   4, 'AWS インフラ設計・コスト最適化経験あり'),
    ('コーディング規約を作成することができる。',                                            4, 'チームのコーディング規約を策定'),
    ('セキュリティを考慮したコードを作成するように意識して実践している。',                   4, NULL),
    ('障害発生時の状況把握、原因究明、解決策の策定、復旧作業をすることができる。',          4, '本番障害対応の経験多数'),
    ('基本設計書を作成することができる。',                                                  4, NULL),
    ('要件定義書を作成することができる。',                                                  3, NULL),
    ('非機能要件（性能、可用性、セキュリティなど）を定義することができる。',                4, NULL),
    ('コードレビューを実施し、品質向上に貢献するように意識して実践している。',              4, 'チームのレビュー基準を策定'),
    ('設計書レビューを実施し、品質向上に貢献するように意識して実践している。',              4, NULL),
    ('WBS、ガントチャートなどを作成し、作業を管理することができる。',                       4, 'チームリーダーとして複数案件を管理'),
    ('工数、費用の見積もりをすることができる。',                                            4, NULL),
    ('メンバーのスキルや特性を把握し、作業指示をするように意識して実践している。',          4, NULL),
    ('メンバーの育成をすることができる。',                                                  4, 'OJT担当として後輩を指導'),
    ('作業の進捗について報告・連絡・相談をするように意識して実践している。',                4, NULL)
) AS v(skill_name, lv, rem)
JOIN it_skills s ON s.name = v.skill_name;

INSERT INTO qualification_details (inventory_id, qualification_id, acquired_year_month, remarks)
VALUES (
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'tl01'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
    (SELECT id FROM qualifications WHERE name = '応用情報技術者試験'),
    '2018-11-01', NULL
);

INSERT INTO seminar_details (inventory_id, ad_seminar_id, seminar_name, seminar_category_id, attended_year_month, remarks)
VALUES
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'tl01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        (SELECT id FROM ad_seminars WHERE name = '管理職のための部下育成シリーズ＜聴く力&話す力＞'),
        NULL, NULL, '2024-04-01', 'メンバーへの展開目的で受講'
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'tl01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        NULL, 'AWSコスト最適化勉強会',
        (SELECT id FROM seminar_categories WHERE name = '技術'),
        '2024-02-01', 'コスト削減施策の一環'
    );

INSERT INTO inventory_goals (inventory_id, goal_category, it_skill_id, qualification_id, ad_seminar_id, custom_name, target_period, reason)
VALUES
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'tl01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        'QUALIFICATION',
        NULL,
        (SELECT id FROM qualifications WHERE name = '情報処理安全確保支援士試験'),
        NULL, NULL,
        '2025-10-01',
        'セキュリティ要件の高い案件に備えて取得する'
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'tl01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        'IT_SKILL',
        (SELECT id FROM it_skills WHERE name = 'ステークホルダーを把握するように意識して実践している。'),
        NULL, NULL, NULL,
        '2025-03-01',
        'PM ロールへのステップアップに向け、プロジェクト全体の関係者管理を学ぶ'
    );

-- =============================================================================
-- 当年度（2025年度）棚卸データ
-- 2025年度は 03_master_data.sql で登録済み（is_active=TRUE）
-- =============================================================================

-- ─── user01 / 2025年度（COMPLETED） ─────────────────────────────────────────

INSERT INTO inventories (user_id, fiscal_year_id, status, submitted_at, goal_review_completed_at, goal_completed_at)
VALUES (
    (SELECT id FROM users WHERE user_id = 'user01'),
    (SELECT id FROM fiscal_years WHERE name = '2025年度'),
    'COMPLETED',
    '2025-05-09 10:00:00+09',
    '2025-05-10 11:00:00+09',
    '2025-05-12 15:00:00+09'
);

-- レベル: 4=独力で実務に適用できる / 3=指導があれば実務で使える
INSERT INTO it_skill_details (inventory_id, it_skill_id, skill_level_id, remarks)
SELECT
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'user01'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
    s.id,
    (SELECT id FROM skill_levels WHERE level_value = v.lv),
    v.rem
FROM (VALUES
    ('詳細設計書を読み、PGの作成、改修をすることができる。',                           4, '引き続き主力作業'),
    ('詳細設計書を作成することができる。',                                             3, '前年度目標を達成'),
    ('PL/SQLを作成、改修することができる。',                                           4, 'パフォーマンスチューニング経験あり'),
    ('SQLの基本的な操作をすることができる。',                                           4, NULL),
    ('特定のフレームワークを理解するように意識して実践している。',                     4, 'Spring Boot・React を習熟'),
    ('コーディング規約を理解し、遵守するように意識して実践している。',                 4, NULL),
    ('可読性、保守性、拡張性の高いコード、SQL等を作成するように意識して実践している。', 4, NULL),
    ('単体テスト仕様書をもとにテストを実施することができる。',                         4, NULL),
    ('結合テスト仕様書をもとにテストを実施することができる。',                         4, NULL),
    ('バージョン管理システム（Gitなど）を使用することができる。',                       4, NULL),
    ('基本設計書を作成することができる。',                                             3, 'SE ロールで初めて担当'),
    ('作業の進捗について報告・連絡・相談をするように意識して実践している。',           4, NULL)
) AS v(skill_name, lv, rem)
JOIN it_skills s ON s.name = v.skill_name;

INSERT INTO qualification_details (inventory_id, qualification_id, acquired_year_month, remarks)
VALUES
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
        (SELECT id FROM qualifications WHERE name = '基本情報技術者試験'),
        '2020-06-01', '在学中に取得'
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
        (SELECT id FROM qualifications WHERE name = 'AWS認定資格 Associate Solutions Architect'),
        '2023-08-01', 'SAA-C03で合格'
    );

INSERT INTO seminar_details (inventory_id, ad_seminar_id, seminar_name, seminar_category_id, attended_year_month, remarks)
VALUES
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
        (SELECT id FROM ad_seminars WHERE name = '仕事の進捗管理入門'),
        NULL, NULL, '2025-05-01', NULL
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
        NULL, 'TypeScript 勉強会',
        (SELECT id FROM seminar_categories WHERE name = '技術'),
        '2025-03-01', '社内有志勉強会'
    );

INSERT INTO inventory_goals (inventory_id, goal_category, it_skill_id, qualification_id, ad_seminar_id, custom_name, target_period, reason)
VALUES
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
        'IT_SKILL',
        (SELECT id FROM it_skills WHERE name = '要件定義書を作成することができる。'),
        NULL, NULL, NULL,
        '2026-03-01',
        'SE ロールへの完全移行に向け、上流工程の経験をさらに積みたい'
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
        'QUALIFICATION',
        NULL,
        (SELECT id FROM qualifications WHERE name = '応用情報技術者試験'),
        NULL, NULL,
        '2026-10-01',
        '昇格要件として次年度内での取得を目指す'
    );

-- ─── user02 / 2025年度（COMPLETED） ─────────────────────────────────────────

INSERT INTO inventories (user_id, fiscal_year_id, status, submitted_at, goal_review_completed_at, goal_completed_at)
VALUES (
    (SELECT id FROM users WHERE user_id = 'user02'),
    (SELECT id FROM fiscal_years WHERE name = '2025年度'),
    'COMPLETED',
    '2025-05-14 13:00:00+09',
    '2025-05-15 10:00:00+09',
    '2025-05-16 17:00:00+09'
);

-- レベル: 4=独力で実務に適用できる / 3=指導があれば実務で使える
INSERT INTO it_skill_details (inventory_id, it_skill_id, skill_level_id, remarks)
SELECT
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'user02'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
    s.id,
    (SELECT id FROM skill_levels WHERE level_value = v.lv),
    v.rem
FROM (VALUES
    ('詳細設計書を読み、PGの作成、改修をすることができる。',                          4, '独力で担当できるようになった'),
    ('SQLの基本的な操作をすることができる。',                                          3, NULL),
    ('特定のフレームワークを理解するように意識して実践している。',                    3, NULL),
    ('コーディング規約を理解し、遵守するように意識して実践している。',                3, NULL),
    ('詳細設計書をもとに単体テスト仕様書を作成することができる。',                    3, '前年度より向上'),
    ('単体テスト仕様書をもとにテストを実施することができる。',                        4, NULL),
    ('結合テスト仕様書をもとにテストを実施することができる。',                        3, NULL),
    ('バージョン管理システム（Gitなど）を使用することができる。',                      4, NULL),
    ('クラウドサービス（AWS、Azure、GCPなど）の設定をすることができる。',              3, '前年度目標を達成、AWS の基本設定が可能'),
    ('作業の進捗について報告・連絡・相談をするように意識して実践している。',          4, NULL)
) AS v(skill_name, lv, rem)
JOIN it_skills s ON s.name = v.skill_name;

INSERT INTO qualification_details (inventory_id, qualification_id, acquired_year_month, remarks)
VALUES (
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'user02'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
    (SELECT id FROM qualifications WHERE name = '基本情報技術者試験'),
    '2022-05-01', NULL
);

INSERT INTO seminar_details (inventory_id, ad_seminar_id, seminar_name, seminar_category_id, attended_year_month, remarks)
VALUES (
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'user02'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
    (SELECT id FROM ad_seminars WHERE name = 'ビジネスマナー研修2＜ビジネスを円滑に進めるための形式＞'),
    NULL, NULL, '2025-06-01', NULL
);

INSERT INTO inventory_goals (inventory_id, goal_category, it_skill_id, qualification_id, ad_seminar_id, custom_name, target_period, reason)
VALUES
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user02'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
        'QUALIFICATION',
        NULL,
        (SELECT id FROM qualifications WHERE name = '応用情報技術者試験'),
        NULL, NULL,
        '2026-10-01',
        '昇格要件として秋期試験での取得を目指す'
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user02'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
        'IT_SKILL',
        (SELECT id FROM it_skills WHERE name = 'PL/SQLを作成、改修することができる。'),
        NULL, NULL, NULL,
        '2026-03-01',
        'データベース周りのスキルを強化して担当できる範囲を広げたい'
    );

-- ─── tl01 / 2025年度（COMPLETED） ────────────────────────────────────────────

INSERT INTO inventories (user_id, fiscal_year_id, status, submitted_at, goal_review_completed_at, goal_completed_at)
VALUES (
    (SELECT id FROM users WHERE user_id = 'tl01'),
    (SELECT id FROM fiscal_years WHERE name = '2025年度'),
    'COMPLETED',
    '2025-05-07 09:00:00+09',
    '2025-05-07 10:00:00+09',
    '2025-05-08 09:30:00+09'
);

-- レベル: 4=独力で実務に適用できる / 3=指導があれば実務で使える
INSERT INTO it_skill_details (inventory_id, it_skill_id, skill_level_id, remarks)
SELECT
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'tl01'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
    s.id,
    (SELECT id FROM skill_levels WHERE level_value = v.lv),
    v.rem
FROM (VALUES
    ('詳細設計書を作成することができる。',                                                  4, NULL),
    ('PL/SQLを作成、改修することができる。',                                                4, NULL),
    ('新しい技術やツールの調査、評価をすることができる。',                                  4, NULL),
    ('クラウドサービス（AWS、Azure、GCPなど）の設定をすることができる。',                   4, 'AWS Well-Architected Framework の活用'),
    ('コーディング規約を作成することができる。',                                            4, NULL),
    ('セキュリティを考慮したコードを作成するように意識して実践している。',                   4, NULL),
    ('障害発生時の状況把握、原因究明、解決策の策定、復旧作業をすることができる。',          4, NULL),
    ('基本設計書を作成することができる。',                                                  4, NULL),
    ('要件定義書を作成することができる。',                                                  4, '前年度より向上'),
    ('非機能要件（性能、可用性、セキュリティなど）を定義することができる。',                4, NULL),
    ('コードレビューを実施し、品質向上に貢献するように意識して実践している。',              4, NULL),
    ('設計書レビューを実施し、品質向上に貢献するように意識して実践している。',              4, NULL),
    ('WBS、ガントチャートなどを作成し、作業を管理することができる。',                       4, NULL),
    ('工数、費用の見積もりをすることができる。',                                            4, NULL),
    ('メンバーのスキルや特性を把握し、作業指示をするように意識して実践している。',          4, NULL),
    ('メンバーの育成をすることができる。',                                                  4, NULL),
    ('ステークホルダーを把握するように意識して実践している。',                              3, '前年度目標を達成、引き続き実践中'),
    ('作業の進捗について報告・連絡・相談をするように意識して実践している。',                4, NULL)
) AS v(skill_name, lv, rem)
JOIN it_skills s ON s.name = v.skill_name;

INSERT INTO qualification_details (inventory_id, qualification_id, acquired_year_month, remarks)
VALUES (
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'tl01'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
    (SELECT id FROM qualifications WHERE name = '応用情報技術者試験'),
    '2018-11-01', NULL
);

INSERT INTO seminar_details (inventory_id, ad_seminar_id, seminar_name, seminar_category_id, attended_year_month, remarks)
VALUES
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'tl01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
        (SELECT id FROM ad_seminars WHERE name = 'プロジェクトマネジメントの全体像'),
        NULL, NULL, '2025-05-01', 'PM ロール移行の準備として受講'
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'tl01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
        NULL, 'AWS Well-Architected Framework 勉強会',
        (SELECT id FROM seminar_categories WHERE name = '技術'),
        '2025-02-01', NULL
    );

INSERT INTO inventory_goals (inventory_id, goal_category, it_skill_id, qualification_id, ad_seminar_id, custom_name, target_period, reason)
VALUES
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'tl01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
        'QUALIFICATION',
        NULL,
        (SELECT id FROM qualifications WHERE name = 'PMP'),
        NULL, NULL,
        '2026-10-01',
        'PM ロールへのステップアップに向け、国際資格を取得する'
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'tl01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2025年度')),
        'IT_SKILL',
        (SELECT id FROM it_skills WHERE name = 'リスクの特定、分析、管理をすることができる。'),
        NULL, NULL, NULL,
        '2026-03-01',
        'プロジェクトマネージャーとして必要なリスク管理スキルを習得する'
    );

-- =============================================================================
-- 次年度（2026年度）年度マスタ
-- 入力期間: 2026-04-01 〜 2027-03-31
-- =============================================================================
INSERT INTO fiscal_years (name, start_date, end_date, input_start_date, input_end_date, is_active)
VALUES ('2026年度', '2026-04-01', '2027-03-31', '2026-04-01', '2027-03-31', TRUE);

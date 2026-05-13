-- =============================================================================
-- V4: テストデータ：前年度（2024年度）棚卸データ
-- =============================================================================

INSERT INTO fiscal_years (name, start_date, end_date, input_start_date, input_end_date, is_active)
VALUES ('2024年度', '2024-04-01', '2025-03-31', '2024-04-01', '2024-06-30', FALSE);

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

INSERT INTO it_skill_details (inventory_id, it_skill_id, skill_level_id, remarks)
SELECT
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'user01'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
    s.id,
    (SELECT id FROM skill_levels WHERE level_value = v.lv),
    v.rem
FROM (VALUES
    ('Linux',                     3, 'Ubuntuサーバー運用経験あり'),
    ('Windows Server',            2, '基本的な設定は可能'),
    ('TCP/IP・ネットワーク基礎',  3, NULL),
    ('ルーター / スイッチ設定',   1, NULL),
    ('AWS',                       3, 'EC2・RDS・S3の基本操作'),
    ('Azure',                     1, NULL),
    ('HTML / CSS',                3, NULL),
    ('JavaScript',                3, 'ES6以降の基礎知識あり'),
    ('React',                     3, '実装経験あり、さらなる習熟が必要'),
    ('Java',                      4, '実務での主力言語'),
    ('Python',                    2, '研修で学習済み'),
    ('Spring Boot',               4, 'REST API開発経験あり'),
    ('SQL基礎',                   4, 'JOIN・サブクエリなど複雑なクエリも可'),
    ('PostgreSQL',                3, 'インデックス設計・チューニング経験あり'),
    ('MySQL',                     2, NULL),
    ('WBS / スケジュール管理',    2, '小規模案件のみ'),
    ('リスク管理',                2, NULL),
    ('アジャイル / スクラム',     2, 'スクラム開発経験あり')
) AS v(skill_name, lv, rem)
JOIN it_skills s ON s.name = v.skill_name;

INSERT INTO qualification_details (inventory_id, qualification_id, acquired_year_month, remarks)
VALUES
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        (SELECT id FROM qualifications WHERE name = '基本情報技術者（FE）'),
        '2020-06-01', '在学中に取得'
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        (SELECT id FROM qualifications WHERE name = 'AWS認定ソリューションアーキテクト - アソシエイト'),
        '2023-08-01', 'SAA-C03で合格'
    );

INSERT INTO seminar_details (inventory_id, ad_seminar_id, seminar_name, seminar_category_id, attended_year_month, remarks)
VALUES
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        (SELECT id FROM ad_seminars WHERE name = 'クラウド入門'),
        NULL, NULL, '2024-04-01', NULL
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        (SELECT id FROM ad_seminars WHERE name = 'セキュリティ基礎'),
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
        (SELECT id FROM it_skills WHERE name = 'React'),
        NULL, NULL, NULL,
        '2025-03-01',
        'フロントエンド開発の主担当になるため、React をより深く習得したい'
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        'QUALIFICATION',
        NULL,
        (SELECT id FROM qualifications WHERE name = '応用情報技術者（AP）'),
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
        (SELECT id FROM ad_seminars WHERE name = 'Java中級'),
        NULL,
        '2025-02-01',
        'Java のスキルをさらに向上させ、チーム内で指導できるレベルを目指す'
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

INSERT INTO it_skill_details (inventory_id, it_skill_id, skill_level_id, remarks)
SELECT
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'user02'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
    s.id,
    (SELECT id FROM skill_levels WHERE level_value = v.lv),
    v.rem
FROM (VALUES
    ('Linux',                     2, NULL),
    ('Windows Server',            3, '運用担当として経験あり'),
    ('TCP/IP・ネットワーク基礎',  2, NULL),
    ('ルーター / スイッチ設定',   1, NULL),
    ('AWS',                       2, NULL),
    ('Azure',                     3, 'Azure AD・App Service を業務で利用'),
    ('HTML / CSS',                2, NULL),
    ('JavaScript',                2, NULL),
    ('React',                     1, NULL),
    ('Java',                      3, NULL),
    ('Python',                    3, 'データ分析業務で使用'),
    ('Spring Boot',               2, NULL),
    ('SQL基礎',                   3, NULL),
    ('PostgreSQL',                2, NULL),
    ('MySQL',                     2, NULL),
    ('WBS / スケジュール管理',    3, 'サブリーダーとして経験あり'),
    ('リスク管理',                2, NULL),
    ('アジャイル / スクラム',     1, NULL)
) AS v(skill_name, lv, rem)
JOIN it_skills s ON s.name = v.skill_name;

INSERT INTO qualification_details (inventory_id, qualification_id, acquired_year_month, remarks)
VALUES (
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'user02'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
    (SELECT id FROM qualifications WHERE name = '基本情報技術者（FE）'),
    '2022-05-01', NULL
);

INSERT INTO seminar_details (inventory_id, ad_seminar_id, seminar_name, seminar_category_id, attended_year_month, remarks)
VALUES (
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'user02'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
    (SELECT id FROM ad_seminars WHERE name = 'プロジェクト管理基礎'),
    NULL, NULL, '2024-04-01', NULL
);

INSERT INTO inventory_goals (inventory_id, goal_category, it_skill_id, qualification_id, ad_seminar_id, custom_name, target_period, reason)
VALUES
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user02'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        'IT_SKILL',
        (SELECT id FROM it_skills WHERE name = 'AWS'),
        NULL, NULL, NULL,
        '2025-03-01',
        'クラウド移行プロジェクトに備え、AWS の実践スキルを高める'
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'user02'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        'QUALIFICATION',
        NULL,
        (SELECT id FROM qualifications WHERE name = '応用情報技術者（AP）'),
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

INSERT INTO it_skill_details (inventory_id, it_skill_id, skill_level_id, remarks)
SELECT
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'tl01'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
    s.id,
    (SELECT id FROM skill_levels WHERE level_value = v.lv),
    v.rem
FROM (VALUES
    ('Linux',                     4, 'サーバー設計・構築経験あり'),
    ('Windows Server',            3, NULL),
    ('TCP/IP・ネットワーク基礎',  4, NULL),
    ('ルーター / スイッチ設定',   2, NULL),
    ('AWS',                       4, 'インフラ設計・コスト最適化経験あり'),
    ('Azure',                     2, NULL),
    ('HTML / CSS',                3, NULL),
    ('JavaScript',                4, NULL),
    ('React',                     4, 'フロントエンド設計担当'),
    ('Java',                      5, 'チーム内で技術指導を担当'),
    ('Python',                    3, NULL),
    ('Spring Boot',               5, 'アーキテクチャ設計・コードレビュー担当'),
    ('SQL基礎',                   5, NULL),
    ('PostgreSQL',                4, 'パフォーマンスチューニング経験あり'),
    ('MySQL',                     3, NULL),
    ('WBS / スケジュール管理',    4, 'チームリーダーとして複数案件を管理'),
    ('リスク管理',                4, NULL),
    ('アジャイル / スクラム',     4, 'スクラムマスター経験あり')
) AS v(skill_name, lv, rem)
JOIN it_skills s ON s.name = v.skill_name;

INSERT INTO qualification_details (inventory_id, qualification_id, acquired_year_month, remarks)
VALUES (
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'tl01'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
    (SELECT id FROM qualifications WHERE name = '応用情報技術者（AP）'),
    '2018-11-01', NULL
);

INSERT INTO seminar_details (inventory_id, ad_seminar_id, seminar_name, seminar_category_id, attended_year_month, remarks)
VALUES
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'tl01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        (SELECT id FROM ad_seminars WHERE name = 'Java中級'),
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
        (SELECT id FROM qualifications WHERE name = '情報処理安全確保支援士（SC）'),
        NULL, NULL,
        '2025-10-01',
        'セキュリティ要件の高い案件に備えて取得する'
    ),
    (
        (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
         WHERE u.user_id = 'tl01'
           AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
        'IT_SKILL',
        (SELECT id FROM it_skills WHERE name = 'アジャイル / スクラム'),
        NULL, NULL, NULL,
        '2025-03-01',
        'チーム全体のスクラム導入を推進するため、実践知識を深める'
    );

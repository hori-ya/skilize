-- =============================================================================
-- Skilize DB 初期化スクリプト（ローカル開発用）
-- PostgreSQL 16.4
-- =============================================================================

-- -----------------------------------------------------------------------------
-- テーブル削除（依存関係の逆順）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS inventory_goals        CASCADE;
DROP TABLE IF EXISTS seminar_details        CASCADE;
DROP TABLE IF EXISTS qualification_details  CASCADE;
DROP TABLE IF EXISTS it_skill_details       CASCADE;
DROP TABLE IF EXISTS inventories            CASCADE;
DROP TABLE IF EXISTS users                  CASCADE;
DROP TABLE IF EXISTS ad_seminars            CASCADE;
DROP TABLE IF EXISTS qualifications         CASCADE;
DROP TABLE IF EXISTS it_skills              CASCADE;
DROP TABLE IF EXISTS it_skill_categories    CASCADE;
DROP TABLE IF EXISTS seminar_categories     CASCADE;
DROP TABLE IF EXISTS ad_seminar_categories  CASCADE;
DROP TABLE IF EXISTS qualification_categories CASCADE;
DROP TABLE IF EXISTS skill_levels           CASCADE;
DROP TABLE IF EXISTS fiscal_years           CASCADE;
DROP TABLE IF EXISTS fiscal_year_settings   CASCADE;

DROP FUNCTION IF EXISTS update_updated_at();

-- =============================================================================
-- 共通トリガー関数: updated_at 自動更新
-- =============================================================================
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- テーブル作成
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. fiscal_year_settings（年度設定 / シングルトン）
-- -----------------------------------------------------------------------------
CREATE TABLE fiscal_year_settings (
    id                       SMALLINT    NOT NULL DEFAULT 1,
    fiscal_year_start_month  SMALLINT    NOT NULL DEFAULT 4,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by               INTEGER,    -- FK → users(id)  ※後で ALTER TABLE で追加

    CONSTRAINT pk_fiscal_year_settings PRIMARY KEY (id),
    CONSTRAINT chk_fiscal_year_settings_month
        CHECK (fiscal_year_start_month BETWEEN 1 AND 12),
    CONSTRAINT chk_fiscal_year_settings_singleton
        CHECK (id = 1)
);

COMMENT ON TABLE  fiscal_year_settings IS '年度設定（シングルトン）';
COMMENT ON COLUMN fiscal_year_settings.fiscal_year_start_month IS '会計年度開始月（1〜12）';

-- -----------------------------------------------------------------------------
-- 2. fiscal_years（年度マスタ）
-- -----------------------------------------------------------------------------
CREATE TABLE fiscal_years (
    id               SERIAL      NOT NULL,
    name             VARCHAR(20) NOT NULL,
    start_date       DATE        NOT NULL,
    end_date         DATE        NOT NULL,
    input_start_date DATE,
    input_end_date   DATE,
    is_active        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_fiscal_years PRIMARY KEY (id),
    CONSTRAINT uq_fiscal_years_name UNIQUE (name),
    CONSTRAINT chk_fiscal_years_dates CHECK (start_date < end_date)
);

COMMENT ON TABLE fiscal_years IS '年度マスタ';

CREATE TRIGGER trg_fiscal_years_updated_at
    BEFORE UPDATE ON fiscal_years
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 3. skill_levels（レベルマスタ）
-- -----------------------------------------------------------------------------
CREATE TABLE skill_levels (
    id          SERIAL       NOT NULL,
    level_value SMALLINT     NOT NULL,
    description VARCHAR(200) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_skill_levels PRIMARY KEY (id),
    CONSTRAINT uq_skill_levels_value UNIQUE (level_value)
);

COMMENT ON TABLE  skill_levels IS 'レベルマスタ（スキル採点定義）';
COMMENT ON COLUMN skill_levels.level_value IS '採点数値（棚卸データへの保存値）';

CREATE TRIGGER trg_skill_levels_updated_at
    BEFORE UPDATE ON skill_levels
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 4. it_skill_categories（ITスキル分類マスタ / 自己参照・最大3階層）
-- -----------------------------------------------------------------------------
CREATE TABLE it_skill_categories (
    id         SERIAL       NOT NULL,
    parent_id  INTEGER,     -- FK → it_skill_categories(id)。NULL が分類1（ルート）
    level      SMALLINT     NOT NULL,
    name       VARCHAR(100) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_it_skill_categories PRIMARY KEY (id),
    CONSTRAINT fk_it_skill_categories_parent
        FOREIGN KEY (parent_id) REFERENCES it_skill_categories(id),
    CONSTRAINT chk_it_skill_categories_level CHECK (level IN (1, 2, 3))
);

COMMENT ON TABLE  it_skill_categories IS 'ITスキル分類マスタ（最大3階層・自己参照）';
COMMENT ON COLUMN it_skill_categories.parent_id IS 'NULLが分類1（ルート）';

CREATE INDEX idx_it_skill_categories_parent ON it_skill_categories(parent_id);

CREATE TRIGGER trg_it_skill_categories_updated_at
    BEFORE UPDATE ON it_skill_categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 5. qualification_categories（資格分類マスタ / フラット1階層）
-- -----------------------------------------------------------------------------
CREATE TABLE qualification_categories (
    id         SERIAL       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_qualification_categories PRIMARY KEY (id),
    CONSTRAINT uq_qualification_categories_name UNIQUE (name)
);

COMMENT ON TABLE qualification_categories IS '資格分類マスタ（フラット1階層）';

CREATE TRIGGER trg_qualification_categories_updated_at
    BEFORE UPDATE ON qualification_categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 6. ad_seminar_categories（ADセミナー分類マスタ / フラット1階層）
-- -----------------------------------------------------------------------------
CREATE TABLE ad_seminar_categories (
    id         SERIAL       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_ad_seminar_categories PRIMARY KEY (id),
    CONSTRAINT uq_ad_seminar_categories_name UNIQUE (name)
);

COMMENT ON TABLE ad_seminar_categories IS 'ADセミナー分類マスタ（フラット1階層）';

CREATE TRIGGER trg_ad_seminar_categories_updated_at
    BEFORE UPDATE ON ad_seminar_categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 7. seminar_categories（セミナー分類マスタ / フラット1階層 / AD以外のセミナー用）
-- -----------------------------------------------------------------------------
CREATE TABLE seminar_categories (
    id         SERIAL       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_seminar_categories PRIMARY KEY (id),
    CONSTRAINT uq_seminar_categories_name UNIQUE (name)
);

COMMENT ON TABLE seminar_categories IS 'セミナー分類マスタ（フラット1階層。AD以外のセミナーに適用）';

CREATE TRIGGER trg_seminar_categories_updated_at
    BEFORE UPDATE ON seminar_categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 8. it_skills（ITスキルマスタ）
-- -----------------------------------------------------------------------------
CREATE TABLE it_skills (
    id          SERIAL       NOT NULL,
    category_id INTEGER      NOT NULL,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_it_skills PRIMARY KEY (id),
    CONSTRAINT fk_it_skills_category
        FOREIGN KEY (category_id) REFERENCES it_skill_categories(id)
);

COMMENT ON TABLE it_skills IS 'ITスキルマスタ';

CREATE INDEX idx_it_skills_category  ON it_skills(category_id);
CREATE INDEX idx_it_skills_is_active ON it_skills(is_active);

CREATE TRIGGER trg_it_skills_updated_at
    BEFORE UPDATE ON it_skills
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 9. qualifications（参考資格マスタ）
-- -----------------------------------------------------------------------------
CREATE TABLE qualifications (
    id          SERIAL       NOT NULL,
    category_id INTEGER,     -- FK → qualification_categories(id)。NULL は未分類
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_qualifications PRIMARY KEY (id),
    CONSTRAINT fk_qualifications_category
        FOREIGN KEY (category_id) REFERENCES qualification_categories(id)
);

COMMENT ON TABLE  qualifications IS '参考資格マスタ';
COMMENT ON COLUMN qualifications.category_id IS 'NULL は未分類';

CREATE INDEX idx_qualifications_category  ON qualifications(category_id);
CREATE INDEX idx_qualifications_is_active ON qualifications(is_active);

CREATE TRIGGER trg_qualifications_updated_at
    BEFORE UPDATE ON qualifications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 10. ad_seminars（ADマスタ）
-- -----------------------------------------------------------------------------
CREATE TABLE ad_seminars (
    id          SERIAL       NOT NULL,
    category_id INTEGER,     -- FK → ad_seminar_categories(id)。NULL は未分類
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_ad_seminars PRIMARY KEY (id),
    CONSTRAINT fk_ad_seminars_category
        FOREIGN KEY (category_id) REFERENCES ad_seminar_categories(id)
);

COMMENT ON TABLE  ad_seminars IS 'ADマスタ';
COMMENT ON COLUMN ad_seminars.category_id IS 'NULL は未分類';

CREATE INDEX idx_ad_seminars_category ON ad_seminars(category_id);

CREATE TRIGGER trg_ad_seminars_updated_at
    BEFORE UPDATE ON ad_seminars
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 11. users（ユーザー / TLへの自己参照FK）
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id                  SERIAL       NOT NULL,
    user_id             VARCHAR(50)  NOT NULL,  -- ログインID（変更不可）
    name                VARCHAR(100) NOT NULL,
    email               VARCHAR(255),           -- 任意。NULL 許容
    password_hash       VARCHAR(255) NOT NULL,
    role                VARCHAR(10)  NOT NULL,
    tl_user_id          INTEGER,     -- FK → users(id)。TL または ADMIN ロールのユーザーを指定
    is_initial_password BOOLEAN      NOT NULL DEFAULT TRUE,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_user_id UNIQUE (user_id),
    CONSTRAINT fk_users_tl FOREIGN KEY (tl_user_id) REFERENCES users(id),
    CONSTRAINT chk_users_role CHECK (role IN ('GENERAL', 'TL', 'ADMIN'))
);

COMMENT ON TABLE  users IS 'ユーザー';
COMMENT ON COLUMN users.user_id IS 'ログインID（一意・変更不可）';
COMMENT ON COLUMN users.email IS 'メールアドレス（任意）';
COMMENT ON COLUMN users.tl_user_id IS 'TLユーザーへの自己参照FK（TL または ADMIN ロールを指定）';
COMMENT ON COLUMN users.password_hash IS 'BCryptハッシュ（コストファクター: 12）';

CREATE INDEX idx_users_tl_user_id ON users(tl_user_id);
CREATE INDEX idx_users_role       ON users(role);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- fiscal_year_settings.updated_by の FK を users 作成後に追加
ALTER TABLE fiscal_year_settings
    ADD CONSTRAINT fk_fiscal_year_settings_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(id);

-- -----------------------------------------------------------------------------
-- 12. inventories（棚卸ヘッダー）
-- -----------------------------------------------------------------------------
CREATE TABLE inventories (
    id                        SERIAL      NOT NULL,
    user_id                   INTEGER     NOT NULL,
    fiscal_year_id            INTEGER     NOT NULL,
    status                    VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at              TIMESTAMPTZ,
    goal_review_completed_at  TIMESTAMPTZ,  -- 前回目標振り返り完了日時（NULL かつ前年度目標あり → ログイン時に SCR-019 へ誘導）
    goal_completed_at         TIMESTAMPTZ,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_inventories PRIMARY KEY (id),
    CONSTRAINT uq_inventories_user_year UNIQUE (user_id, fiscal_year_id),
    CONSTRAINT fk_inventories_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_inventories_fiscal_year
        FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_years(id),
    CONSTRAINT chk_inventories_status
        CHECK (status IN ('DRAFT', 'PENDING_GOAL', 'COMPLETED'))
);

COMMENT ON TABLE  inventories IS '棚卸ヘッダー（ユーザー×年度で1件）';
COMMENT ON COLUMN inventories.goal_review_completed_at IS '前回目標振り返り完了日時。NULL かつ前年度目標あり → ログイン時に SCR-019 へ誘導';

CREATE INDEX idx_inventories_user_id        ON inventories(user_id);
CREATE INDEX idx_inventories_fiscal_year_id ON inventories(fiscal_year_id);
CREATE INDEX idx_inventories_status         ON inventories(status);

CREATE TRIGGER trg_inventories_updated_at
    BEFORE UPDATE ON inventories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 13. it_skill_details（ITスキル棚卸明細）
-- -----------------------------------------------------------------------------
CREATE TABLE it_skill_details (
    id                SERIAL       NOT NULL,
    inventory_id      INTEGER      NOT NULL,
    it_skill_id       INTEGER,     -- NULL はカスタムスキル
    custom_skill_name VARCHAR(200),
    skill_level_id    INTEGER      NOT NULL,
    remarks           TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_it_skill_details PRIMARY KEY (id),
    CONSTRAINT fk_it_skill_details_inventory
        FOREIGN KEY (inventory_id) REFERENCES inventories(id),
    CONSTRAINT fk_it_skill_details_skill
        FOREIGN KEY (it_skill_id) REFERENCES it_skills(id),
    CONSTRAINT fk_it_skill_details_level
        FOREIGN KEY (skill_level_id) REFERENCES skill_levels(id),
    CONSTRAINT chk_it_skill_details_skill_ref
        CHECK (it_skill_id IS NOT NULL OR custom_skill_name IS NOT NULL)
);

COMMENT ON TABLE  it_skill_details IS 'ITスキル棚卸明細';
COMMENT ON COLUMN it_skill_details.it_skill_id IS 'NULL はカスタムスキル（custom_skill_name を使用）';

CREATE INDEX idx_it_skill_details_inventory ON it_skill_details(inventory_id);
CREATE INDEX idx_it_skill_details_skill     ON it_skill_details(it_skill_id);

CREATE TRIGGER trg_it_skill_details_updated_at
    BEFORE UPDATE ON it_skill_details
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 14. qualification_details（資格棚卸明細）
-- -----------------------------------------------------------------------------
CREATE TABLE qualification_details (
    id                        SERIAL       NOT NULL,
    inventory_id              INTEGER      NOT NULL,
    qualification_id          INTEGER,     -- NULL はカスタム資格
    custom_qualification_name VARCHAR(200),
    acquired_year_month       DATE,        -- 月初日で保存。未取得は NULL
    remarks                   TEXT,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_qualification_details PRIMARY KEY (id),
    CONSTRAINT fk_qualification_details_inventory
        FOREIGN KEY (inventory_id) REFERENCES inventories(id),
    CONSTRAINT fk_qualification_details_qualification
        FOREIGN KEY (qualification_id) REFERENCES qualifications(id),
    CONSTRAINT chk_qualification_details_ref
        CHECK (qualification_id IS NOT NULL OR custom_qualification_name IS NOT NULL)
);

COMMENT ON TABLE  qualification_details IS '資格棚卸明細';
COMMENT ON COLUMN qualification_details.acquired_year_month IS '月初日で保存（例: 2025-04-01）';

CREATE INDEX idx_qualification_details_inventory     ON qualification_details(inventory_id);
CREATE INDEX idx_qualification_details_qualification ON qualification_details(qualification_id);

CREATE TRIGGER trg_qualification_details_updated_at
    BEFORE UPDATE ON qualification_details
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 15. seminar_details（セミナー棚卸明細）
-- -----------------------------------------------------------------------------
CREATE TABLE seminar_details (
    id                   SERIAL       NOT NULL,
    inventory_id         INTEGER      NOT NULL,
    ad_seminar_id        INTEGER,     -- NULL はAD以外のセミナー
    seminar_name         VARCHAR(200),
    seminar_category_id  INTEGER,     -- FK → seminar_categories(id)。AD以外のセミナー時のみ使用
    attended_year_month  DATE,        -- 月初日で保存。未受講は NULL
    remarks              TEXT,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_seminar_details PRIMARY KEY (id),
    CONSTRAINT fk_seminar_details_inventory
        FOREIGN KEY (inventory_id) REFERENCES inventories(id),
    CONSTRAINT fk_seminar_details_ad_seminar
        FOREIGN KEY (ad_seminar_id) REFERENCES ad_seminars(id),
    CONSTRAINT fk_seminar_details_seminar_category
        FOREIGN KEY (seminar_category_id) REFERENCES seminar_categories(id),
    CONSTRAINT chk_seminar_details_ref
        CHECK (ad_seminar_id IS NOT NULL OR seminar_name IS NOT NULL)
);

COMMENT ON TABLE  seminar_details IS 'セミナー棚卸明細';
COMMENT ON COLUMN seminar_details.ad_seminar_id IS 'NULL はAD以外のセミナー（seminar_name を使用）';
COMMENT ON COLUMN seminar_details.seminar_category_id IS 'AD以外のセミナー時のみ設定。ADセミナーの分類は ad_seminars.category_id で管理';

CREATE INDEX idx_seminar_details_inventory ON seminar_details(inventory_id);

CREATE TRIGGER trg_seminar_details_updated_at
    BEFORE UPDATE ON seminar_details
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 16. inventory_goals（目標設定）
-- -----------------------------------------------------------------------------
CREATE TABLE inventory_goals (
    id                 SERIAL       NOT NULL,
    inventory_id       INTEGER      NOT NULL,
    goal_category      VARCHAR(20)  NOT NULL,
    it_skill_id        INTEGER,
    qualification_id   INTEGER,
    ad_seminar_id      INTEGER,
    custom_name        VARCHAR(200),
    target_period      DATE         NOT NULL,  -- 月初日で保存
    reason             TEXT,
    achievement_status VARCHAR(20),  -- 翌年度の振り返り時に記録。NULL は未振り返り
    review_note        TEXT,         -- 翌年度の振り返りコメント
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_inventory_goals PRIMARY KEY (id),
    CONSTRAINT fk_inventory_goals_inventory
        FOREIGN KEY (inventory_id) REFERENCES inventories(id),
    CONSTRAINT fk_inventory_goals_it_skill
        FOREIGN KEY (it_skill_id) REFERENCES it_skills(id),
    CONSTRAINT fk_inventory_goals_qualification
        FOREIGN KEY (qualification_id) REFERENCES qualifications(id),
    CONSTRAINT fk_inventory_goals_ad_seminar
        FOREIGN KEY (ad_seminar_id) REFERENCES ad_seminars(id),
    CONSTRAINT chk_inventory_goals_category
        CHECK (goal_category IN ('IT_SKILL', 'QUALIFICATION', 'AD')),
    CONSTRAINT chk_inventory_goals_target
        CHECK (
            it_skill_id      IS NOT NULL OR
            qualification_id IS NOT NULL OR
            ad_seminar_id    IS NOT NULL OR
            custom_name      IS NOT NULL
        ),
    CONSTRAINT chk_inventory_goals_achievement_status
        CHECK (achievement_status IN ('ACHIEVED', 'PARTIAL', 'NOT_ACHIEVED'))
);

COMMENT ON TABLE  inventory_goals IS '目標設定';
COMMENT ON COLUMN inventory_goals.goal_category IS 'IT_SKILL / QUALIFICATION / AD';
COMMENT ON COLUMN inventory_goals.target_period IS '月初日で保存（例: 2026-03-01）';
COMMENT ON COLUMN inventory_goals.achievement_status IS '達成状況。翌年度の振り返り時に記録（ACHIEVED / PARTIAL / NOT_ACHIEVED）。NULL は未振り返り';
COMMENT ON COLUMN inventory_goals.review_note IS '振り返りコメント。翌年度の振り返り時に記録';

CREATE INDEX idx_inventory_goals_inventory ON inventory_goals(inventory_id);

CREATE TRIGGER trg_inventory_goals_updated_at
    BEFORE UPDATE ON inventory_goals
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- =============================================================================
-- 初期データ
-- =============================================================================

-- 年度設定（シングルトン）
INSERT INTO fiscal_year_settings (id, fiscal_year_start_month)
VALUES (1, 4)
ON CONFLICT (id) DO NOTHING;

-- レベルマスタ
INSERT INTO skill_levels (level_value, description) VALUES
    (1, '知識なし／未経験'),
    (2, '概念を理解している'),
    (3, '指導があれば実務で使える'),
    (4, '独力で実務に適用できる'),
    (5, '他者に指導・展開できる');

-- 年度マスタ（テスト用 2025年度）
INSERT INTO fiscal_years (name, start_date, end_date, input_start_date, input_end_date, is_active)
VALUES ('2025年度', '2025-04-01', '2026-03-31', '2025-04-01', '2025-06-30', TRUE);

-- =============================================================================
-- ITスキル分類（Lv1 / Lv2）
-- =============================================================================
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    (NULL, 1, 'プログラミング', 1),
    (NULL, 1, 'インフラ',       2),
    (NULL, 1, 'アプリケーション', 3),
    (NULL, 1, 'マネジメント',   4);

INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'プログラミング' AND level = 1),   2, 'Java',              1),
    ((SELECT id FROM it_skill_categories WHERE name = 'プログラミング' AND level = 1),   2, 'Python',            2),
    ((SELECT id FROM it_skill_categories WHERE name = 'プログラミング' AND level = 1),   2, 'C#',               3),
    ((SELECT id FROM it_skill_categories WHERE name = 'インフラ' AND level = 1),         2, 'OS / サーバー',     1),
    ((SELECT id FROM it_skill_categories WHERE name = 'インフラ' AND level = 1),         2, 'ネットワーク',      2),
    ((SELECT id FROM it_skill_categories WHERE name = 'インフラ' AND level = 1),         2, 'クラウド',          3),
    ((SELECT id FROM it_skill_categories WHERE name = 'アプリケーション' AND level = 1), 2, 'フロントエンド',    1),
    ((SELECT id FROM it_skill_categories WHERE name = 'アプリケーション' AND level = 1), 2, 'バックエンド',      2),
    ((SELECT id FROM it_skill_categories WHERE name = 'アプリケーション' AND level = 1), 2, 'データベース',      3),
    ((SELECT id FROM it_skill_categories WHERE name = 'マネジメント' AND level = 1),     2, 'プロジェクト管理',  1),
    ((SELECT id FROM it_skill_categories WHERE name = 'マネジメント' AND level = 1),     2, 'プロセス改善',      2);

-- =============================================================================
-- ITスキル
-- =============================================================================
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'Java'),              'Java SE',              1),
    ((SELECT id FROM it_skill_categories WHERE name = 'Java'),              'Spring Boot',          2),
    ((SELECT id FROM it_skill_categories WHERE name = 'Python'),            'Python基礎',           1),
    ((SELECT id FROM it_skill_categories WHERE name = 'C#'),               'C# / .NET',            1),
    ((SELECT id FROM it_skill_categories WHERE name = 'OS / サーバー'),    'Linux',                 1),
    ((SELECT id FROM it_skill_categories WHERE name = 'OS / サーバー'),    'Windows Server',        2),
    ((SELECT id FROM it_skill_categories WHERE name = 'ネットワーク'),     'TCP/IP・ネットワーク基礎', 1),
    ((SELECT id FROM it_skill_categories WHERE name = 'ネットワーク'),     'ルーター / スイッチ設定', 2),
    ((SELECT id FROM it_skill_categories WHERE name = 'クラウド'),         'AWS',                   1),
    ((SELECT id FROM it_skill_categories WHERE name = 'クラウド'),         'Azure',                 2),
    ((SELECT id FROM it_skill_categories WHERE name = 'フロントエンド'),   'HTML / CSS',            1),
    ((SELECT id FROM it_skill_categories WHERE name = 'フロントエンド'),   'JavaScript',            2),
    ((SELECT id FROM it_skill_categories WHERE name = 'フロントエンド'),   'React',                 3),
    ((SELECT id FROM it_skill_categories WHERE name = 'バックエンド'),     'REST API設計',          1),
    ((SELECT id FROM it_skill_categories WHERE name = 'データベース'),     'SQL基礎',               1),
    ((SELECT id FROM it_skill_categories WHERE name = 'データベース'),     'PostgreSQL',            2),
    ((SELECT id FROM it_skill_categories WHERE name = 'プロジェクト管理'), 'WBS / スケジュール管理', 1),
    ((SELECT id FROM it_skill_categories WHERE name = 'プロジェクト管理'), 'リスク管理',            2),
    ((SELECT id FROM it_skill_categories WHERE name = 'プロセス改善'),    'アジャイル / スクラム',  1);

-- =============================================================================
-- 資格分類・資格
-- =============================================================================
INSERT INTO qualification_categories (name, sort_order) VALUES
    ('国家資格（IT系）', 1),
    ('ベンダー資格',     2);

INSERT INTO qualifications (category_id, name, sort_order) VALUES
    ((SELECT id FROM qualification_categories WHERE name = '国家資格（IT系）'), '基本情報技術者（FE）',                           1),
    ((SELECT id FROM qualification_categories WHERE name = '国家資格（IT系）'), '応用情報技術者（AP）',                           2),
    ((SELECT id FROM qualification_categories WHERE name = '国家資格（IT系）'), '情報処理安全確保支援士（SC）',                    3),
    ((SELECT id FROM qualification_categories WHERE name = '国家資格（IT系）'), 'データベーススペシャリスト（DB）',                4),
    ((SELECT id FROM qualification_categories WHERE name = 'ベンダー資格'),     'AWS認定ソリューションアーキテクト - アソシエイト', 1),
    ((SELECT id FROM qualification_categories WHERE name = 'ベンダー資格'),     'Oracle Java SE 認定',                            2),
    ((SELECT id FROM qualification_categories WHERE name = 'ベンダー資格'),     'Microsoft Azure Administrator（AZ-104）',         3);

-- =============================================================================
-- ADセミナー分類・ADセミナー
-- =============================================================================
INSERT INTO ad_seminar_categories (name, sort_order) VALUES
    ('技術研修',             1),
    ('ビジネス研修',         2),
    ('コンプライアンス研修', 3);

INSERT INTO ad_seminars (category_id, name, sort_order) VALUES
    ((SELECT id FROM ad_seminar_categories WHERE name = '技術研修'),            'クラウド入門',         1),
    ((SELECT id FROM ad_seminar_categories WHERE name = '技術研修'),            'セキュリティ基礎',     2),
    ((SELECT id FROM ad_seminar_categories WHERE name = '技術研修'),            'Java中級',             3),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'ビジネス研修'),        'ビジネスマナー研修',   1),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'ビジネス研修'),        'プロジェクト管理基礎', 2),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'コンプライアンス研修'), '情報セキュリティ教育', 1),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'コンプライアンス研修'), '個人情報保護研修',     2);

-- =============================================================================
-- 社外セミナー分類
-- =============================================================================
INSERT INTO seminar_categories (name, sort_order) VALUES
    ('技術',   1),
    ('ビジネス', 2),
    ('その他', 3);

-- =============================================================================
-- ユーザー
-- 初期パスワード = ユーザーID（BCrypt cost=12）
-- =============================================================================

-- admin / 初期PW: admin → ログイン後に変更不要（is_initial_password=FALSE）
INSERT INTO users (user_id, name, password_hash, role, is_initial_password, is_active)
VALUES (
    'admin',
    '管理者',
    '$2b$12$6zPU82VzWT9rZ7jLF.3yp.qm815BLk3o6j47RjxRu1ZN7CQBPo0Li',
    'ADMIN',
    FALSE,
    TRUE
);

-- tl01 / 初期PW: tl01
INSERT INTO users (user_id, name, password_hash, role, is_initial_password, is_active)
VALUES ('tl01', 'テストTL', '$2a$12$EePvdSwevAErJKjplTw7nOQUbNuclA0YAzG5DLm7.zLWsw51jJxXW', 'TL', TRUE, TRUE);

-- user01, user02 / 初期PW: 各ユーザーID
INSERT INTO users (user_id, name, password_hash, role, tl_user_id, is_initial_password, is_active) VALUES
    ('user01', 'テストユーザー01', '$2a$12$5eDTZtPIqIRxeGm7bOBcZ.D2lFBxREG.PM9LDzJuM3ipvL.CSfThm', 'GENERAL', (SELECT id FROM users WHERE user_id = 'tl01'), TRUE, TRUE),
    ('user02', 'テストユーザー02', '$2a$12$dLqToObRmzZ1J/PsK.08x.Xq6ttTLieZNFHqJw411mF/6nS1vGl/S', 'GENERAL', (SELECT id FROM users WHERE user_id = 'tl01'), TRUE, TRUE);

-- =============================================================================
-- テストデータ：前年度（2024年度）棚卸データ
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

-- ITスキル明細（全スキル）
INSERT INTO it_skill_details (inventory_id, it_skill_id, skill_level_id, remarks)
SELECT
    (SELECT i.id FROM inventories i JOIN users u ON i.user_id = u.id
     WHERE u.user_id = 'user01'
       AND i.fiscal_year_id = (SELECT id FROM fiscal_years WHERE name = '2024年度')),
    s.id,
    (SELECT id FROM skill_levels WHERE level_value = v.lv),
    v.rem
FROM (VALUES
    ('Java SE',                   4, 'Spring Bootでの実務経験あり'),
    ('Spring Boot',               4, 'REST API開発経験あり'),
    ('Python基礎',                2, '研修で学習済み'),
    ('C# / .NET',                 1, NULL),
    ('Linux',                     3, 'Ubuntuサーバー運用経験あり'),
    ('Windows Server',            2, '基本的な設定は可能'),
    ('TCP/IP・ネットワーク基礎',  3, NULL),
    ('ルーター / スイッチ設定',   1, NULL),
    ('AWS',                       3, 'EC2・RDS・S3の基本操作'),
    ('Azure',                     1, NULL),
    ('HTML / CSS',                3, NULL),
    ('JavaScript',                3, 'ES6以降の基礎知識あり'),
    ('React',                     3, '実装経験あり、さらなる習熟が必要'),
    ('REST API設計',              4, 'Spring Bootでの実装経験多数'),
    ('SQL基礎',                   4, 'JOIN・サブクエリなど複雑なクエリも可'),
    ('PostgreSQL',                3, 'インデックス設計・チューニング経験あり'),
    ('WBS / スケジュール管理',    2, '小規模案件のみ'),
    ('リスク管理',                2, NULL),
    ('アジャイル / スクラム',     2, 'スクラム開発経験あり')
) AS v(skill_name, lv, rem)
JOIN it_skills s ON s.name = v.skill_name;

-- 資格明細
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

-- セミナー明細
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
    ('Java SE',                   3, NULL),
    ('Spring Boot',               2, NULL),
    ('Python基礎',                3, 'データ分析業務で使用'),
    ('C# / .NET',                 2, NULL),
    ('Linux',                     2, NULL),
    ('Windows Server',            3, '運用担当として経験あり'),
    ('TCP/IP・ネットワーク基礎',  2, NULL),
    ('ルーター / スイッチ設定',   1, NULL),
    ('AWS',                       2, NULL),
    ('Azure',                     3, 'Azure AD・App Service を業務で利用'),
    ('HTML / CSS',                2, NULL),
    ('JavaScript',                2, NULL),
    ('React',                     1, NULL),
    ('REST API設計',              2, NULL),
    ('SQL基礎',                   3, NULL),
    ('PostgreSQL',                2, NULL),
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
    ('Java SE',                   5, 'チーム内で技術指導を担当'),
    ('Spring Boot',               5, 'アーキテクチャ設計・コードレビュー担当'),
    ('Python基礎',                3, NULL),
    ('C# / .NET',                 2, NULL),
    ('Linux',                     4, 'サーバー設計・構築経験あり'),
    ('Windows Server',            3, NULL),
    ('TCP/IP・ネットワーク基礎',  4, NULL),
    ('ルーター / スイッチ設定',   2, NULL),
    ('AWS',                       4, 'インフラ設計・コスト最適化経験あり'),
    ('Azure',                     2, NULL),
    ('HTML / CSS',                3, NULL),
    ('JavaScript',                4, NULL),
    ('React',                     4, 'フロントエンド設計担当'),
    ('REST API設計',              5, 'API設計ガイドライン策定済み'),
    ('SQL基礎',                   5, NULL),
    ('PostgreSQL',                4, 'パフォーマンスチューニング経験あり'),
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

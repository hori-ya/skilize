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
DROP TABLE IF EXISTS skill_levels           CASCADE;
DROP TABLE IF EXISTS fiscal_years           CASCADE;
DROP TABLE IF EXISTS fiscal_year_settings   CASCADE;

DROP FUNCTION IF EXISTS update_updated_at();

-- -----------------------------------------------------------------------------
-- 共通トリガー関数: updated_at 自動更新
-- -----------------------------------------------------------------------------
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
-- 5. it_skills（ITスキルマスタ）
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

CREATE INDEX idx_it_skills_category ON it_skills(category_id);
CREATE INDEX idx_it_skills_is_active ON it_skills(is_active);

CREATE TRIGGER trg_it_skills_updated_at
    BEFORE UPDATE ON it_skills
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 6. qualifications（参考資格マスタ）
-- -----------------------------------------------------------------------------
CREATE TABLE qualifications (
    id          SERIAL       NOT NULL,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_qualifications PRIMARY KEY (id)
);

COMMENT ON TABLE qualifications IS '参考資格マスタ';

CREATE INDEX idx_qualifications_is_active ON qualifications(is_active);

CREATE TRIGGER trg_qualifications_updated_at
    BEFORE UPDATE ON qualifications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 7. ad_seminars（ADマスタ）
-- -----------------------------------------------------------------------------
CREATE TABLE ad_seminars (
    id          SERIAL       NOT NULL,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_ad_seminars PRIMARY KEY (id)
);

COMMENT ON TABLE ad_seminars IS 'ADマスタ（スキルアップ活動区分）';

CREATE TRIGGER trg_ad_seminars_updated_at
    BEFORE UPDATE ON ad_seminars
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 8. users（ユーザー / TLへの自己参照FK）
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id                  SERIAL       NOT NULL,
    name                VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    role                VARCHAR(10)  NOT NULL,
    tl_user_id          INTEGER,     -- FK → users(id)。TL または ADMIN ロールのユーザーを指定
    is_initial_password BOOLEAN      NOT NULL DEFAULT TRUE,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT fk_users_tl FOREIGN KEY (tl_user_id) REFERENCES users(id),
    CONSTRAINT chk_users_role CHECK (role IN ('GENERAL', 'TL', 'ADMIN'))
);

COMMENT ON TABLE  users IS 'ユーザー';
COMMENT ON COLUMN users.tl_user_id IS 'TLユーザーへの自己参照FK（TL または ADMIN ロールを指定）';
COMMENT ON COLUMN users.password_hash IS 'BCryptハッシュ';

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
-- 9. inventories（棚卸ヘッダー）
-- -----------------------------------------------------------------------------
CREATE TABLE inventories (
    id                SERIAL      NOT NULL,
    user_id           INTEGER     NOT NULL,
    fiscal_year_id    INTEGER     NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at      TIMESTAMPTZ,
    goal_completed_at TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_inventories PRIMARY KEY (id),
    CONSTRAINT uq_inventories_user_year UNIQUE (user_id, fiscal_year_id),
    CONSTRAINT fk_inventories_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_inventories_fiscal_year
        FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_years(id),
    CONSTRAINT chk_inventories_status
        CHECK (status IN ('DRAFT', 'PENDING_GOAL', 'COMPLETED'))
);

COMMENT ON TABLE inventories IS '棚卸ヘッダー（ユーザー×年度で1件）';

CREATE INDEX idx_inventories_user_id        ON inventories(user_id);
CREATE INDEX idx_inventories_fiscal_year_id ON inventories(fiscal_year_id);
CREATE INDEX idx_inventories_status         ON inventories(status);

CREATE TRIGGER trg_inventories_updated_at
    BEFORE UPDATE ON inventories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 10. it_skill_details（ITスキル棚卸明細）
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
-- 11. qualification_details（資格棚卸明細）
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
-- 12. seminar_details（セミナー棚卸明細）
-- -----------------------------------------------------------------------------
CREATE TABLE seminar_details (
    id                   SERIAL       NOT NULL,
    inventory_id         INTEGER      NOT NULL,
    ad_seminar_id        INTEGER,     -- NULL はフリーセミナー
    seminar_name         VARCHAR(200),
    attended_year_month  DATE,        -- 月初日で保存。未受講は NULL
    remarks              TEXT,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_seminar_details PRIMARY KEY (id),
    CONSTRAINT fk_seminar_details_inventory
        FOREIGN KEY (inventory_id) REFERENCES inventories(id),
    CONSTRAINT fk_seminar_details_ad_seminar
        FOREIGN KEY (ad_seminar_id) REFERENCES ad_seminars(id),
    CONSTRAINT chk_seminar_details_ref
        CHECK (ad_seminar_id IS NOT NULL OR seminar_name IS NOT NULL)
);

COMMENT ON TABLE  seminar_details IS 'セミナー棚卸明細';
COMMENT ON COLUMN seminar_details.ad_seminar_id IS 'NULL はフリーセミナー（seminar_name を使用）';

CREATE INDEX idx_seminar_details_inventory ON seminar_details(inventory_id);

CREATE TRIGGER trg_seminar_details_updated_at
    BEFORE UPDATE ON seminar_details
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- -----------------------------------------------------------------------------
-- 13. inventory_goals（目標設定）
-- -----------------------------------------------------------------------------
CREATE TABLE inventory_goals (
    id               SERIAL       NOT NULL,
    inventory_id     INTEGER      NOT NULL,
    goal_category    VARCHAR(20)  NOT NULL,
    it_skill_id      INTEGER,
    qualification_id INTEGER,
    ad_seminar_id    INTEGER,
    custom_name      VARCHAR(200),
    target_period    DATE         NOT NULL,  -- 月初日で保存
    reason           TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

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
        )
);

COMMENT ON TABLE  inventory_goals IS '目標設定';
COMMENT ON COLUMN inventory_goals.goal_category IS 'IT_SKILL / QUALIFICATION / AD';
COMMENT ON COLUMN inventory_goals.target_period IS '月初日で保存（例: 2026-03-01）';

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

-- レベルマスタ（デフォルト定義）
INSERT INTO skill_levels (level_value, description) VALUES
    (1, '知識なし／未経験'),
    (2, '概念を理解している'),
    (3, '指導があれば実務で使える'),
    (4, '独力で実務に適用できる'),
    (5, '他者に指導・展開できる');

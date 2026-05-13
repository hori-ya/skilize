-- =============================================================================
-- V1: 初期スキーマ作成
-- =============================================================================

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 1. fiscal_year_settings
CREATE TABLE fiscal_year_settings (
    id                       SMALLINT    NOT NULL DEFAULT 1,
    fiscal_year_start_month  SMALLINT    NOT NULL DEFAULT 4,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by               INTEGER,

    CONSTRAINT pk_fiscal_year_settings PRIMARY KEY (id),
    CONSTRAINT chk_fiscal_year_settings_month
        CHECK (fiscal_year_start_month BETWEEN 1 AND 12),
    CONSTRAINT chk_fiscal_year_settings_singleton
        CHECK (id = 1)
);

-- 2. fiscal_years
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

CREATE TRIGGER trg_fiscal_years_updated_at
    BEFORE UPDATE ON fiscal_years
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 3. skill_levels
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

CREATE TRIGGER trg_skill_levels_updated_at
    BEFORE UPDATE ON skill_levels
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 4. it_skill_categories
CREATE TABLE it_skill_categories (
    id         SERIAL       NOT NULL,
    parent_id  INTEGER,
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

CREATE INDEX idx_it_skill_categories_parent ON it_skill_categories(parent_id);

CREATE TRIGGER trg_it_skill_categories_updated_at
    BEFORE UPDATE ON it_skill_categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 5. qualification_categories
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

CREATE TRIGGER trg_qualification_categories_updated_at
    BEFORE UPDATE ON qualification_categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 6. ad_seminar_categories
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

CREATE TRIGGER trg_ad_seminar_categories_updated_at
    BEFORE UPDATE ON ad_seminar_categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 7. seminar_categories
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

CREATE TRIGGER trg_seminar_categories_updated_at
    BEFORE UPDATE ON seminar_categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 8. it_skills
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

CREATE INDEX idx_it_skills_category  ON it_skills(category_id);
CREATE INDEX idx_it_skills_is_active ON it_skills(is_active);

CREATE TRIGGER trg_it_skills_updated_at
    BEFORE UPDATE ON it_skills
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 9. qualifications
CREATE TABLE qualifications (
    id          SERIAL       NOT NULL,
    category_id INTEGER,
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

CREATE INDEX idx_qualifications_category  ON qualifications(category_id);
CREATE INDEX idx_qualifications_is_active ON qualifications(is_active);

CREATE TRIGGER trg_qualifications_updated_at
    BEFORE UPDATE ON qualifications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 10. ad_seminars
CREATE TABLE ad_seminars (
    id          SERIAL       NOT NULL,
    category_id INTEGER,
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

CREATE INDEX idx_ad_seminars_category ON ad_seminars(category_id);

CREATE TRIGGER trg_ad_seminars_updated_at
    BEFORE UPDATE ON ad_seminars
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 11. users
CREATE TABLE users (
    id                  SERIAL       NOT NULL,
    user_id             VARCHAR(50)  NOT NULL,
    name                VARCHAR(100) NOT NULL,
    email               VARCHAR(255),
    password_hash       VARCHAR(255) NOT NULL,
    role                VARCHAR(10)  NOT NULL,
    tl_user_id          INTEGER,
    is_initial_password BOOLEAN      NOT NULL DEFAULT TRUE,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_user_id UNIQUE (user_id),
    CONSTRAINT fk_users_tl FOREIGN KEY (tl_user_id) REFERENCES users(id),
    CONSTRAINT chk_users_role CHECK (role IN ('GENERAL', 'TL', 'ADMIN'))
);

CREATE INDEX idx_users_tl_user_id ON users(tl_user_id);
CREATE INDEX idx_users_role       ON users(role);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

ALTER TABLE fiscal_year_settings
    ADD CONSTRAINT fk_fiscal_year_settings_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(id);

-- 12. inventories
CREATE TABLE inventories (
    id                        SERIAL      NOT NULL,
    user_id                   INTEGER     NOT NULL,
    fiscal_year_id            INTEGER     NOT NULL,
    status                    VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at              TIMESTAMPTZ,
    goal_review_completed_at  TIMESTAMPTZ,
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

CREATE INDEX idx_inventories_user_id        ON inventories(user_id);
CREATE INDEX idx_inventories_fiscal_year_id ON inventories(fiscal_year_id);
CREATE INDEX idx_inventories_status         ON inventories(status);

CREATE TRIGGER trg_inventories_updated_at
    BEFORE UPDATE ON inventories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 13. it_skill_details
CREATE TABLE it_skill_details (
    id                SERIAL       NOT NULL,
    inventory_id      INTEGER      NOT NULL,
    it_skill_id       INTEGER,
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

CREATE INDEX idx_it_skill_details_inventory ON it_skill_details(inventory_id);
CREATE INDEX idx_it_skill_details_skill     ON it_skill_details(it_skill_id);

CREATE TRIGGER trg_it_skill_details_updated_at
    BEFORE UPDATE ON it_skill_details
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 14. qualification_details
CREATE TABLE qualification_details (
    id                        SERIAL       NOT NULL,
    inventory_id              INTEGER      NOT NULL,
    qualification_id          INTEGER,
    custom_qualification_name VARCHAR(200),
    acquired_year_month       DATE,
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

CREATE INDEX idx_qualification_details_inventory     ON qualification_details(inventory_id);
CREATE INDEX idx_qualification_details_qualification ON qualification_details(qualification_id);

CREATE TRIGGER trg_qualification_details_updated_at
    BEFORE UPDATE ON qualification_details
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 15. seminar_details
CREATE TABLE seminar_details (
    id                   SERIAL       NOT NULL,
    inventory_id         INTEGER      NOT NULL,
    ad_seminar_id        INTEGER,
    seminar_name         VARCHAR(200),
    seminar_category_id  INTEGER,
    attended_year_month  DATE,
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

CREATE INDEX idx_seminar_details_inventory ON seminar_details(inventory_id);

CREATE TRIGGER trg_seminar_details_updated_at
    BEFORE UPDATE ON seminar_details
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 16. inventory_goals
CREATE TABLE inventory_goals (
    id                 SERIAL       NOT NULL,
    inventory_id       INTEGER      NOT NULL,
    goal_category      VARCHAR(20)  NOT NULL,
    it_skill_id        INTEGER,
    qualification_id   INTEGER,
    ad_seminar_id      INTEGER,
    custom_name        VARCHAR(200),
    target_period      DATE         NOT NULL,
    reason             TEXT,
    achievement_status VARCHAR(20),
    review_note        TEXT,
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

CREATE INDEX idx_inventory_goals_inventory ON inventory_goals(inventory_id);

CREATE TRIGGER trg_inventory_goals_updated_at
    BEFORE UPDATE ON inventory_goals
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- =============================================================================
-- 初期データ
-- =============================================================================

INSERT INTO fiscal_year_settings (id, fiscal_year_start_month)
VALUES (1, 4)
ON CONFLICT (id) DO NOTHING;

INSERT INTO skill_levels (level_value, description) VALUES
    (1, '知識なし／未経験'),
    (2, '概念を理解している'),
    (3, '指導があれば実務で使える'),
    (4, '独力で実務に適用できる'),
    (5, '他者に指導・展開できる');

-- 初期管理者: user_id=admin / 初期パスワード=admin (BCrypt cost=12)
INSERT INTO users (user_id, name, password_hash, role, is_initial_password, is_active)
VALUES (
    'admin',
    '管理者',
    '$2b$12$6zPU82VzWT9rZ7jLF.3yp.qm815BLk3o6j47RjxRu1ZN7CQBPo0Li',
    'ADMIN',
    TRUE,
    TRUE
);

-- =============================================================================
-- 01_schema.sql — テーブル定義・インデックス・トリガー（ローカル開発用）
-- PostgreSQL 16.4
-- =============================================================================

-- -----------------------------------------------------------------------------
-- テーブル・関数の削除（依存関係の逆順）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS interview_detail_notes CASCADE;
DROP TABLE IF EXISTS inventory_interviews   CASCADE;
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

-- 1. fiscal_year_settings（年度設定 / シングルトン）
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

-- 2. fiscal_years（年度マスタ）
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

-- 3. skill_levels（レベルマスタ）
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

-- 4. it_skill_categories（ITスキル分類マスタ / 自己参照・最大3階層）
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

-- 5. qualification_categories（資格分類マスタ / フラット1階層）
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

-- 6. ad_seminar_categories（ADセミナー分類マスタ / フラット1階層）
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

-- 7. seminar_categories（セミナー分類マスタ / フラット1階層 / AD以外のセミナー用）
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

-- 8. it_skills（ITスキルマスタ）
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

-- 9. qualifications（参考資格マスタ）
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

-- 10. ad_seminars（ADマスタ）
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

-- 11. users（ユーザー / TLへの自己参照FK）
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

-- 12. inventories（棚卸ヘッダー）
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

-- 13. it_skill_details（ITスキル棚卸明細）
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

-- 14. qualification_details（資格棚卸明細）
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

-- 15. seminar_details（セミナー棚卸明細）
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

-- 16. inventory_goals（目標設定）
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

-- 17. inventory_interviews（面談メモヘッダー）
CREATE TABLE inventory_interviews (
    id             SERIAL      NOT NULL,
    inventory_id   INTEGER     NOT NULL,
    interviewer_id INTEGER     NOT NULL,
    general_note   TEXT,
    interviewed_at DATE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_inventory_interviews PRIMARY KEY (id),
    CONSTRAINT uq_inventory_interviews UNIQUE (inventory_id, interviewer_id),
    CONSTRAINT fk_inventory_interviews_inventory
        FOREIGN KEY (inventory_id) REFERENCES inventories(id),
    CONSTRAINT fk_inventory_interviews_interviewer
        FOREIGN KEY (interviewer_id) REFERENCES users(id)
);

COMMENT ON TABLE  inventory_interviews IS '面談メモヘッダー（棚卸×入力者で1件）';
COMMENT ON COLUMN inventory_interviews.interviewer_id IS '面談メモを記入したTL/ADMINのusers.id';

CREATE INDEX idx_inventory_interviews_inventory  ON inventory_interviews(inventory_id);
CREATE INDEX idx_inventory_interviews_interviewer ON inventory_interviews(interviewer_id);

CREATE TRIGGER trg_inventory_interviews_updated_at
    BEFORE UPDATE ON inventory_interviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 18. interview_detail_notes（面談メモ明細）
CREATE TABLE interview_detail_notes (
    id           SERIAL      NOT NULL,
    interview_id INTEGER     NOT NULL,
    detail_type  VARCHAR(20) NOT NULL,
    detail_id    INTEGER     NOT NULL,
    note         TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_interview_detail_notes PRIMARY KEY (id),
    CONSTRAINT uq_interview_detail_notes UNIQUE (interview_id, detail_type, detail_id),
    CONSTRAINT fk_interview_detail_notes_interview
        FOREIGN KEY (interview_id) REFERENCES inventory_interviews(id) ON DELETE CASCADE,
    CONSTRAINT chk_interview_detail_notes_type
        CHECK (detail_type IN ('IT_SKILL', 'QUALIFICATION', 'SEMINAR', 'GOAL'))
);

COMMENT ON TABLE  interview_detail_notes IS '面談メモ明細（各棚卸明細行に紐づくTLメモ）';
COMMENT ON COLUMN interview_detail_notes.detail_type IS 'IT_SKILL / QUALIFICATION / SEMINAR / GOAL';
COMMENT ON COLUMN interview_detail_notes.detail_id   IS 'detail_type に応じた各明細テーブルのPK';

CREATE INDEX idx_interview_detail_notes_interview ON interview_detail_notes(interview_id);

CREATE TRIGGER trg_interview_detail_notes_updated_at
    BEFORE UPDATE ON interview_detail_notes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 19. user_expectations（ユーザーへの期待）
CREATE TABLE user_expectations (
    user_id              INTEGER     NOT NULL,
    tl_expectation       TEXT,
    company_expectation  TEXT,
    tl_updated_at        TIMESTAMPTZ,
    company_updated_at   TIMESTAMPTZ,

    CONSTRAINT pk_user_expectations PRIMARY KEY (user_id),
    CONSTRAINT fk_user_expectations_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

COMMENT ON TABLE  user_expectations IS 'ユーザーへの期待（TL期待・会社期待をユーザーごとに1行で管理）';
COMMENT ON COLUMN user_expectations.tl_expectation      IS 'TLが期待すること（TL/ADMINが入力）';
COMMENT ON COLUMN user_expectations.company_expectation IS '会社が期待すること（ADMINのみ入力）';

-- 20. ai_career_analyses（AIキャリア分析）
CREATE TABLE ai_career_analyses (
    id              SERIAL       NOT NULL,
    user_id         INTEGER      NOT NULL,
    fiscal_year_id  INTEGER      NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    analysis_result JSONB,
    error_message   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_ai_career_analyses PRIMARY KEY (id),
    CONSTRAINT uq_ai_career_analyses UNIQUE (user_id, fiscal_year_id),
    CONSTRAINT fk_ai_career_analyses_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ai_career_analyses_fiscal_year
        FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_years(id),
    CONSTRAINT chk_ai_career_analyses_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

COMMENT ON TABLE  ai_career_analyses IS 'AIキャリア分析結果（ユーザー×年度で1件。棚卸COMPLETED遷移時に自動生成）';
COMMENT ON COLUMN ai_career_analyses.status          IS 'PENDING / PROCESSING / COMPLETED / FAILED';
COMMENT ON COLUMN ai_career_analyses.analysis_result IS 'LLMが生成した分析結果JSON（summary / strengths / growth_areas / expectation_fit / recommended_actions）';
COMMENT ON COLUMN ai_career_analyses.error_message   IS 'エラー内容（status=FAILED時のみ格納）';

CREATE INDEX idx_ai_career_analyses_user ON ai_career_analyses(user_id);

CREATE TRIGGER trg_ai_career_analyses_updated_at
    BEFORE UPDATE ON ai_career_analyses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

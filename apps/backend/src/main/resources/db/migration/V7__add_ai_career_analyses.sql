-- =============================================================================
-- V7__add_ai_career_analyses.sql
-- AIキャリア分析結果テーブルの追加
-- =============================================================================

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

-- =============================================================================
-- V5: 面談メモ機能 テーブル追加
-- =============================================================================

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

CREATE INDEX idx_inventory_interviews_inventory  ON inventory_interviews(inventory_id);
CREATE INDEX idx_inventory_interviews_interviewer ON inventory_interviews(interviewer_id);

CREATE TRIGGER trg_inventory_interviews_updated_at
    BEFORE UPDATE ON inventory_interviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

COMMENT ON TABLE  inventory_interviews IS '面談メモヘッダー（棚卸×入力者で1件）';
COMMENT ON COLUMN inventory_interviews.interviewer_id IS '面談メモを記入したTL/ADMINのusers.id';

-- -----------------------------------------------------------------------------

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

CREATE INDEX idx_interview_detail_notes_interview ON interview_detail_notes(interview_id);

CREATE TRIGGER trg_interview_detail_notes_updated_at
    BEFORE UPDATE ON interview_detail_notes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

COMMENT ON TABLE  interview_detail_notes IS '面談メモ明細（各棚卸明細行に紐づくTLメモ）';
COMMENT ON COLUMN interview_detail_notes.detail_type IS 'IT_SKILL / QUALIFICATION / SEMINAR / GOAL';
COMMENT ON COLUMN interview_detail_notes.detail_id   IS 'detail_type に応じた各明細テーブルのPK';

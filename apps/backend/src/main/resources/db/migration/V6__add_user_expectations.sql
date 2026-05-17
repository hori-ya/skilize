-- V6: ユーザーへの期待テーブル追加
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

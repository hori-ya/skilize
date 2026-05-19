-- skill_levels にグラフスコア計算用の重み列を追加する。
-- score_weight = 0 はスコアに寄与しない（例: レベル1=知識なし）。
-- 既存レコードはレベル値から 1 を引いた値（level_value - 1）をデフォルトとして設定する。
ALTER TABLE skill_levels
    ADD COLUMN score_weight INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN skill_levels.score_weight IS 'グラフスコア計算に使用する重み値。0 はスコアに寄与しない（レベル1=知識なし想定）。';

-- 既存レコード: level_value - 1 を初期値として設定
UPDATE skill_levels SET score_weight = level_value - 1;

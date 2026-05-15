-- =============================================================================
-- V3: デフォルトマスタデータ登録
--
-- 本番運用開始前に管理者が設定・変更可能なマスタデータ。
-- 実際に使用するマスタデータに合わせてこのファイルを編集してから適用すること。
-- ローカル開発用の対応ファイル: scripts/db/03_master_data.sql
-- =============================================================================

-- =============================================================================
-- 年度マスタ
-- =============================================================================
INSERT INTO fiscal_years (name, start_date, end_date, input_start_date, input_end_date, is_active)
VALUES ('2025年度', '2025-04-01', '2026-03-31', '2025-04-01', '2025-06-30', TRUE);

-- =============================================================================
-- ITスキル分類（L1 / L2 / L3）
-- =============================================================================

-- L1: ロール分類
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    (NULL, 1, 'PG',                 1),
    (NULL, 1, 'SE',                 2),
    (NULL, 1, 'TL',                 3),
    (NULL, 1, 'PM',                 4),
    (NULL, 1, 'システムコンサルタント', 5),
    (NULL, 1, '業務知識',           6);

-- L2: PG
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'PG' AND level = 1), 2, '設計・開発',   1),
    ((SELECT id FROM it_skill_categories WHERE name = 'PG' AND level = 1), 2, '技術基盤',     2),
    ((SELECT id FROM it_skill_categories WHERE name = 'PG' AND level = 1), 2, '品質・管理',   3),
    ((SELECT id FROM it_skill_categories WHERE name = 'PG' AND level = 1), 2, 'テスト・導入', 4),
    ((SELECT id FROM it_skill_categories WHERE name = 'PG' AND level = 1), 2, '共通スキル',   5);

-- L2: SE
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'SE' AND level = 1), 2, '上流工程',       1),
    ((SELECT id FROM it_skill_categories WHERE name = 'SE' AND level = 1), 2, '非機能・インフラ', 2),
    ((SELECT id FROM it_skill_categories WHERE name = 'SE' AND level = 1), 2, 'テスト・管理',   3),
    ((SELECT id FROM it_skill_categories WHERE name = 'SE' AND level = 1), 2, '品質向上・意識', 4);

-- L2: TL
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'TL' AND level = 1), 2, '管理・指導', 1);

-- L2: PM
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'PM' AND level = 1), 2, '統制・管理', 1),
    ((SELECT id FROM it_skill_categories WHERE name = 'PM' AND level = 1), 2, '調整・支援', 2);

-- L2: システムコンサルタント
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'システムコンサルタント' AND level = 1), 2, '顧客・営業', 1);

-- L2: 業務知識
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '業務知識' AND level = 1), 2, '基幹業務',     1),
    ((SELECT id FROM it_skill_categories WHERE name = '業務知識' AND level = 1), 2, '製造',         2),
    ((SELECT id FROM it_skill_categories WHERE name = '業務知識' AND level = 1), 2, 'バックオフィス', 3);

-- L3: PG > 設計・開発
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '設計・開発'), 3, '製造・修正', 1);

-- L3: PG > 技術基盤
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '技術基盤'), 3, 'DB・言語',    1),
    ((SELECT id FROM it_skill_categories WHERE name = '技術基盤'), 3, 'インフラ設定', 2);

-- L3: PG > 品質・管理
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '品質・管理'), 3, '品質・規約', 1);

-- L3: PG > テスト・導入
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'テスト・導入'), 3, 'テスト実施',    1),
    ((SELECT id FROM it_skill_categories WHERE name = 'テスト・導入'), 3, '環境・リリース', 2);

-- L3: PG > 共通スキル
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '共通スキル'), 3, 'ポータブル', 1);

-- L3: SE > 上流工程
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '上流工程'), 3, '要件・設計', 1);

-- L3: SE > 非機能・インフラ
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '非機能・インフラ'), 3, 'システム定義', 1);

-- L3: SE > テスト・管理
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'テスト・管理'), 3, 'テスト設計',    1),
    ((SELECT id FROM it_skill_categories WHERE name = 'テスト・管理'), 3, '基盤・運用管理', 2);

-- L3: SE > 品質向上・意識
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '品質向上・意識'), 3, 'レビュー・提案', 1);

-- L3: TL > 管理・指導
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '管理・指導'), 3, '進捗・見積',   1),
    ((SELECT id FROM it_skill_categories WHERE name = '管理・指導'), 3, 'メンバー対応', 2);

-- L3: PM > 統制・管理
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '統制・管理'), 3, 'プロジェクト管理', 1);

-- L3: PM > 調整・支援
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '調整・支援'), 3, '組織・クライアント', 1);

-- L3: システムコンサルタント > 顧客・営業
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '顧客・営業'), 3, '関係・課題', 1),
    ((SELECT id FROM it_skill_categories WHERE name = '顧客・営業'), 3, '提案・契約', 2);

-- L3: 業務知識 > 基幹業務
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '基幹業務'), 3, 'SCM', 1);

-- L3: 業務知識 > 製造
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '製造'), 3, '製造・品質', 1);

-- L3: 業務知識 > バックオフィス
-- ※ L2と同名を避けるため「財務・人事」に変更
INSERT INTO it_skill_categories (parent_id, level, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'バックオフィス'), 3, '財務・人事', 1);

-- =============================================================================
-- ITスキル（スキルは L3 カテゴリに紐付け）
-- =============================================================================

-- PG > 設計・開発 > 製造・修正
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '製造・修正'), '詳細設計書を作成することができる。',                       1),
    ((SELECT id FROM it_skill_categories WHERE name = '製造・修正'), '詳細設計書を読み、PGの作成、改修をすることができる。',      2),
    ((SELECT id FROM it_skill_categories WHERE name = '製造・修正'), 'PL/SQLを作成、改修することができる。',                      3);

-- PG > 技術基盤 > DB・言語
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'DB・言語'), 'SQLの基本的な操作をすることができる。',               1),
    ((SELECT id FROM it_skill_categories WHERE name = 'DB・言語'), '特定のフレームワークを理解するように意識して実践している。', 2),
    ((SELECT id FROM it_skill_categories WHERE name = 'DB・言語'), '新しい技術やツールの調査、評価をすることができる。',       3);

-- PG > 技術基盤 > インフラ設定
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'インフラ設定'), 'サーバー（Linux、Windowsなど）の設定をすることができる。',                1),
    ((SELECT id FROM it_skill_categories WHERE name = 'インフラ設定'), 'データベース（Oracle、PostgreSQL、SQL Serverなど）の設定をすることができる。', 2),
    ((SELECT id FROM it_skill_categories WHERE name = 'インフラ設定'), 'クラウドサービス（AWS、Azure、GCPなど）の設定をすることができる。',          3);

-- PG > 品質・管理 > 品質・規約
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '品質・規約'), 'コーディング規約を作成することができる。',                                          1),
    ((SELECT id FROM it_skill_categories WHERE name = '品質・規約'), 'コーディング規約を理解し、遵守するように意識して実践している。',                    2),
    ((SELECT id FROM it_skill_categories WHERE name = '品質・規約'), '可読性、保守性、拡張性の高いコード、SQL等を作成するように意識して実践している。',   3),
    ((SELECT id FROM it_skill_categories WHERE name = '品質・規約'), 'レスポンスの早いコード、SQL等を作成するように意識して実践している。',                4),
    ((SELECT id FROM it_skill_categories WHERE name = '品質・規約'), 'セキュリティを考慮したコードを作成するように意識して実践している。',                 5);

-- PG > テスト・導入 > テスト実施
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'テスト実施'), '詳細設計書をもとに単体テスト仕様書を作成することができる。', 1),
    ((SELECT id FROM it_skill_categories WHERE name = 'テスト実施'), '単体テスト仕様書をもとにテストを実施することができる。',     2),
    ((SELECT id FROM it_skill_categories WHERE name = 'テスト実施'), '結合テスト仕様書をもとにテストを実施することができる。',     3),
    ((SELECT id FROM it_skill_categories WHERE name = 'テスト実施'), 'システムテスト仕様書をもとにテストを実施することができる。', 4);

-- PG > テスト・導入 > 環境・リリース
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '環境・リリース'), '開発中の問題の状況把握、原因究明、解決策の策定をすることができる。',         1),
    ((SELECT id FROM it_skill_categories WHERE name = '環境・リリース'), 'デバッグツールを活用することができる。',                                      2),
    ((SELECT id FROM it_skill_categories WHERE name = '環境・リリース'), '開発端末に開発環境を構築することができる。（IDEやODBCの設定など）',           3),
    ((SELECT id FROM it_skill_categories WHERE name = '環境・リリース'), 'バージョン管理システム（Gitなど）を使用することができる。',                   4),
    ((SELECT id FROM it_skill_categories WHERE name = '環境・リリース'), '手順書通りに、データのセットアップ、移行の実施をすることができる。',          5),
    ((SELECT id FROM it_skill_categories WHERE name = '環境・リリース'), '漏れ、デグレがないように意識してリリース作業を実践している。',                6),
    ((SELECT id FROM it_skill_categories WHERE name = '環境・リリース'), '障害発生時の状況把握、原因究明、解決策の策定、復旧作業をすることができる。', 7);

-- PG > 共通スキル > ポータブル
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'ポータブル'), '作業の進捗について報告・連絡・相談をするように意識して実践している。', 1),
    ((SELECT id FROM it_skill_categories WHERE name = 'ポータブル'), '結論から話すように意識して実践している。',                          2);

-- SE > 上流工程 > 要件・設計
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '要件・設計'), '基本設計書を作成することができる。', 1),
    ((SELECT id FROM it_skill_categories WHERE name = '要件・設計'), '要件定義書を作成することができる。', 2);

-- SE > 非機能・インフラ > システム定義
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'システム定義'), 'インフラ要件（サーバー、ネットワークなど）を検討することができる。',  1),
    ((SELECT id FROM it_skill_categories WHERE name = 'システム定義'), '非機能要件（性能、可用性、セキュリティなど）を定義することができる。', 2),
    ((SELECT id FROM it_skill_categories WHERE name = 'システム定義'), '技術選定、アーキテクチャ設計をすることができる。',                    3);

-- SE > テスト・管理 > テスト設計
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'テスト設計'), '結合テスト仕様書を作成することができる。',     1),
    ((SELECT id FROM it_skill_categories WHERE name = 'テスト設計'), 'システムテスト仕様書を作成することができる。', 2);

-- SE > テスト・管理 > 基盤・運用管理
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '基盤・運用管理'), 'ジョブの実行計画、管理をすることができる。',                     1),
    ((SELECT id FROM it_skill_categories WHERE name = '基盤・運用管理'), 'バージョン管理システム（Gitなど）の構築、管理をすることができる。', 2),
    ((SELECT id FROM it_skill_categories WHERE name = '基盤・運用管理'), 'クライアント端末の設定の計画、手順書作成をすることができる。',      3),
    ((SELECT id FROM it_skill_categories WHERE name = '基盤・運用管理'), 'データのセットアップ、移行の計画、手順書を作成することができる。',  4),
    ((SELECT id FROM it_skill_categories WHERE name = '基盤・運用管理'), 'リリース時期の調整、リリースの計画をすることができる。',            5);

-- SE > 品質向上・意識 > レビュー・提案
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'レビュー・提案'), 'コードレビューを実施し、品質向上に貢献するように意識して実践している。', 1),
    ((SELECT id FROM it_skill_categories WHERE name = 'レビュー・提案'), '設計書レビューを実施し、品質向上に貢献するように意識して実践している。', 2),
    ((SELECT id FROM it_skill_categories WHERE name = 'レビュー・提案'), '課題解決のためにクライアントに提案するように意識して実践している。',     3),
    ((SELECT id FROM it_skill_categories WHERE name = 'レビュー・提案'), 'システムの目的を理解、意識するようにしている。',                         4);

-- TL > 管理・指導 > 進捗・見積
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '進捗・見積'), 'WBS、ガントチャートなどを作成し、作業を管理することができる。', 1),
    ((SELECT id FROM it_skill_categories WHERE name = '進捗・見積'), '工数、費用の見積もりをすることができる。',                     2),
    ((SELECT id FROM it_skill_categories WHERE name = '進捗・見積'), '進捗会議を主導することができる。',                             3);

-- TL > 管理・指導 > メンバー対応
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'メンバー対応'), 'メンバーのスキルや特性を把握し、作業指示をするように意識して実践している。', 1),
    ((SELECT id FROM it_skill_categories WHERE name = 'メンバー対応'), 'メンバーと円滑なコミュニケーションをするように意識して実践している。',       2),
    ((SELECT id FROM it_skill_categories WHERE name = 'メンバー対応'), 'メンバーの育成をすることができる。',                                         3);

-- PM > 統制・管理 > プロジェクト管理
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'プロジェクト管理'), 'ステークホルダーを把握するように意識して実践している。', 1),
    ((SELECT id FROM it_skill_categories WHERE name = 'プロジェクト管理'), '品質管理をすることができる。',                         2),
    ((SELECT id FROM it_skill_categories WHERE name = 'プロジェクト管理'), 'コスト管理をすることができる。',                        3),
    ((SELECT id FROM it_skill_categories WHERE name = 'プロジェクト管理'), 'マスタスケジュール管理をすることができる。',             4),
    ((SELECT id FROM it_skill_categories WHERE name = 'プロジェクト管理'), 'リスクの特定、分析、管理をすることができる。',           5),
    ((SELECT id FROM it_skill_categories WHERE name = 'プロジェクト管理'), '変更管理をすることができる。',                          6);

-- PM > 調整・支援 > 組織・クライアント
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '組織・クライアント'), 'BCPの作成をすることができる。',                            1),
    ((SELECT id FROM it_skill_categories WHERE name = '組織・クライアント'), 'クライアントに進捗、課題、リスクを報告することができる。',  2),
    ((SELECT id FROM it_skill_categories WHERE name = '組織・クライアント'), '運用テストの調整、実施をすることができる。',                3),
    ((SELECT id FROM it_skill_categories WHERE name = '組織・クライアント'), '組織横断的な調整をすることができる。',                      4),
    ((SELECT id FROM it_skill_categories WHERE name = '組織・クライアント'), 'チームビルディング、モチベーション管理をすることができる。', 5),
    ((SELECT id FROM it_skill_categories WHERE name = '組織・クライアント'), 'メンターとして後輩を指導することができる。',                6);

-- システムコンサルタント > 顧客・営業 > 関係・課題
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '関係・課題'), '顧客との関係構築をするように意識して実践している。（既存顧客との関係、中部ITのイベント参加など）', 1),
    ((SELECT id FROM it_skill_categories WHERE name = '関係・課題'), '顧客の購買プロセスを理解するように意識して実践している。（稟議、予算、期末など）',               2),
    ((SELECT id FROM it_skill_categories WHERE name = '関係・課題'), '顧客の顕在、潜在課題を引き出すように意識して実践している。',                                     3),
    ((SELECT id FROM it_skill_categories WHERE name = '関係・課題'), '顧客の経営課題をIT課題に置き換えることができる。',                                               4);

-- システムコンサルタント > 顧客・営業 > 提案・契約
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '提案・契約'), '顧客にシステム導入による費用対効果（投資利益率、投資回収期間など）を説明することができる。', 1),
    ((SELECT id FROM it_skill_categories WHERE name = '提案・契約'), '提案書を作成し、プレゼンテーションをすることができる。',                                       2),
    ((SELECT id FROM it_skill_categories WHERE name = '提案・契約'), '見積書を作成すること、見積の根拠を説明することができる。',                                     3),
    ((SELECT id FROM it_skill_categories WHERE name = '提案・契約'), '派遣契約、請負契約、準委任契約などの契約形態について理解している。',                           4),
    ((SELECT id FROM it_skill_categories WHERE name = '提案・契約'), '価格、契約条件の交渉をすることができる。',                                                     5),
    ((SELECT id FROM it_skill_categories WHERE name = '提案・契約'), '契約管理をすることができる。',                                                                 6),
    ((SELECT id FROM it_skill_categories WHERE name = '提案・契約'), '顧客とエンジニアの間を円滑に取り持つように意識して実践している。',                             7);

-- 業務知識 > 基幹業務 > SCM
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = 'SCM'), '販売、受注管理について理解している。', 1),
    ((SELECT id FROM it_skill_categories WHERE name = 'SCM'), '購買、発注管理について理解している。', 2),
    ((SELECT id FROM it_skill_categories WHERE name = 'SCM'), '在庫、倉庫管理について理解している。', 3);

-- 業務知識 > 製造 > 製造・品質
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '製造・品質'), '生産管理について理解している。', 1),
    ((SELECT id FROM it_skill_categories WHERE name = '製造・品質'), '品質管理について理解している。', 2);

-- 業務知識 > バックオフィス > 財務・人事
INSERT INTO it_skills (category_id, name, sort_order) VALUES
    ((SELECT id FROM it_skill_categories WHERE name = '財務・人事'), '会計、財務管理について理解している。', 1),
    ((SELECT id FROM it_skill_categories WHERE name = '財務・人事'), '人事、給与管理について理解している。', 2);

-- =============================================================================
-- 資格分類・資格
-- =============================================================================
INSERT INTO qualification_categories (name, sort_order) VALUES
    ('一般資格',       1),
    ('DBMS',           2),
    ('IPA試験',        3),
    ('Linux技術者',    4),
    ('MSOffice資格',   5),
    ('PMO協会',        6),
    ('インフラ',       7),
    ('国際資格',       8),
    ('シスコ認定',     9),
    ('ﾃﾞｨｰﾌﾟﾗｰﾆﾝｸﾞ', 10),
    ('ITCA',           11);

INSERT INTO qualifications (category_id, name, sort_order) VALUES
    ((SELECT id FROM qualification_categories WHERE name = '一般資格'), 'FP技能検定3級',           1),
    ((SELECT id FROM qualification_categories WHERE name = '一般資格'), 'TOEIC（600点以上）',      2),
    ((SELECT id FROM qualification_categories WHERE name = '一般資格'), '宅地建物取引士資格試験',  3),
    ((SELECT id FROM qualification_categories WHERE name = '一般資格'), '日経経済力テスト',        4),
    ((SELECT id FROM qualification_categories WHERE name = '一般資格'), '日商簿記1級',             5),
    ((SELECT id FROM qualification_categories WHERE name = '一般資格'), '日商簿記2級',             6),
    ((SELECT id FROM qualification_categories WHERE name = '一般資格'), '日商簿記3級',             7),
    ((SELECT id FROM qualification_categories WHERE name = '一般資格'), '秘書検定2級',             8),
    ((SELECT id FROM qualification_categories WHERE name = '一般資格'), '中小企業診断士',          9),
    ((SELECT id FROM qualification_categories WHERE name = 'DBMS'), 'MySQL認定資格',               1),
    ((SELECT id FROM qualification_categories WHERE name = 'DBMS'), 'Oracle Master Bronze',        2),
    ((SELECT id FROM qualification_categories WHERE name = 'DBMS'), 'Oracle Master Silver',        3),
    ((SELECT id FROM qualification_categories WHERE name = 'DBMS'), 'Oracle Master Gold',          4),
    ((SELECT id FROM qualification_categories WHERE name = 'DBMS'), 'Oracle認定Javaプログラマ',   5),
    ((SELECT id FROM qualification_categories WHERE name = 'IPA試験'), '情報セキュリティマネジメント試験', 1),
    ((SELECT id FROM qualification_categories WHERE name = 'IPA試験'), '基本情報技術者試験',              2),
    ((SELECT id FROM qualification_categories WHERE name = 'IPA試験'), '応用情報技術者試験',              3),
    ((SELECT id FROM qualification_categories WHERE name = 'IPA試験'), 'システムアーキテクト試験',        4),
    ((SELECT id FROM qualification_categories WHERE name = 'IPA試験'), 'システム監査技術者試験',          5),
    ((SELECT id FROM qualification_categories WHERE name = 'IPA試験'), '情報処理安全確保支援士試験',      6),
    ((SELECT id FROM qualification_categories WHERE name = 'IPA試験'), 'データベーススペシャリスト試験',  7),
    ((SELECT id FROM qualification_categories WHERE name = 'IPA試験'), 'ネットワークスペシャリスト試験',  8),
    ((SELECT id FROM qualification_categories WHERE name = 'IPA試験'), 'プロジェクトマネージャー試験',    9),
    ((SELECT id FROM qualification_categories WHERE name = 'IPA試験'), 'ITストラテジスト試験',           10),
    ((SELECT id FROM qualification_categories WHERE name = 'Linux技術者'), 'LinuC レベル1',              1),
    ((SELECT id FROM qualification_categories WHERE name = 'Linux技術者'), 'LinuC レベル2',              2),
    ((SELECT id FROM qualification_categories WHERE name = 'MSOffice資格'), 'MOS',                       1),
    ((SELECT id FROM qualification_categories WHERE name = 'PMO協会'), 'PMOスペシャリスト試験',          1),
    ((SELECT id FROM qualification_categories WHERE name = 'インフラ'), 'AWS認定資格 Associate Developer',            1),
    ((SELECT id FROM qualification_categories WHERE name = 'インフラ'), 'AWS認定資格 Associate Solutions Architect',  2),
    ((SELECT id FROM qualification_categories WHERE name = 'インフラ'), 'AWS認定資格 Associate SysOps Administrator', 3),
    ((SELECT id FROM qualification_categories WHERE name = 'インフラ'), 'AWS認定資格 Foundational Cloud Practitoner', 4),
    ((SELECT id FROM qualification_categories WHERE name = '国際資格'), 'PMP',                            1),
    ((SELECT id FROM qualification_categories WHERE name = 'シスコ認定'), 'CCNA',                        1),
    ((SELECT id FROM qualification_categories WHERE name = 'ﾃﾞｨｰﾌﾟﾗｰﾆﾝｸﾞ'), 'G検定',                  1),
    ((SELECT id FROM qualification_categories WHERE name = 'ﾃﾞｨｰﾌﾟﾗｰﾆﾝｸﾞ'), 'E資格',                  2),
    ((SELECT id FROM qualification_categories WHERE name = 'ITCA'), 'ITコーディネータ',                  1);

-- =============================================================================
-- ADセミナー分類・ADセミナー
-- =============================================================================
INSERT INTO ad_seminar_categories (name, sort_order) VALUES
    ('Planning&Control',   1),
    ('Communication',      2),
    ('Thinking',           3),
    ('Business Knowledge', 4),
    ('Self-Management',    5);

-- Planning&Control
INSERT INTO ad_seminars (category_id, name, sort_order) VALUES
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '経営戦略概論',                                                                              1),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '組織・人事管理概論',                                                                        2),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), 'ストーリーで学ぶ ビジネスリーダー研修＜全体編＞',                                           3),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), 'ストーリーで学ぶ ビジネスリーダー研修＜ビジョン設定・仕事の構想編＞',                      4),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), 'ストーリーで学ぶ ビジネスリーダー研修＜マネジメント（PDCA）/仕事の動機づけ編＞',           5),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '管理職のための部下育成シリーズ＜時間力＞',                                                  6),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '［管理職向け］目標の立て方のコツ',                                                          7),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '［管理職向け］組織目標を達成するための指標の立て方',                                        8),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '［管理職向け］PDCAの廻し方',                                                                9),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '［管理職向け］アクションプランの立て方',                                                   10),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '次期管理職養成研修＜全体概要編＞',                                                          11),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '次期管理職養成研修＜パラダイムシフト編＞',                                                  12),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '次期管理職養成研修＜フォロワーシップ編＞',                                                  13),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '『体感型』チームビルディング研修',                                                          14),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), 'プロジェクトマネジメントの全体像',                                                          15),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), 'ビジネス判断力向上研修',                                                                    16),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '抜け漏れなく仕事を進めるためのタスク分解',                                                  17),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '仕事の進捗管理入門',                                                                        18),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '［若手向け］目的思考のすすめ',                                                              19),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '［若手向け］マルチタスクの進め方',                                                          20),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), 'タイムマネジメント',                                                                        21),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '覚悟のタイムマネジメント',                                                                  22),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '仕事の進め方の基本',                                                                        23),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '［管理職向け］実践！部下への権限委譲＜仕事の洗い出し＞',                                   24),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '［管理職向け］実践！部下の目標設定 成功のコツ',                                             25),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '［管理職向け］実践！部下を成長させる育成計画設定 成功のコツ',                               26),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '［管理職向け］実践！突然プロジェクトに任命されたあなたのためのプロジェクトマネジメント入門', 27),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '実践！仕事の分解＜未経験の仕事でも成功する技術＞',                                           28),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '実践！より生産性を高めるための時間管理とやるべき仕事の整理',                                29),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '実践！PDCA＜自身の業務で計画・実行・評価・改善のサイクルを廻すコツ＞',                      30),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '実践！管理職準備研修＜組織目標設定・メンバーへの伝達浸透＞',                                31),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '実践！管理職準備研修＜業務マネジメント・メンバーマネジメント＞',                            32),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '［若手向け］実践！時間管理＜短い時間で今よりも生産性をあげるコツ＞',                        33),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Planning&Control'), '［若手向け］特訓！仕事の段取り＜手戻りなく仕事を進めるための受け方＞',                      34);

-- Communication
INSERT INTO ad_seminars (category_id, name, sort_order) VALUES
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), 'ストーリーで学ぶ ビジネスリーダー研修＜ビジョン・仕事の構想浸透/チームビルディング編＞',     1),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［管理職向け］部下を育成するために必要な要素',                                               2),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '管理職のための部下育成シリーズ＜聴く力&話す力＞',                                            3),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '管理職のための部下育成シリーズ＜書く力＞',                                                    4),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '【基礎】部下を持つ管理職のためのコーチング',                                                 5),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［管理職向け］人事評価の基本＜心構えと評価編＞',                                             6),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［管理職向け］人事評価の基本＜フィードバック面談編＞',                                       7),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '成果を出す会議のコツ研修',                                                                   8),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), 'ファシリテーション入門',                                                                     9),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［若手向け］今日から始めるビジネス読解力トレーニング',                                       10),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), 'ビジネス・ライティング＜わかりやすい文章の書き方編＞',                                       11),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '【特訓】ビジネス・ライティング',                                                             12),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［新入社員向け］ビジネス・ライティング＜ビジネス文書のマナー編＞',                            13),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［新入社員向け］ビジネス・ライティング＜メール文書の型習得編＞',                              14),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), 'スライド作成の基本',                                                                         15),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［若手向け］オーラルコミュニケーションの全体像',                                             16),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '【基礎】ポジティブ・リスニング',                                                             17),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '傾聴力の基本',                                                                               18),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '質問力の基本',                                                                               19),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［若手向け］ビジネス・トーキング',                                                           20),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［若手向け］仕事をお願いするときの話し方',                                                   21),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), 'プレゼンテーション入門＜シナリオ作成編＞',                                                   22),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), 'プレゼンテーション入門＜デリバリー力向上編＞',                                               23),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '相手に理解・納得してもらうための伝え方',                                                     24),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '交渉力入門',                                                                                 25),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '『体感型』報連相研修1＜仕事の受け方編＞',                                                    26),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '『体感型』報連相研修2＜報告・相談編＞',                                                      27),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '『体感型』挨拶研修',                                                                         28),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［管理職向け］実践！人事評価フィードバック＜厳しい評価を伝え、部下の成長を促す＞',           29),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［管理職向け］実践！部下との面談で使えるコーチング',                                         30),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［管理職向け］実践！部下の叱り方＜感情にとらわれず部下を指導する＞',                         31),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '特訓！読み手に伝わる議事録＜正確かつわかりやすく読み手に伝わる議事録作成＞',                 32),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '特訓！相手に理解・納得してもらうための伝え方＜社内会議で自分の意見を伝えるコツ＞',           33),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '特訓！相手に理解・納得してもらうための伝え方＜限られた時間内に相手の合意をとるコツ＞',      34),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '特訓！相手の真意を理解するための聴き方＜相手が話したくなる雰囲気づくり＞',                   35),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '特訓！はじめての会議ファシリテーション＜積極的な意見交換を生み出す＞',                       36),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '特訓！はじめての会議ファシリテーション＜論点を整理して合意形成する＞',                       37),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '特訓！読み間違い・聞き間違いをなくすための問い60 ＜ビジネス読解力向上＞',                    38),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［若手向け］特訓！信頼を勝ち取る報連相＜口頭での報連相のコツ＞',                             39),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［若手向け］特訓！信頼を勝ち取る報連相＜文書での報連相のコツ＞',                             40),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［若手向け］特訓！メールの書き方＜わかりやすいメールを書くための要諦＞',                      41),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［若手向け］特訓！相手に理解・納得してもらうための伝え方＜言いたいことが伝わる話の構成＞',   42),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［若手向け］特訓！相手に理解・納得してもらうための伝え方＜相手に受け入れてもらえる話し方のコツ＞', 43),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Communication'), '［若手向け］特訓！相手を理解するための聴き方＜話を聞きながら相手の話を正確に把握するコツ＞', 44);

-- Thinking
INSERT INTO ad_seminars (category_id, name, sort_order) VALUES
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), '管理職のための部下育成シリーズ＜考える力＞',                                                        1),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), '［若手向け］思考力の全体像',                                                                        2),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), '【基礎】ロジカル・シンキング',                                                                      3),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), '論理的思考力ブラッシュアップ研修＜前編・後編＞',                                                    4),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), 'ケースで学ぶ 論理的思考力ブラッシュアップ研修',                                                    5),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), '［若手向け］クリエイティブ・シンキング',                                                            6),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), '［若手向け］アイデアを出すための発想法',                                                            7),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), 'クリティカル・シンキング',                                                                          8),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), '［若手向け］データの読み方入門',                                                                    9),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), 'はじめての統計分析',                                                                               10),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), 'コンセプチュアルスキルの高め方',                                                                   11),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), '思考を深めるための情報整理術',                                                                     12),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), '課題・施策を特定するための要素分解トレーニング',                                                   13),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), '図解を用いた思考整理トレーニング',                                                                 14),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), '［新入社員向け］考える力の鍛え方',                                                                 15),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), '［管理職向け］実践！組織の問題が洗い出せる思考プロセス＜全社視点で俯瞰的に捉える＞',               16),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), '［管理職向け］実践！組織の問題を解決する思考プロセス＜課題の特定と解決策の立案＞',                 17),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), '「前からそうだ」を許容していませんか？特訓！クリティカル・シンキング＜前提を疑う＞',               18),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), 'その結論は正しいですか？特訓！クリティカル・シンキング＜論理展開を疑う＞',                         19),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), '［若手向け］特訓！考えるクセをつけるための問い60',                                                 20),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Thinking'), '［若手向け］特訓！ビジネスに必要な情報整理力向上のコツ＜正確に報告するための現状整理＞',           21);

-- Business Knowledge
INSERT INTO ad_seminars (category_id, name, sort_order) VALUES
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '経理・財務部課長の役割と実務',                                                                                         1),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '人事・総務部課長の役割と実務',                                                                                         2),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［管理職向け］労務管理研修＜メンタルヘルスの基礎知識編＞',                                                             3),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［管理職向け］労務管理研修＜セクハラ・パワハラの基礎知識編＞',                                                         4),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［現場指導者向け］体系的に学ぶOJTの進め方',                                                                           5),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '若手社員に必要なビジネススキルの全体像',                                                                               6),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '新入社員研修',                                                                                                         7),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), 'ビジネスマナー研修1＜良好な人間関係を築く5要素＞',                                                                    8),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), 'ビジネスマナー研修2＜ビジネスを円滑に進めるための形式＞',                                                             9),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［若手向け］ケースで学ぶコンプライアンス',                                                                            10),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［若手向け］労務基礎知識',                                                                                            11),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［若手向け］他部門連携のために知るべき7要素',                                                                        12),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '基礎からはじめるビジネス教養＜売上・コストの捉え方編＞',                                                             13),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '基礎からはじめるビジネス教養＜経済編＞',                                                                              14),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '基礎からはじめるビジネス教養＜経営編＞',                                                                              15),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '【初級】経営数字＜初めて損益計算書・貸借対照表を見る方対象＞',                                                        16),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '【中級】経営数字（経営分析の基礎）＜損益計算書・貸借対照表が読める方対象＞',                                         17),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), 'ケースで学ぶ 財務分析力ブラッシュアップ研修 vol.1',                                                                   18),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), 'ケースで学ぶ 財務分析力ブラッシュアップ研修 vol.2',                                                                   19),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '回収不能を防ぐための危ない会社の見分け方',                                                                            20),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '標準化の進め方の基本',                                                                                                21),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), 'マニュアル作成の基本＜作成する際の考え方＞',                                                                          22),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), 'はじめてのヒューマンエラー',                                                                                          23),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［若手向け］リスク感度の高め方',                                                                                      24),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), 'クレーム電話対応',                                                                                                    25),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［若手向け］基礎人事実務',                                                                                            26),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '契約書の見方・作り方の基本',                                                                                          27),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［事務部門向け］経費削減のコツ',                                                                                      28),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '【基礎】経理実務',                                                                                                    29),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), 'マーケティングの基本',                                                                                                30),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '初心者のためのWebマーケティング',                                                                                     31),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '初心者のためのPR・広報',                                                                                              32),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '営業事務の基本',                                                                                                      33),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［法人営業向け］新規開拓営業のコツ＜電話アポ&飛び込み編＞',                                                          34),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［法人営業向け］新規開拓営業のコツ＜商談準備編＞',                                                                   35),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［法人営業向け］新規開拓営業のコツ＜案件フォロー・クロージング編＞',                                                 36),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［法人営業向け］新規開拓営業のコツ＜紹介・リピート編＞',                                                             37),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［法人営業向け］新規開拓営業のコツ＜管理職のための営業管理編＞',                                                     38),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［法人営業向け］提案型営業のコツ',                                                                                   39),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［小売業・サービス業向け］店長リーダーシップ研修',                                                                   40),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［小売業・サービス業向け］販売員ベーシックスキル研修',                                                               41),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［小売業向け］売り場作りの基本',                                                                                      42),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［製造業・商社向け］はじめての在庫管理',                                                                              43),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［製造業・商社向け］はじめての購買管理',                                                                              44),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［製造業向け］はじめての生産管理＜生産計画編＞',                                                                      45),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［製造業向け］はじめての原価管理',                                                                                   46),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［製造業向け］はじめての品質管理',                                                                                   47),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［製造業向け］5S（整理・整頓・清掃・清潔・しつけ）の基本',                                                          48),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［IT業向け］はじめてのプロジェクト管理シリーズ＜入門編＞',                                                           49),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［IT業向け］はじめてのプロジェクト管理シリーズ＜WBS作成編＞',                                                        50),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［IT業向け］はじめてのプロジェクト管理シリーズ＜進捗管理編＞',                                                       51),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［IT業向け］はじめてのプロジェクト管理シリーズ＜品質管理編＞',                                                       52),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［IT業向け］はじめてのプロジェクト管理シリーズ＜リスク管理編＞',                                                     53),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［管理職向け］特訓！管理職が知っておくべき決算書の見方＜決算書から課題を読み解く＞',                                 54),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［経営層・人事向け］人材開発エキスパート養成プログラム',                                                              55),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［現場指導者向け］実践！OJT育成計画立案と仕事の任せ方＜勘と経験だけに頼らない仕事の任せ方＞',                        56),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［現場指導者向け］実践！OJT仕事の振り返りサポート',                                                                  57),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［若手向け］特訓！新人・若手がおさえるべきコンプライアンスのいろは＜情報資産の取り扱い＞',                           58),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［若手向け］特訓！Excelで効率的に資料を作る＜基本操作から基礎的な関数を用いた表計算／簡単なグラフ作成＞',           59),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Business Knowledge'), '［若手向け］特訓！PowerPointで効率的に資料を作る＜基本操作から図・グラフ・写真の取り込み／位置・大きさの調整＞',   60);

-- Self-Management
INSERT INTO ad_seminars (category_id, name, sort_order) VALUES
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), '新任管理職研修',                                                                               1),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), 'ストーリーで学ぶ ビジネスリーダー研修＜リーダーとしてのセルフマネジメント編＞',                2),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), 'ハイパフォーマーが実践するセルフマネジメント研修',                                            3),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), '周囲への影響を考えるセルフリーダーシップ',                                                    4),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), 'エモーショナルマネジメントの基本',                                                            5),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), 'ここからはじめる健康管理',                                                                    6),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), '［中途入社者向け］入社時研修',                                                                7),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), '［若手向け］フォロワーシップの高め方',                                                        8),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), '［若手向け］キャリアデザイン研修',                                                            9),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), '［若手向け］成長するために必要な要素',                                                       10),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), '［若手向け］仕事経験を成長につなげるコツ',                                                   11),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), '［若手向け］自己成長につなげるリフレクション',                                               12),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), '［若手向け］自身の成長を促す自己理解の深め方',                                               13),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), '［若手向け］成長を促進するための知識の拡げ方',                                               14),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), '［若手向け］仕事への向き合い方',                                                             15),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), '［若手向け］仕事に対するセルフマインド醸成',                                                 16),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), '自己成長のための習慣化',                                                                     17),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), '2年目社員としての立ち居振る舞い',                                                            18),
    ((SELECT id FROM ad_seminar_categories WHERE name = 'Self-Management'), '新入社員フォローアップ研修',                                                                 19);

-- =============================================================================
-- 社外セミナー分類
-- =============================================================================
INSERT INTO seminar_categories (name, sort_order) VALUES
    ('技術',     1),
    ('ビジネス', 2),
    ('その他',   3);

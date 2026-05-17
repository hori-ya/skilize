# テーブル定義書

**バージョン**: 1.3.0  
**作成日**: 2026-05-09  
**更新日**: 2026-05-17  
**DB**: PostgreSQL 16.4

関連資料: [ER図](./er-diagram.md) / [データモデル](./data-model.md)

---

## テーブル一覧

| # | テーブル名 | 日本語名 | 説明 |
|---|-----------|--------|------|
| 1 | `fiscal_year_settings` | 年度設定 | 会計年度開始月などのシステム設定（シングルトン） |
| 2 | `fiscal_years` | 年度マスタ | 棚卸の対象年度 |
| 3 | `skill_levels` | レベルマスタ | スキル採点レベルの定義 |
| 4 | `it_skill_categories` | ITスキル分類マスタ | ITスキルの階層分類（最大3階層・自己参照） |
| 5 | `qualification_categories` | 資格分類マスタ | 資格の分類（フラット1階層） |
| 6 | `ad_seminar_categories` | ADセミナー分類マスタ | ADセミナーの分類（フラット1階層） |
| 7 | `seminar_categories` | セミナー分類マスタ | セミナー（AD以外）の分類（フラット1階層） |
| 8 | `it_skills` | ITスキルマスタ | ITスキルの一覧 |
| 9 | `qualifications` | 参考資格マスタ | 参考資格の一覧 |
| 10 | `ad_seminars` | ADマスタ | AD（スキルアップ活動区分）の一覧 |
| 11 | `users` | ユーザー | システムユーザー（TLへの自己参照FK） |
| 12 | `inventories` | 棚卸ヘッダー | ユーザー×年度の棚卸（ITスキル・資格・セミナー共通ヘッダー） |
| 13 | `it_skill_details` | ITスキル棚卸明細 | ITスキルの採点・備考 |
| 14 | `qualification_details` | 資格棚卸明細 | 資格の取得年月・備考 |
| 15 | `seminar_details` | セミナー棚卸明細 | セミナーの受講年月・備考 |
| 16 | `inventory_goals` | 目標設定 | 翌年度の目標（ITスキル / 資格 / AD） |
| 17 | `inventory_interviews` | 面談メモヘッダー | TL/ADMINが棚卸に対して記録する面談メモ（入力者×棚卸で1件） |
| 18 | `interview_detail_notes` | 面談メモ明細 | 各棚卸明細行（ITスキル・資格・セミナー・目標）に紐づく明細レベルのメモ |
| 19 | `user_expectations` | ユーザーへの期待 | TL期待コメント・会社期待コメントをユーザーごとに1行で管理 |
| 20 | `ai_career_analyses` | AIキャリア分析 | AI分析結果をユーザー×年度ごとに保存 |

---

## 1. fiscal_year_settings（年度設定）

会計年度開始月などのシステム全体設定。常に1レコードのみ存在する。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SMALLINT` | ○ | — | PK（常に値 1） |
| 2 | fiscal_year_start_month | `SMALLINT` | ○ | `4` | 会計年度開始月（1〜12） |
| 3 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 最終更新日時 |
| 4 | updated_by | `INTEGER` | — | `NULL` | 更新ユーザーID（FK → users.id） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| FK | updated_by | `users(id)` / NULL 許容 |
| CHECK | fiscal_year_start_month | `BETWEEN 1 AND 12` |

---

## 2. fiscal_years（年度マスタ）

棚卸の対象年度を管理する。削除は提供しない（棚卸データとの整合性確保のため）。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | name | `VARCHAR(20)` | ○ | — | 年度名（例: FY2025） |
| 3 | start_date | `DATE` | ○ | — | 開始日 |
| 4 | end_date | `DATE` | ○ | — | 終了日 |
| 5 | input_start_date | `DATE` | — | `NULL` | 入力推奨開始日（参考情報。入力ロックには使用しない） |
| 6 | input_end_date | `DATE` | — | `NULL` | 入力推奨締切日（参考情報） |
| 7 | is_active | `BOOLEAN` | ○ | `TRUE` | 有効フラグ |
| 8 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 9 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| UNIQUE | name | 年度名の重複禁止 |
| CHECK | — | `start_date < end_date` |

---

## 3. skill_levels（レベルマスタ）

スキル採点に使うレベルを定義する。変更しても既存棚卸データの `skill_level_id` FK は維持される（論理削除のみ）。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | level_value | `SMALLINT` | ○ | — | 採点数値（棚卸データへの保存値） |
| 3 | description | `VARCHAR(200)` | ○ | — | 説明（例: 知識がある） |
| 4 | is_active | `BOOLEAN` | ○ | `TRUE` | 有効フラグ |
| 5 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 6 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| UNIQUE | level_value | 数値の重複禁止 |

> 一覧取得時は `level_value ASC` で固定ソート（sort_order カラムなし）。

---

## 4. it_skill_categories（ITスキル分類マスタ）

ITスキルの階層分類（最大3階層）を自己参照で管理する。`parent_id = NULL` が分類1（ルート）。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | parent_id | `INTEGER` | — | `NULL` | 親分類ID（FK → it_skill_categories.id。NULL が分類1） |
| 3 | level | `SMALLINT` | ○ | — | 階層レベル（1 / 2 / 3） |
| 4 | name | `VARCHAR(100)` | ○ | — | 分類名 |
| 5 | sort_order | `INTEGER` | ○ | `0` | 表示順 |
| 6 | is_active | `BOOLEAN` | ○ | `TRUE` | 有効フラグ |
| 7 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 8 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| FK | parent_id | `it_skill_categories(id)` / NULL 許容 |
| CHECK | level | `IN (1, 2, 3)` |
| IDX | parent_id | ツリー走査用 |

---

## 5. qualification_categories（資格分類マスタ）

資格の分類をフラット（1階層）で管理する。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | name | `VARCHAR(100)` | ○ | — | 分類名 |
| 3 | sort_order | `INTEGER` | ○ | `0` | 表示順 |
| 4 | is_active | `BOOLEAN` | ○ | `TRUE` | 有効フラグ |
| 5 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 6 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| UNIQUE | name | 分類名の重複禁止 |

---

## 6. ad_seminar_categories（ADセミナー分類マスタ）

ADセミナーの分類をフラット（1階層）で管理する。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | name | `VARCHAR(100)` | ○ | — | 分類名 |
| 3 | sort_order | `INTEGER` | ○ | `0` | 表示順 |
| 4 | is_active | `BOOLEAN` | ○ | `TRUE` | 有効フラグ |
| 5 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 6 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| UNIQUE | name | 分類名の重複禁止 |

---

## 7. seminar_categories（セミナー分類マスタ）

セミナー（AD以外）の分類をフラット（1階層）で管理する。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | name | `VARCHAR(100)` | ○ | — | 分類名 |
| 3 | sort_order | `INTEGER` | ○ | `0` | 表示順 |
| 4 | is_active | `BOOLEAN` | ○ | `TRUE` | 有効フラグ |
| 5 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 6 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| UNIQUE | name | 分類名の重複禁止 |

---

## 8. it_skills（ITスキルマスタ）

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | category_id | `INTEGER` | ○ | — | 所属する最下位分類ID（FK → it_skill_categories.id） |
| 3 | name | `VARCHAR(200)` | ○ | — | スキル名 |
| 4 | description | `TEXT` | — | `NULL` | 説明 |
| 5 | sort_order | `INTEGER` | ○ | `0` | 表示順 |
| 6 | is_active | `BOOLEAN` | ○ | `TRUE` | 有効フラグ（論理削除） |
| 7 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 8 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| FK | category_id | `it_skill_categories(id)` |
| IDX | category_id | 分類絞り込み用 |
| IDX | is_active | 有効スキル取得用 |

---

## 9. qualifications（参考資格マスタ）

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | category_id | `INTEGER` | — | `NULL` | 資格分類ID（FK → qualification_categories.id。NULL は未分類） |
| 3 | name | `VARCHAR(200)` | ○ | — | 資格名 |
| 4 | description | `TEXT` | — | `NULL` | 説明 |
| 5 | sort_order | `INTEGER` | ○ | `0` | 表示順 |
| 6 | is_active | `BOOLEAN` | ○ | `TRUE` | 有効フラグ（論理削除） |
| 7 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 8 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| FK | category_id | `qualification_categories(id)` / NULL 許容 |
| IDX | category_id | 分類絞り込み用 |
| IDX | is_active | 有効資格取得用 |

---

## 10. ad_seminars（ADマスタ）

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | category_id | `INTEGER` | — | `NULL` | ADセミナー分類ID（FK → ad_seminar_categories.id。NULL は未分類） |
| 3 | name | `VARCHAR(200)` | ○ | — | AD名 |
| 4 | description | `TEXT` | — | `NULL` | 説明 |
| 5 | sort_order | `INTEGER` | ○ | `0` | 表示順 |
| 6 | is_active | `BOOLEAN` | ○ | `TRUE` | 有効フラグ（論理削除） |
| 7 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 8 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| FK | category_id | `ad_seminar_categories(id)` / NULL 許容 |
| IDX | category_id | 分類絞り込み用 |

---

## 11. users（ユーザー）

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | user_id | `VARCHAR(50)` | ○ | — | ユーザーID（ログインID・英数字・記号使用可） |
| 3 | name | `VARCHAR(100)` | ○ | — | 氏名 |
| 4 | email | `VARCHAR(255)` | — | `NULL` | メールアドレス（任意） |
| 5 | password_hash | `VARCHAR(255)` | ○ | — | パスワードハッシュ（BCrypt） |
| 6 | role | `VARCHAR(10)` | ○ | — | ロール（`GENERAL` / `TL` / `ADMIN`） |
| 7 | tl_user_id | `INTEGER` | — | `NULL` | TLユーザーID（FK → users.id 自己参照） |
| 8 | is_initial_password | `BOOLEAN` | ○ | `TRUE` | 初回ログイン時パスワード変更強制フラグ |
| 9 | is_active | `BOOLEAN` | ○ | `TRUE` | 有効フラグ（論理削除） |
| 10 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 11 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| UNIQUE | user_id | ログインIDの重複禁止 |
| FK | tl_user_id | `users(id)` / NULL 許容 / 自己参照 |
| CHECK | role | `IN ('GENERAL', 'TL', 'ADMIN')` |
| IDX | tl_user_id | チームメンバー検索用 |
| IDX | role | TL / ADMIN 絞り込み用（TL選択プルダウン等） |

> `tl_user_id` は TL または ADMIN ロールのユーザーを指定する（アプリ側で制御）。  
> `email` は任意項目のため NULL を許容する。登録されている場合でも一意性は強制しない。

---

## 12. inventories（棚卸ヘッダー）

ユーザーと年度の組み合わせで 1 レコード。ITスキル・資格・セミナーの各明細の親となる。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | user_id | `INTEGER` | ○ | — | ユーザーID（FK → users.id） |
| 3 | fiscal_year_id | `INTEGER` | ○ | — | 年度ID（FK → fiscal_years.id） |
| 4 | status | `VARCHAR(20)` | ○ | `'DRAFT'` | ステータス（`DRAFT` / `PENDING_GOAL` / `COMPLETED`） |
| 5 | submitted_at | `TIMESTAMPTZ` | — | `NULL` | 棚卸提出日時 |
| 6 | goal_review_completed_at | `TIMESTAMPTZ` | — | `NULL` | 前回目標振り返り完了日時（NULL かつ前年度目標あり → ログイン時に SCR-019 へ誘導） |
| 7 | goal_completed_at | `TIMESTAMPTZ` | — | `NULL` | 目標設定完了日時 |
| 8 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 9 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| FK | user_id | `users(id)` |
| FK | fiscal_year_id | `fiscal_years(id)` |
| UNIQUE | (user_id, fiscal_year_id) | ユーザー×年度で1件のみ |
| CHECK | status | `IN ('DRAFT', 'PENDING_GOAL', 'COMPLETED')` |
| IDX | user_id | ユーザー別棚卸一覧取得用 |
| IDX | fiscal_year_id | 年度別集計用 |
| IDX | status | ダッシュボード集計用 |

---

## 13. it_skill_details（ITスキル棚卸明細）

`it_skill_id` と `custom_skill_name` のどちらか一方が必ず設定される。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | inventory_id | `INTEGER` | ○ | — | 棚卸ヘッダーID（FK → inventories.id） |
| 3 | it_skill_id | `INTEGER` | — | `NULL` | ITスキルID（FK → it_skills.id。NULL はカスタムスキル） |
| 4 | custom_skill_name | `VARCHAR(200)` | — | `NULL` | カスタムスキル名（`it_skill_id` が NULL の場合に使用） |
| 5 | skill_level_id | `INTEGER` | ○ | — | レベルID（FK → skill_levels.id） |
| 6 | remarks | `TEXT` | — | `NULL` | 備考（採点根拠） |
| 7 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 8 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| FK | inventory_id | `inventories(id)` |
| FK | it_skill_id | `it_skills(id)` / NULL 許容 |
| FK | skill_level_id | `skill_levels(id)` |
| CHECK | — | `it_skill_id IS NOT NULL OR custom_skill_name IS NOT NULL` |
| IDX | inventory_id | 明細一括取得用 |
| IDX | it_skill_id | カスタムスキル昇格時の自動紐付け用 |

---

## 14. qualification_details（資格棚卸明細）

`qualification_id` と `custom_qualification_name` のどちらか一方が必ず設定される。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | inventory_id | `INTEGER` | ○ | — | 棚卸ヘッダーID（FK → inventories.id） |
| 3 | qualification_id | `INTEGER` | — | `NULL` | 資格ID（FK → qualifications.id。NULL はカスタム資格） |
| 4 | custom_qualification_name | `VARCHAR(200)` | — | `NULL` | カスタム資格名（`qualification_id` が NULL の場合に使用） |
| 5 | acquired_year_month | `DATE` | — | `NULL` | 取得年月（月初日で保存。未取得は NULL） |
| 6 | remarks | `TEXT` | — | `NULL` | 備考（取得理由） |
| 7 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 8 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| FK | inventory_id | `inventories(id)` |
| FK | qualification_id | `qualifications(id)` / NULL 許容 |
| CHECK | — | `qualification_id IS NOT NULL OR custom_qualification_name IS NOT NULL` |
| IDX | inventory_id | 明細一括取得用 |
| IDX | qualification_id | カスタム資格昇格時の自動紐付け用 |

---

## 15. seminar_details（セミナー棚卸明細）

`ad_seminar_id` と `seminar_name` のどちらか一方が必ず設定される。`seminar_category_id` はセミナー（AD以外、`seminar_name` 使用時）にのみ設定する。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | inventory_id | `INTEGER` | ○ | — | 棚卸ヘッダーID（FK → inventories.id） |
| 3 | ad_seminar_id | `INTEGER` | — | `NULL` | ADセミナーID（FK → ad_seminars.id。NULL はAD以外のセミナー） |
| 4 | seminar_name | `VARCHAR(200)` | — | `NULL` | セミナー名（AD以外のセミナーで使用） |
| 5 | seminar_category_id | `INTEGER` | — | `NULL` | セミナー分類ID（FK → seminar_categories.id。AD以外のセミナー時のみ使用） |
| 6 | attended_year_month | `DATE` | — | `NULL` | 受講年月（月初日で保存。未受講は NULL） |
| 7 | remarks | `TEXT` | — | `NULL` | 備考（受講理由・振り返り） |
| 8 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 9 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| FK | inventory_id | `inventories(id)` |
| FK | ad_seminar_id | `ad_seminars(id)` / NULL 許容 |
| FK | seminar_category_id | `seminar_categories(id)` / NULL 許容 |
| CHECK | — | `ad_seminar_id IS NOT NULL OR seminar_name IS NOT NULL` |
| IDX | inventory_id | 明細一括取得用 |

---

## 16. inventory_goals（目標設定）

目標カテゴリ (`goal_category`) に応じて参照先 FK が変わる。カスタム目標は `custom_name` を使用。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | inventory_id | `INTEGER` | ○ | — | 棚卸ヘッダーID（FK → inventories.id） |
| 3 | goal_category | `VARCHAR(20)` | ○ | — | 目標カテゴリ（`IT_SKILL` / `QUALIFICATION` / `AD`） |
| 4 | it_skill_id | `INTEGER` | — | `NULL` | ITスキルID（FK → it_skills.id） |
| 5 | qualification_id | `INTEGER` | — | `NULL` | 資格ID（FK → qualifications.id） |
| 6 | ad_seminar_id | `INTEGER` | — | `NULL` | ADセミナーID（FK → ad_seminars.id） |
| 7 | custom_name | `VARCHAR(200)` | — | `NULL` | カスタムスキル・資格目標の自由テキスト |
| 8 | target_period | `DATE` | ○ | — | 達成・予定時期（月初日で保存） |
| 9 | reason | `TEXT` | — | `NULL` | 理由 |
| 10 | achievement_status | `VARCHAR(20)` | — | `NULL` | 達成状況（翌年度の振り返り時に記録。`ACHIEVED` / `PARTIAL` / `NOT_ACHIEVED`） |
| 11 | review_note | `TEXT` | — | `NULL` | 振り返りコメント（翌年度の振り返り時に記録） |
| 12 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 13 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| FK | inventory_id | `inventories(id)` |
| FK | it_skill_id | `it_skills(id)` / NULL 許容 |
| FK | qualification_id | `qualifications(id)` / NULL 許容 |
| FK | ad_seminar_id | `ad_seminars(id)` / NULL 許容 |
| CHECK | goal_category | `IN ('IT_SKILL', 'QUALIFICATION', 'AD')` |
| CHECK | — | `it_skill_id IS NOT NULL OR qualification_id IS NOT NULL OR ad_seminar_id IS NOT NULL OR custom_name IS NOT NULL` |
| CHECK | achievement_status | `IN ('ACHIEVED', 'PARTIAL', 'NOT_ACHIEVED')` / NULL 許容 |
| IDX | inventory_id | 目標一覧取得用 |

### goal_category と FK の対応

| goal_category | 使用する FK / フィールド |
|--------------|------------------------|
| `IT_SKILL` | `it_skill_id`（マスタ参照）または `custom_name`（カスタム） |
| `QUALIFICATION` | `qualification_id`（マスタ参照）または `custom_name`（カスタム） |
| `AD` | `ad_seminar_id`（必須） |

---

---

## 17. inventory_interviews（面談メモヘッダー）

TL/ADMINが棚卸ヘッダーに対して記録する面談メモ。  
`(inventory_id, interviewer_id)` のユニーク制約により、同一棚卸に対して各TL/ADMINが独立した面談メモを持つ。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | inventory_id | `INTEGER` | ○ | — | 棚卸ヘッダーID（FK → inventories.id） |
| 3 | interviewer_id | `INTEGER` | ○ | — | 面談メモを記入したユーザーID（FK → users.id） |
| 4 | general_note | `TEXT` | — | `NULL` | 全体備忘録（面談全体の総括メモ） |
| 5 | interviewed_at | `DATE` | — | `NULL` | 面談実施日 |
| 6 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 7 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| FK | inventory_id | `inventories(id)` |
| FK | interviewer_id | `users(id)` |
| UNIQUE | (inventory_id, interviewer_id) | 同一棚卸に対して同一入力者のメモは1件のみ |
| IDX | inventory_id | 棚卸別メモ検索用 |
| IDX | interviewer_id | 入力者別メモ検索用 |

> `interviewer_id` には TL または ADMIN ロールのユーザーのみが設定される（アプリ側で制御）。

---

## 18. interview_detail_notes（面談メモ明細）

棚卸の各明細行（ITスキル・資格・セミナー・目標）に1対1で紐づく明細レベルの面談メモ。  
`(interview_id, detail_type, detail_id)` のユニーク制約により、1面談メモヘッダー内で同一明細へのメモは1件のみ。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | interview_id | `INTEGER` | ○ | — | 面談メモヘッダーID（FK → inventory_interviews.id） |
| 3 | detail_type | `VARCHAR(20)` | ○ | — | 明細種別（`IT_SKILL` / `QUALIFICATION` / `SEMINAR` / `GOAL`） |
| 4 | detail_id | `INTEGER` | ○ | — | 対象明細のID（`detail_type` に応じた各明細テーブルのID） |
| 5 | note | `TEXT` | ○ | — | 面談メモ内容 |
| 6 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 7 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| FK | interview_id | `inventory_interviews(id)` |
| UNIQUE | (interview_id, detail_type, detail_id) | 同一明細へのメモは1件のみ |
| CHECK | detail_type | `IN ('IT_SKILL', 'QUALIFICATION', 'SEMINAR', 'GOAL')` |
| IDX | interview_id | メモ一覧取得用 |

### detail_type と detail_id の対応

| detail_type | detail_id の参照先 |
|-------------|-------------------|
| `IT_SKILL` | `it_skill_details.id` |
| `QUALIFICATION` | `qualification_details.id` |
| `SEMINAR` | `seminar_details.id` |
| `GOAL` | `inventory_goals.id` |

> `detail_id` は FK 制約を設定しない（`detail_type` に応じて参照先テーブルが変わるため）。アプリ側で存在チェックを実施する。

---

## 19. user_expectations（ユーザーへの期待）

ユーザーごとに TL 期待コメントと会社期待コメントを 1 行で管理する。レコードが存在しない場合は「未入力」として扱い、初回保存時に生成する。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | user_id | `INTEGER` | ○ | — | PK / FK → users.id |
| 2 | tl_expectation | `TEXT` | — | `NULL` | TLが期待すること（TL/ADMIN が入力） |
| 3 | company_expectation | `TEXT` | — | `NULL` | 会社が期待すること（ADMIN のみ入力） |
| 4 | tl_updated_at | `TIMESTAMPTZ` | — | `NULL` | TL期待最終更新日時 |
| 5 | company_updated_at | `TIMESTAMPTZ` | — | `NULL` | 会社期待最終更新日時 |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | user_id | — |
| FK | user_id | `users(id)` |

---

## 20. ai_career_analyses（AIキャリア分析）

AI によるキャリア分析結果をユーザー×年度ごとに保存するテーブル。  
棚卸ステータスが `COMPLETED` に遷移した際にバックグラウンドで自動生成される。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK（自動採番） |
| 2 | user_id | `INTEGER` | ○ | — | FK → users.id |
| 3 | fiscal_year_id | `INTEGER` | ○ | — | FK → fiscal_years.id |
| 4 | status | `VARCHAR(20)` | ○ | `'PENDING'` | 処理ステータス（PENDING / PROCESSING / COMPLETED / FAILED） |
| 5 | analysis_result | `JSONB` | — | `NULL` | LLM が生成した分析結果 JSON（status=COMPLETED 時のみ格納） |
| 6 | error_message | `TEXT` | — | `NULL` | エラー内容（status=FAILED 時のみ格納） |
| 7 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 8 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 最終更新日時 |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| UNIQUE | (user_id, fiscal_year_id) | ユーザー×年度で1レコードのみ |
| FK | user_id | `users(id)` |
| FK | fiscal_year_id | `fiscal_years(id)` |
| CHECK | status | `IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')` |
| INDEX | user_id, fiscal_year_id | 検索・API 取得用 |

### analysis_result JSON 構造

```json
{
  "summary": "string",
  "strengths": ["string"],
  "growth_areas": ["string"],
  "expectation_fit": "string",
  "recommended_actions": ["string"]
}
```

---

## 補足

### updated_at の自動更新

`updated_at` カラムは UPDATE 時にトリガーで自動更新する（`init.sql` 参照）。

### 年月の保存方法

`acquired_year_month`・`attended_year_month`・`target_period` は `DATE` 型で保存し、常に**月初日（1日）**を格納する。  
例：2025年4月 → `2025-04-01`

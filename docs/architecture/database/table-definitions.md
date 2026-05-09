# テーブル定義書

**バージョン**: 1.0.0  
**作成日**: 2026-05-09  
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
| 5 | `it_skills` | ITスキルマスタ | ITスキルの一覧 |
| 6 | `qualifications` | 参考資格マスタ | 参考資格の一覧 |
| 7 | `ad_seminars` | ADマスタ | AD（スキルアップ活動区分）の一覧 |
| 8 | `users` | ユーザー | システムユーザー（TLへの自己参照FK） |
| 9 | `inventories` | 棚卸ヘッダー | ユーザー×年度の棚卸（ITスキル・資格・セミナー共通ヘッダー） |
| 10 | `it_skill_details` | ITスキル棚卸明細 | ITスキルの採点・備考 |
| 11 | `qualification_details` | 資格棚卸明細 | 資格の取得年月・備考 |
| 12 | `seminar_details` | セミナー棚卸明細 | セミナーの受講年月・備考 |
| 13 | `inventory_goals` | 目標設定 | 翌年度の目標（ITスキル / 資格 / AD） |

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

## 5. it_skills（ITスキルマスタ）

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

## 6. qualifications（参考資格マスタ）

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | name | `VARCHAR(200)` | ○ | — | 資格名 |
| 3 | description | `TEXT` | — | `NULL` | 説明 |
| 4 | sort_order | `INTEGER` | ○ | `0` | 表示順 |
| 5 | is_active | `BOOLEAN` | ○ | `TRUE` | 有効フラグ（論理削除） |
| 6 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 7 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| IDX | is_active | 有効資格取得用 |

---

## 7. ad_seminars（ADマスタ）

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | name | `VARCHAR(200)` | ○ | — | AD名 |
| 3 | description | `TEXT` | — | `NULL` | 説明 |
| 4 | sort_order | `INTEGER` | ○ | `0` | 表示順 |
| 5 | is_active | `BOOLEAN` | ○ | `TRUE` | 有効フラグ（論理削除） |
| 6 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 7 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |

---

## 8. users（ユーザー）

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

## 9. inventories（棚卸ヘッダー）

ユーザーと年度の組み合わせで 1 レコード。ITスキル・資格・セミナーの各明細の親となる。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | user_id | `INTEGER` | ○ | — | ユーザーID（FK → users.id） |
| 3 | fiscal_year_id | `INTEGER` | ○ | — | 年度ID（FK → fiscal_years.id） |
| 4 | status | `VARCHAR(20)` | ○ | `'DRAFT'` | ステータス（`DRAFT` / `PENDING_GOAL` / `COMPLETED`） |
| 5 | submitted_at | `TIMESTAMPTZ` | — | `NULL` | 棚卸提出日時 |
| 6 | goal_completed_at | `TIMESTAMPTZ` | — | `NULL` | 目標設定完了日時 |
| 7 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 8 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

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

## 10. it_skill_details（ITスキル棚卸明細）

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

## 11. qualification_details（資格棚卸明細）

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

## 12. seminar_details（セミナー棚卸明細）

`ad_seminar_id` と `seminar_name` のどちらか一方が必ず設定される。

### カラム定義

| # | カラム名 | データ型 | NOT NULL | デフォルト | 説明 |
|---|---------|---------|:--------:|-----------|------|
| 1 | id | `SERIAL` | ○ | — | PK |
| 2 | inventory_id | `INTEGER` | ○ | — | 棚卸ヘッダーID（FK → inventories.id） |
| 3 | ad_seminar_id | `INTEGER` | — | `NULL` | ADセミナーID（FK → ad_seminars.id。NULL はフリーセミナー） |
| 4 | seminar_name | `VARCHAR(200)` | — | `NULL` | セミナー名（フリーセミナーで使用） |
| 5 | attended_year_month | `DATE` | — | `NULL` | 受講年月（月初日で保存。未受講は NULL） |
| 6 | remarks | `TEXT` | — | `NULL` | 備考（受講理由・振り返り） |
| 7 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 8 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

### 制約・インデックス

| 種別 | 対象 | 内容 |
|------|------|------|
| PK | id | — |
| FK | inventory_id | `inventories(id)` |
| FK | ad_seminar_id | `ad_seminars(id)` / NULL 許容 |
| CHECK | — | `ad_seminar_id IS NOT NULL OR seminar_name IS NOT NULL` |
| IDX | inventory_id | 明細一括取得用 |

---

## 13. inventory_goals（目標設定）

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
| 10 | created_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 作成日時 |
| 11 | updated_at | `TIMESTAMPTZ` | ○ | `CURRENT_TIMESTAMP` | 更新日時（自動更新） |

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
| IDX | inventory_id | 目標一覧取得用 |

### goal_category と FK の対応

| goal_category | 使用する FK / フィールド |
|--------------|------------------------|
| `IT_SKILL` | `it_skill_id`（マスタ参照）または `custom_name`（カスタム） |
| `QUALIFICATION` | `qualification_id`（マスタ参照）または `custom_name`（カスタム） |
| `AD` | `ad_seminar_id`（必須） |

---

## 補足

### updated_at の自動更新

`updated_at` カラムは UPDATE 時にトリガーで自動更新する（`init.sql` 参照）。

### 年月の保存方法

`acquired_year_month`・`attended_year_month`・`target_period` は `DATE` 型で保存し、常に**月初日（1日）**を格納する。  
例：2025年4月 → `2025-04-01`

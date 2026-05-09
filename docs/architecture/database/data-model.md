# データモデル（概念設計）

**バージョン**: 1.0.0  
**作成日**: 2026-05-09

関連資料：[ER図](./er-diagram.md)

---

## エンティティ一覧

### 設定・年度

```
FiscalYearSettings（年度設定）
  └─ fiscal_year_start_month（会計年度開始月。デフォルト4月）

FiscalYear（年度マスタ）
  ├─ id
  ├─ name（例：FY2025）
  ├─ start_date / end_date
  ├─ input_start_date / input_end_date（参考情報。入力ロックには使用しない）
  └─ is_active
```

### マスタ

```
SkillLevel（レベルマスタ）
  ├─ id
  ├─ level_value（数値。採点値として棚卸データに保存）
  ├─ description（例：知識がある / 経験がある / 実践できる）
  └─ is_active

ITSkillCategory（ITスキル分類マスタ。自己参照で最大3階層）
  ├─ id
  ├─ parent_id → ITSkillCategory（NULLが分類1ルート）
  ├─ level（1 / 2 / 3）
  ├─ name
  ├─ sort_order
  └─ is_active

ITSkill（ITスキルマスタ）
  ├─ id
  ├─ category_id → ITSkillCategory（所属する最下位分類）
  ├─ name
  ├─ description
  ├─ sort_order
  └─ is_active

Qualification（参考資格マスタ）
  ├─ id
  ├─ name
  ├─ description
  ├─ sort_order
  └─ is_active

ADSeminar（ADマスタ）
  ├─ id
  ├─ name
  ├─ description
  ├─ sort_order
  └─ is_active
```

### ユーザー

```
User（ユーザー）
  ├─ id
  ├─ user_id（ログインID。一意・変更不可）
  ├─ name
  ├─ email（nullable：任意。一意性強制なし）
  ├─ password_hash（初期値はuser_idと同一をBCryptでハッシュ化）
  ├─ role（GENERAL / TL / ADMIN）
  ├─ tl_user_id → User（nullable：TLユーザーへの自己参照FK）
  ├─ is_initial_password（初回ログイン時パスワード変更強制フラグ）
  └─ is_active
```

### 棚卸

```
Inventory（棚卸ヘッダー。ITスキル・資格・セミナー共通）
  ├─ id
  ├─ user_id → User
  ├─ fiscal_year_id → FiscalYear
  ├─ status（DRAFT / PENDING_GOAL / COMPLETED）
  ├─ submitted_at（棚卸提出日時）
  └─ goal_completed_at（目標設定完了日時）

ITSkillDetail（ITスキル棚卸明細）
  ├─ id
  ├─ inventory_id → Inventory
  ├─ it_skill_id → ITSkill（nullable：NULLはカスタムスキル）
  ├─ custom_skill_name（it_skill_id=NULLの場合に使用）
  ├─ skill_level_id → SkillLevel（採点。論理削除のみのため過去データの参照は維持される）
  └─ remarks（備考：採点根拠）

QualificationDetail（資格棚卸明細）
  ├─ id
  ├─ inventory_id → Inventory
  ├─ qualification_id → Qualification（nullable：NULLはカスタム資格）
  ├─ custom_qualification_name（qualification_id=NULLの場合に使用）
  ├─ acquired_year_month（取得年月。nullable）
  └─ remarks（備考：取得理由）

SeminarDetail（セミナー棚卸明細）
  ├─ id
  ├─ inventory_id → Inventory
  ├─ ad_seminar_id → ADSeminar（nullable：NULLはフリーセミナー）
  ├─ seminar_name（ad_seminar_id=NULLの場合に使用）
  ├─ attended_year_month（受講年月。nullable）
  └─ remarks（備考：受講理由・振り返り）
```

### 目標設定

```
InventoryGoal（目標設定）
  ├─ id
  ├─ inventory_id → Inventory
  ├─ goal_category（IT_SKILL / QUALIFICATION / AD）
  ├─ it_skill_id → ITSkill（nullable：goal_category=IT_SKILLかつマスタ参照の場合）
  ├─ qualification_id → Qualification（nullable：goal_category=QUALIFICATIONかつマスタ参照の場合）
  ├─ ad_seminar_id → ADSeminar（nullable：goal_category=ADの場合）
  ├─ custom_name（カスタムスキル・資格を目標とする場合の自由テキスト）
  ├─ target_period（達成・予定時期：年月）
  └─ reason（理由）
```

---

## NULLを許容するFKの意味

| テーブル | カラム | NULLの意味 |
|----------|--------|-----------|
| `it_skill_details` | `it_skill_id` | カスタムスキル（`custom_skill_name` を使用） |
| `qualification_details` | `qualification_id` | カスタム資格（`custom_qualification_name` を使用） |
| `seminar_details` | `ad_seminar_id` | フリーセミナー（`seminar_name` を使用） |
| `inventory_goals` | `it_skill_id` / `qualification_id` / `ad_seminar_id` | `goal_category` に応じて1つのみ設定。カスタム目標は `custom_name` を使用 |
| `users` | `tl_user_id` | TL未設定ユーザー |
| `users` | `email` | メールアドレス未登録ユーザー（任意項目） |

---

## 年月の保存方法

`acquired_year_month`・`attended_year_month`・`target_period` は `date` 型で保存し、常に**月初日（1日）**を格納する。  
例：2025年4月 → `2025-04-01`

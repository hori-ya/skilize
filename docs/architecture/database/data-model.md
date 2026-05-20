# データモデル（概念設計）

**バージョン**: 1.4.0  
**作成日**: 2026-05-09  
**更新日**: 2026-05-21

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
  ├─ score_weight（グラフスコア計算に使用する重み値。0 はスコアに寄与しない）
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

QualificationCategory（資格分類マスタ。フラット1階層）
  ├─ id
  ├─ name
  ├─ sort_order
  └─ is_active

Qualification（参考資格マスタ）
  ├─ id
  ├─ category_id → QualificationCategory（nullable：NULL は未分類）
  ├─ name
  ├─ description
  ├─ sort_order
  └─ is_active

ADSeminarCategory（ADセミナー分類マスタ。フラット1階層）
  ├─ id
  ├─ name
  ├─ sort_order
  └─ is_active

ADSeminar（ADマスタ）
  ├─ id
  ├─ category_id → ADSeminarCategory（nullable：NULL は未分類）
  ├─ name
  ├─ description
  ├─ sort_order
  └─ is_active

SeminarCategory（セミナー分類マスタ。フラット1階層。AD以外のセミナーに適用）
  ├─ id
  ├─ name
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
  ├─ goal_review_completed_at（前回目標振り返り完了日時。nullable。NULLかつ前年度目標ありの場合、ログイン時にSCR-019へ誘導）
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
  ├─ ad_seminar_id → ADSeminar（nullable：NULLはAD以外のセミナー）
  ├─ seminar_name（ad_seminar_id=NULLの場合に使用）
  ├─ seminar_category_id → SeminarCategory（nullable：AD以外のセミナー時のみ。ADセミナーの分類はad_seminars.category_idで管理）
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
  ├─ reason（理由）
  ├─ achievement_status（達成状況。翌年度の振り返り時に記録。ACHIEVED / PARTIAL / NOT_ACHIEVED。nullable）
  └─ review_note（振り返りコメント。翌年度の振り返り時に記録。nullable）
```

### 面談メモ

```
InventoryInterview（面談メモヘッダー）
  ├─ id
  ├─ inventory_id → Inventory（面談対象の棚卸）
  ├─ interviewer_id → User（入力したTL/ADMIN。UNIQUEキーの一部：同一棚卸に対して入力者ごとに1件）
  └─ general_note（全体備忘録。nullable）

InterviewDetailNote（面談メモ明細）
  ├─ id
  ├─ interview_id → InventoryInterview
  ├─ detail_type（IT_SKILL / QUALIFICATION / SEMINAR / GOAL）
  ├─ detail_id（対象明細のID。detail_type に応じた各明細テーブルのPK）
  └─ note（メモ内容）
```

> `InventoryInterview` の UNIQUE 制約は `(inventory_id, interviewer_id)` の複合。  
> 同一棚卸に対して複数のTL/ADMINが独立した面談メモを記録できる。  
> 各TL/ADMINは自分の `interviewer_id` に紐づくレコードのみ照会・編集可能。

### 期待コメント

```
UserExpectation（ユーザーへの期待コメント）
  ├─ user_id → User（PK兼FK。ユーザーごとに1レコード）
  ├─ tl_expectation（TLが期待すること。nullable。担当TLのみ編集可）
  ├─ company_expectation（会社が期待すること。nullable。ADMINのみ編集可）
  ├─ tl_updated_at（TL期待コメントの最終更新日時。nullable）
  └─ company_updated_at（会社期待コメントの最終更新日時。nullable）
```

### AIキャリア分析

```
AiCareerAnalysis（AIキャリア分析）
  ├─ id
  ├─ user_id → User
  ├─ fiscal_year_id → FiscalYear（UNIQUE: user_id × fiscal_year_id）
  ├─ status（PENDING / PROCESSING / COMPLETED / FAILED）
  ├─ analysis_result（JSONB。nullable。COMPLETED 時のみ格納）
  │   ├─ summary（全体総括メッセージ）
  │   ├─ strengths（強み・成長点のリスト）
  │   ├─ growth_areas（伸び代・注力領域のリスト）
  │   ├─ expectation_fit（期待との整合性コメント）
  │   └─ recommended_actions（ネクストステップのリスト）
  └─ error_message（nullable。FAILED 時のみ格納）
```

> UNIQUE 制約は `(user_id, fiscal_year_id)` の複合。ユーザー×年度で最大1レコード。  
> 棚卸が `COMPLETED` に遷移するたびに UPSERT（再提出時は上書き）される。  
> Python（FastAPI + LangChain）モジュールが非同期で生成・更新する。

---

> `user_expectations` はユーザーごとに最大1レコード。  
> TL期待コメントはそのユーザーに割り当てられたTL（`users.tl_user_id`）のみ編集可能。  
> 会社期待コメントはADMINのみ編集可能。  
> TL・ADMINはどちらも両方のコメントを読み取り専用で参照できる。

---

## NULLを許容するFKの意味

| テーブル | カラム | NULLの意味 |
|----------|--------|-----------|
| `qualifications` | `category_id` | 未分類 |
| `ad_seminars` | `category_id` | 未分類 |
| `it_skill_details` | `it_skill_id` | カスタムスキル（`custom_skill_name` を使用） |
| `qualification_details` | `qualification_id` | カスタム資格（`custom_qualification_name` を使用） |
| `seminar_details` | `ad_seminar_id` | AD以外のセミナー（`seminar_name` を使用） |
| `seminar_details` | `seminar_category_id` | ADセミナーの場合（ADの分類は `ad_seminars.category_id` で管理）、または未分類のセミナー |
| `inventory_goals` | `it_skill_id` / `qualification_id` / `ad_seminar_id` | `goal_category` に応じて1つのみ設定。カスタム目標は `custom_name` を使用 |
| `users` | `tl_user_id` | TL未設定ユーザー |
| `users` | `email` | メールアドレス未登録ユーザー（任意項目） |
| `user_expectations` | `tl_expectation` | TLによる期待コメント未入力 |
| `user_expectations` | `company_expectation` | 管理者による期待コメントが未入力 |

---

## 年月の保存方法

`acquired_year_month`・`attended_year_month`・`target_period` は `date` 型で保存し、常に**月初日（1日）**を格納する。  
例：2025年4月 → `2025-04-01`

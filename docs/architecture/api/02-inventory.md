# 棚卸・目標設定 API

**対象画面**: SCR-002（ダッシュボード）、SCR-003（棚卸入力）、SCR-004（前年度比較）、SCR-019（前回目標の振り返り）、SCR-005（目標設定）

---

## エンドポイント一覧

| メソッド | パス | 概要 | 権限 |
|---------|------|------|------|
| GET | /api/dashboard | ダッシュボード情報取得 | 全員 |
| GET | /api/inventories/mine | 自分の棚卸一覧（全年度） | 全員 |
| POST | /api/inventories | 棚卸ヘッダー新規作成 | 全員 |
| GET | /api/inventories/{id} | 棚卸ヘッダー取得 | 全員（※） |
| PUT | /api/inventories/{id}/it-skill-details | ITスキル明細 一括保存 | 全員（※） |
| PUT | /api/inventories/{id}/qualification-details | 資格明細 一括保存 | 全員（※） |
| PUT | /api/inventories/{id}/seminar-details | セミナー明細 一括保存 | 全員（※） |
| POST | /api/inventories/{id}/submit | 棚卸を提出する | 全員（※） |
| GET | /api/inventories/{id}/comparison | 前年度比較データ取得 | 全員（※） |
| PATCH | /api/inventories/{id}/it-skill-details/{detailId} | IT スキル明細の備考更新 | 全員（※） |
| GET | /api/inventories/{id}/goal-review | 前回目標の振り返りデータ取得 | 全員（※） |
| PUT | /api/inventories/{id}/goal-review | 前回目標の振り返り 一括保存 | 全員（※） |
| POST | /api/inventories/{id}/goal-review/complete | 前回目標の振り返りを完了する | 全員（※） |
| GET | /api/inventories/{id}/goals | 目標一覧取得 | 全員（※） |
| PUT | /api/inventories/{id}/goals | 目標 一括保存 | 全員（※） |
| POST | /api/inventories/{id}/goals/complete | 目標設定を完了する | 全員（※） |

> （※）自分の棚卸のみ操作可。他ユーザーの棚卸への書き込みは不可（403）。  
> 他ユーザーの棚卸参照は TL / ADMIN のみ（→ 03-inquiry.md）。

---

## GET /api/dashboard

SCR-002 ダッシュボード表示に必要な当年度棚卸の状況を返す。

**権限**: 全員

**Response 200**

```json
{
  "user": {
    "id": 1,
    "name": "田中 太郎",
    "role": "GENERAL"
  },
  "currentFiscalYear": {
    "id": 1,
    "name": "FY2025"
  },
  "currentInventory": {
    "id": 10,
    "status": "DRAFT",
    "itSkillCount": 5,
    "qualificationCount": 0,
    "seminarCount": 2,
    "submittedAt": null,
    "goalCompletedAt": null
  }
}
```

> `currentInventory` は当年度の棚卸が存在しない場合 `null`。

---

## GET /api/inventories/mine

自分の棚卸一覧を全年度分返す（SCR-006 年度選択用）。

**権限**: 全員

**Response 200**

```json
[
  {
    "id": 10,
    "fiscalYear": { "id": 1, "name": "FY2025" },
    "status": "COMPLETED",
    "submittedAt": "2025-04-20T10:00:00Z",
    "goalCompletedAt": "2025-04-21T09:30:00Z"
  },
  {
    "id": 7,
    "fiscalYear": { "id": 2, "name": "FY2024" },
    "status": "COMPLETED",
    "submittedAt": "2024-04-18T10:00:00Z",
    "goalCompletedAt": "2024-04-19T11:00:00Z"
  }
]
```

---

## POST /api/inventories

当年度の棚卸ヘッダーを新規作成する。年度ごとに 1 件のみ作成可。

**権限**: 全員

**Request Body**

```json
{
  "fiscalYearId": 1
}
```

**Response 201**

```json
{
  "id": 10,
  "fiscalYear": { "id": 1, "name": "FY2025" },
  "status": "DRAFT",
  "submittedAt": null,
  "goalCompletedAt": null
}
```

**Response 409**

```json
{ "code": "CONFLICT", "message": "当該年度の棚卸はすでに作成されています" }
```

---

## GET /api/inventories/{id}

棚卸ヘッダー情報を取得する。

**権限**: 全員（自分の棚卸のみ。TL / ADMIN は他ユーザーも可）

**Path Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `id` | int | 棚卸 ID |

**Response 200**

```json
{
  "id": 10,
  "userId": 1,
  "fiscalYear": { "id": 1, "name": "FY2025" },
  "status": "DRAFT",
  "submittedAt": null,
  "goalCompletedAt": null,
  "createdAt": "2025-04-01T09:00:00Z",
  "updatedAt": "2025-04-15T10:30:00Z"
}
```

---

## PUT /api/inventories/{id}/it-skill-details

ITスキル明細を一括で保存（upsert）する。送信した内容で全明細を上書きする。

**権限**: 全員（自分の棚卸のみ）

**Request Body**

```json
{
  "items": [
    {
      "id": 100,
      "itSkillId": 5,
      "customSkillName": null,
      "skillLevelId": 3,
      "remarks": "業務で Spring Boot を 3 年使用"
    },
    {
      "id": null,
      "itSkillId": null,
      "customSkillName": "Terraform",
      "skillLevelId": 2,
      "remarks": ""
    }
  ]
}
```

> `id: null` は新規追加。`itSkillId: null` かつ `customSkillName` 有でカスタムスキル。

**Response 200**

```json
{
  "items": [
    {
      "id": 100,
      "itSkillId": 5,
      "itSkillName": "Java",
      "customSkillName": null,
      "skillLevelId": 3,
      "levelValue": 3,
      "remarks": "業務で Spring Boot を 3 年使用"
    },
    {
      "id": 201,
      "itSkillId": null,
      "itSkillName": null,
      "customSkillName": "Terraform",
      "skillLevelId": 2,
      "levelValue": 2,
      "remarks": ""
    }
  ]
}
```

---

## PUT /api/inventories/{id}/qualification-details

資格明細を一括で保存（upsert）する。

**権限**: 全員（自分の棚卸のみ）

**Request Body**

```json
{
  "items": [
    {
      "id": 50,
      "qualificationId": 1,
      "customQualificationName": null,
      "acquiredYearMonth": "2023-06-01",
      "remarks": "独学で取得"
    },
    {
      "id": null,
      "qualificationId": null,
      "customQualificationName": "LPIC-1",
      "acquiredYearMonth": "2025-01-01",
      "remarks": ""
    }
  ]
}
```

**Response 200**

```json
{
  "items": [
    {
      "id": 50,
      "qualificationId": 1,
      "qualificationName": "基本情報技術者試験",
      "customQualificationName": null,
      "acquiredYearMonth": "2023-06-01",
      "remarks": "独学で取得"
    },
    {
      "id": 202,
      "qualificationId": null,
      "qualificationName": null,
      "customQualificationName": "LPIC-1",
      "acquiredYearMonth": "2025-01-01",
      "remarks": ""
    }
  ]
}
```

---

## PUT /api/inventories/{id}/seminar-details

セミナー明細を一括で保存（upsert）する。

**権限**: 全員（自分の棚卸のみ）

**Request Body**

```json
{
  "items": [
    {
      "id": 30,
      "adSeminarId": 1,
      "seminarName": null,
      "seminarCategoryId": null,
      "attendedYearMonth": "2025-06-01",
      "remarks": "チームで参加"
    },
    {
      "id": null,
      "adSeminarId": null,
      "seminarName": "社外セミナー AWS re:Invent",
      "seminarCategoryId": 1,
      "attendedYearMonth": "2025-11-01",
      "remarks": ""
    }
  ]
}
```

> `seminarCategoryId` はセミナー（`adSeminarId: null`、AD以外）時のみ有効。ADセミナー時は無視される。

**Response 200**

```json
{
  "items": [
    {
      "id": 30,
      "adSeminarId": 1,
      "adSeminarName": "マネジメント基礎",
      "adSeminarCategoryId": 1,
      "adSeminarCategoryName": "マネジメント",
      "seminarName": null,
      "seminarCategoryId": null,
      "seminarCategoryName": null,
      "attendedYearMonth": "2025-06-01",
      "remarks": "チームで参加"
    },
    {
      "id": 203,
      "adSeminarId": null,
      "adSeminarName": null,
      "adSeminarCategoryId": null,
      "adSeminarCategoryName": null,
      "seminarName": "社外セミナー AWS re:Invent",
      "seminarCategoryId": 1,
      "seminarCategoryName": "外部セミナー",
      "attendedYearMonth": "2025-11-01",
      "remarks": ""
    }
  ]
}
```

> ADセミナーの場合、`adSeminarCategoryId` / `adSeminarCategoryName` はマスタから自動取得。

---

## POST /api/inventories/{id}/submit

棚卸を提出する。ステータスを `PENDING_GOAL` に遷移させる。

**権限**: 全員（自分の棚卸のみ）

**Request Body**: なし

**Response 200**

```json
{
  "id": 10,
  "status": "PENDING_GOAL",
  "submittedAt": "2025-04-20T10:00:00Z"
}
```

> `COMPLETED` から再提出した場合も `PENDING_GOAL` に戻る（目標の再確認が必要）。

---

## GET /api/inventories/{id}/comparison

SCR-004 前年度比較データを取得する。

**権限**: 全員（自分の棚卸のみ）

**Response 200**

```json
{
  "inventoryId": 10,
  "currentFiscalYear": "FY2025",
  "prevFiscalYear": "FY2024",
  "hasPrevYear": true,
  "items": [
    {
      "itSkillId": 5,
      "skillName": "Java",
      "currentDetailId": 100,
      "currentLevelValue": 4,
      "currentRemarks": "",
      "prevLevelValue": 3,
      "diff": 1
    },
    {
      "itSkillId": 6,
      "skillName": "Python",
      "currentDetailId": 101,
      "currentLevelValue": 2,
      "currentRemarks": "独学で学習中",
      "prevLevelValue": 2,
      "diff": 0
    },
    {
      "itSkillId": null,
      "skillName": "Terraform",
      "currentDetailId": 201,
      "currentLevelValue": 2,
      "currentRemarks": "",
      "prevLevelValue": null,
      "diff": null
    }
  ]
}
```

> `hasPrevYear: false` の場合（初回棚卸）はフロントエンドが比較画面をスキップする。  
> `prevLevelValue: null` は前年度にスキルなし（新規追加）。`diff: null` は計算不可。

---

## PATCH /api/inventories/{id}/it-skill-details/{detailId}

SCR-004 比較画面からの備考追記に使用する。備考フィールドのみ更新。

**権限**: 全員（自分の棚卸のみ）

**Request Body**

```json
{
  "remarks": "前年度より業務量が増えたため自信がついた"
}
```

**Response 200**

```json
{
  "id": 100,
  "remarks": "前年度より業務量が増えたため自信がついた"
}
```

---

## GET /api/inventories/{id}/goal-review

SCR-019 前回目標の振り返り画面で使用。現在の棚卸（`id`）に対応するユーザーの前年度の目標一覧を、振り返り入力値とともに返す。

**権限**: 全員（自分の棚卸のみ）

**Response 200**

```json
{
  "prevFiscalYear": "FY2024",
  "hasPrevGoals": true,
  "items": [
    {
      "prevGoalId": 1,
      "goalCategory": "IT_SKILL",
      "goalName": "Java",
      "targetPeriod": "2025-03-01",
      "reason": "Spring Boot の深い理解を目指す",
      "achievementStatus": "ACHIEVED",
      "reviewNote": "業務でSpring Bootを担当し目標達成できた"
    },
    {
      "prevGoalId": 2,
      "goalCategory": "QUALIFICATION",
      "goalName": "LPIC-1",
      "targetPeriod": "2024-12-01",
      "reason": "",
      "achievementStatus": "NOT_ACHIEVED",
      "reviewNote": "試験日程が合わず受験できなかった"
    },
    {
      "prevGoalId": 3,
      "goalCategory": "AD",
      "goalName": "マネジメント基礎",
      "targetPeriod": "2025-02-01",
      "reason": "チームリーダーを目指すため",
      "achievementStatus": null,
      "reviewNote": null
    }
  ]
}
```

> `hasPrevGoals: false` の場合（初回棚卸など）は `items` が空配列。フロントエンドはこの場合 SCR-019 をスキップして SCR-005 へ遷移する。  
> `achievementStatus` / `reviewNote` は未記入の場合 `null`。

---

## PUT /api/inventories/{id}/goal-review

前回目標の達成状況・振り返りコメントを一括保存する。前年度の `inventory_goals` レコードを更新する。

**権限**: 全員（自分の棚卸のみ）

**Request Body**

```json
{
  "items": [
    {
      "prevGoalId": 1,
      "achievementStatus": "ACHIEVED",
      "reviewNote": "業務でSpring Bootを担当し目標達成できた"
    },
    {
      "prevGoalId": 2,
      "achievementStatus": "NOT_ACHIEVED",
      "reviewNote": "試験日程が合わず受験できなかった"
    },
    {
      "prevGoalId": 3,
      "achievementStatus": null,
      "reviewNote": null
    }
  ]
}
```

> `achievementStatus` / `reviewNote` はともに任意。省略または `null` で未入力扱い（振り返りはすべて任意）。

**Response 200**: `GET /api/inventories/{id}/goal-review` と同形式

---

## POST /api/inventories/{id}/goal-review/complete

前回目標の振り返りを完了する。`inventories.goal_review_completed_at` に現在日時を設定する。振り返り内容の入力は任意のため、空のまま完了することも可能。

**権限**: 全員（自分の棚卸のみ）

**Request Body**: なし

**Response 200**

```json
{
  "id": 10,
  "goalReviewCompletedAt": "2026-04-15T10:30:00Z"
}
```

---

## GET /api/inventories/{id}/goals

目標一覧を取得する。

**権限**: 全員（自分の棚卸のみ）

**Response 200**

```json
{
  "items": [
    {
      "id": 1,
      "goalCategory": "IT_SKILL",
      "itSkillId": 5,
      "itSkillName": "Java",
      "qualificationId": null,
      "qualificationName": null,
      "adSeminarId": null,
      "adSeminarName": null,
      "customName": null,
      "targetPeriod": "2026-03-01",
      "reason": "Spring Boot の深い理解を目指す"
    },
    {
      "id": 2,
      "goalCategory": "AD",
      "itSkillId": null,
      "itSkillName": null,
      "qualificationId": null,
      "qualificationName": null,
      "adSeminarId": 1,
      "adSeminarName": "マネジメント基礎",
      "customName": null,
      "targetPeriod": "2025-09-01",
      "reason": "チームリーダーを目指すため"
    }
  ]
}
```

---

## PUT /api/inventories/{id}/goals

目標を一括保存する。送信した内容で全目標を上書きする。ステータスは `PENDING_GOAL` のまま。

**権限**: 全員（自分の棚卸のみ）

**Request Body**

```json
{
  "items": [
    {
      "id": 1,
      "goalCategory": "IT_SKILL",
      "itSkillId": 5,
      "qualificationId": null,
      "adSeminarId": null,
      "customName": null,
      "targetPeriod": "2026-03-01",
      "reason": "Spring Boot の深い理解を目指す"
    },
    {
      "id": null,
      "goalCategory": "QUALIFICATION",
      "itSkillId": null,
      "qualificationId": null,
      "adSeminarId": null,
      "customName": "LPIC-1",
      "targetPeriod": "2025-12-01",
      "reason": ""
    },
    {
      "id": 2,
      "goalCategory": "AD",
      "itSkillId": null,
      "qualificationId": null,
      "adSeminarId": 1,
      "customName": null,
      "targetPeriod": "2025-09-01",
      "reason": "チームリーダーを目指すため"
    },
    {
      "id": null,
      "goalCategory": "AD",
      "itSkillId": null,
      "qualificationId": null,
      "adSeminarId": 2,
      "customName": null,
      "targetPeriod": "2026-01-01",
      "reason": ""
    }
  ]
}
```

**Response 200**: 保存後の目標一覧（GET /api/inventories/{id}/goals と同形式）

---

## POST /api/inventories/{id}/goals/complete

目標設定を完了する。件数バリデーション後、ステータスを `COMPLETED` に遷移させる。

**権限**: 全員（自分の棚卸のみ）

**Request Body**: なし

**Response 200**

```json
{
  "id": 10,
  "status": "COMPLETED",
  "goalCompletedAt": "2025-04-21T09:30:00Z"
}
```

**Response 422**

```json
{
  "code": "GOAL_INCOMPLETE",
  "message": "目標設定の件数が不足しています",
  "errors": [
    { "field": "itSkillOrQualification", "message": "ITスキル・資格の目標を 1 件以上入力してください" },
    { "field": "ad", "message": "ADの目標を 2 件すべて入力してください" }
  ]
}
```

# 照会 API

**対象画面**: SCR-006（棚卸・目標照会）、SCR-007（チームメンバー照会）、SCR-008（全ユーザー照会）

---

## エンドポイント一覧

| メソッド | パス | 概要 | 権限 |
|---------|------|------|------|
| GET | /api/users/me/inventories | 自分の棚卸一覧（全年度） | 全員 |
| GET | /api/users/{userId}/inventories | 指定ユーザーの棚卸一覧 | TL / ADMIN |
| GET | /api/inventories/{id}/detail | 棚卸の全明細を取得 | 全員（※） |
| GET | /api/users/me/team-members | 自チームのメンバー一覧 | TL / ADMIN |
| GET | /api/users | 全ユーザー一覧（検索） | ADMIN |

> （※）自分の棚卸は全員参照可。他ユーザーの棚卸は TL（自チームのみ）/ ADMIN のみ参照可。

---

## GET /api/users/me/inventories

自分の棚卸一覧を全年度分返す（SCR-006 の年度セレクター用）。

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

## GET /api/users/{userId}/inventories

指定ユーザーの棚卸一覧を全年度分返す（SCR-007 / SCR-008 からメンバー詳細へ遷移する際に使用）。

**権限**: TL（自チームメンバーのみ）/ ADMIN（全員）

**Path Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `userId` | int | ユーザー ID |

**Response 200**: `GET /api/users/me/inventories` と同形式

**Response 403**

```json
{ "code": "FORBIDDEN", "message": "このユーザーへのアクセス権限がありません" }
```

---

## GET /api/inventories/{id}/detail

棚卸の全明細（ITスキル・資格・セミナー・目標）をまとめて返す。SCR-006 の詳細表示に使用。

**権限**: 全員（自分の棚卸のみ）/ TL（自チームメンバー）/ ADMIN（全員）

**Path Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `id` | int | 棚卸 ID |

**Response 200**

```json
{
  "inventory": {
    "id": 10,
    "userId": 1,
    "userName": "田中 太郎",
    "fiscalYear": { "id": 1, "name": "FY2025" },
    "status": "COMPLETED",
    "submittedAt": "2025-04-20T10:00:00Z",
    "goalCompletedAt": "2025-04-21T09:30:00Z"
  },
  "itSkillDetails": [
    {
      "id": 100,
      "itSkillId": 5,
      "itSkillName": "Java",
      "customSkillName": null,
      "category1Name": "アプリケーション開発",
      "skillLevelId": 3,
      "levelValue": 3,
      "levelDescription": "指導があれば実務で使える",
      "remarks": "業務で Spring Boot を 3 年使用",
      "isCustom": false
    },
    {
      "id": 201,
      "itSkillId": null,
      "itSkillName": null,
      "customSkillName": "Terraform",
      "category1Name": null,
      "skillLevelId": 2,
      "levelValue": 2,
      "levelDescription": "概念を理解している",
      "remarks": "",
      "isCustom": true
    }
  ],
  "qualificationDetails": [
    {
      "id": 50,
      "qualificationId": 1,
      "qualificationName": "基本情報技術者試験",
      "customQualificationName": null,
      "acquiredYearMonth": "2023-06-01",
      "remarks": "独学で取得",
      "isCustom": false
    }
  ],
  "seminarDetails": [
    {
      "id": 30,
      "adSeminarId": 1,
      "adSeminarName": "マネジメント基礎",
      "seminarName": null,
      "attendedYearMonth": "2025-06-01",
      "remarks": "チームで参加",
      "isAd": true
    }
  ],
  "goals": [
    {
      "id": 1,
      "goalCategory": "IT_SKILL",
      "targetName": "Java",
      "customName": null,
      "targetPeriod": "2026-03-01",
      "reason": "Spring Boot の深い理解を目指す"
    },
    {
      "id": 2,
      "goalCategory": "AD",
      "targetName": "マネジメント基礎",
      "customName": null,
      "targetPeriod": "2025-09-01",
      "reason": "チームリーダーを目指すため"
    }
  ],
  "comparison": {
    "prevFiscalYear": "FY2024",
    "hasPrevYear": true,
    "items": [
      {
        "itSkillId": 5,
        "skillName": "Java",
        "currentDetailId": 100,
        "currentLevelValue": 3,
        "prevLevelValue": 2,
        "diff": 1
      }
    ]
  }
}
```

> `comparison` は ITスキルの前年度比較情報。`hasPrevYear: false` なら `items` は空配列。  
> 今年度分の `remarks` は照会画面からも編集可能（→ `PATCH /api/inventories/{id}/it-skill-details/{detailId}`）。

---

## GET /api/users/me/team-members

自チームのメンバー一覧を返す（SCR-007）。  
TL の場合は `tl_user_id = 自分の ID` のユーザー、ADMIN は全員が対象。

**権限**: TL / ADMIN

**Query Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `fiscalYearId` | int | 絞り込み用年度 ID（任意） |
| `name` | string | 名前の部分一致（任意） |

**Response 200**

```json
[
  {
    "id": 1,
    "name": "田中 太郎",
    "email": "tanaka@example.com",
    "role": "GENERAL",
    "isActive": true,
    "currentInventory": {
      "id": 10,
      "fiscalYear": { "id": 1, "name": "FY2025" },
      "status": "COMPLETED"
    }
  },
  {
    "id": 3,
    "name": "佐藤 次郎",
    "email": "sato@example.com",
    "role": "GENERAL",
    "isActive": true,
    "currentInventory": null
  }
]
```

> `currentInventory` は当年度（または `fiscalYearId` 指定年度）の棚卸。未作成の場合 `null`。

---

## GET /api/users

全ユーザー一覧を検索・ページングで返す（SCR-008）。

**権限**: ADMIN

**Query Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `name` | string | 名前の部分一致（任意） |
| `role` | string | ロールでフィルタ: `GENERAL` / `TL` / `ADMIN`（任意） |
| `tlUserId` | int | 指定した TL に紐づくユーザーのみ（任意） |
| `isActive` | boolean | 有効 / 無効フィルタ（デフォルト: `true`） |
| `fiscalYearId` | int | 棚卸ステータス表示用年度 ID（任意） |
| `page` | int | ページ番号（デフォルト: 1） |
| `pageSize` | int | 件数（デフォルト: 20） |

**Response 200**

```json
{
  "items": [
    {
      "id": 1,
      "name": "田中 太郎",
      "email": "tanaka@example.com",
      "role": "GENERAL",
      "tlUser": { "id": 5, "name": "山田 花子" },
      "isActive": true,
      "currentInventory": {
        "id": 10,
        "fiscalYear": { "id": 1, "name": "FY2025" },
        "status": "COMPLETED"
      }
    }
  ],
  "total": 42,
  "page": 1,
  "pageSize": 20
}
```

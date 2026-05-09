# 管理者マスタ API

**対象画面**: SCR-011（ADマスタ）、SCR-012（ITスキル分類マスタ）、SCR-013（レベルマスタ）、SCR-014（ユーザー管理）、SCR-015（年度マスタ）

---

## エンドポイント一覧

| メソッド | パス | 概要 | 権限 |
|---------|------|------|------|
| GET | /api/ad-seminars | ADマスタ一覧 | 全員 |
| POST | /api/ad-seminars | ADマスタ追加 | ADMIN |
| PUT | /api/ad-seminars/{id} | ADマスタ更新 | ADMIN |
| DELETE | /api/ad-seminars/{id} | ADマスタ無効化 | ADMIN |
| PATCH | /api/ad-seminars/{id}/restore | ADマスタ復活 | ADMIN |
| GET | /api/it-skill-categories | ITスキル分類ツリー | TL / ADMIN |
| POST | /api/it-skill-categories | ITスキル分類追加 | ADMIN |
| PUT | /api/it-skill-categories/{id} | ITスキル分類更新 | ADMIN |
| DELETE | /api/it-skill-categories/{id} | ITスキル分類削除 | ADMIN |
| GET | /api/skill-levels | レベルマスタ一覧 | 全員 |
| POST | /api/skill-levels | レベルマスタ追加 | ADMIN |
| PUT | /api/skill-levels/{id} | レベルマスタ更新 | ADMIN |
| DELETE | /api/skill-levels/{id} | レベルマスタ削除 | ADMIN |
| GET | /api/users | ユーザー一覧 | ADMIN |
| POST | /api/users | ユーザー追加 | ADMIN |
| PUT | /api/users/{id} | ユーザー更新 | ADMIN |
| PATCH | /api/users/{id}/deactivate | ユーザー無効化 | ADMIN |
| PATCH | /api/users/{id}/activate | ユーザー有効化 | ADMIN |
| GET | /api/fiscal-years | 年度一覧 | 全員 |
| GET | /api/fiscal-years/current | 当年度取得 | 全員 |
| POST | /api/fiscal-years | 年度追加 | ADMIN |
| PUT | /api/fiscal-years/{id} | 年度更新 | ADMIN |
| GET | /api/fiscal-year-settings | 会計年度設定取得 | ADMIN |
| PUT | /api/fiscal-year-settings | 会計年度設定更新 | ADMIN |

---

## GET /api/ad-seminars

有効な AD マスタを全件返す。

**権限**: 全員

**Query Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `isActive` | boolean | 省略で有効のみ |

**Response 200**

```json
[
  {
    "id": 1,
    "name": "マネジメント基礎",
    "description": "PM 基礎スキル習得",
    "sortOrder": 10,
    "isActive": true
  },
  {
    "id": 2,
    "name": "クラウドアーキテクチャ研修",
    "description": "AWS / Azure / GCP",
    "sortOrder": 20,
    "isActive": true
  }
]
```

---

## POST /api/ad-seminars

AD マスタを新規追加する。

**権限**: ADMIN

**Request Body**

```json
{
  "name": "セキュリティ基礎",
  "description": "情報セキュリティ研修",
  "sortOrder": 30,
  "isActive": true
}
```

**Response 201**

```json
{
  "id": 3,
  "name": "セキュリティ基礎",
  "description": "情報セキュリティ研修",
  "sortOrder": 30,
  "isActive": true
}
```

---

## PUT /api/ad-seminars/{id}

AD マスタを更新する。

**権限**: ADMIN

**Request Body**: `POST /api/ad-seminars` と同形式

**Response 200**: `POST` の Response 201 と同形式

---

## DELETE /api/ad-seminars/{id}

AD マスタを論理削除する。

**権限**: ADMIN

**Response 204**: No Content

---

## PATCH /api/ad-seminars/{id}/restore

論理削除済みの AD マスタを有効に戻す。

**権限**: ADMIN

**Response 200**

```json
{ "id": 1, "isActive": true }
```

---

## GET /api/it-skill-categories

ITスキル分類をツリー形式で返す。分類1→2→3 の階層構造。

**権限**: TL / ADMIN

**Response 200**

```json
[
  {
    "id": 1,
    "name": "インフラ",
    "level": 1,
    "parentId": null,
    "sortOrder": 10,
    "isActive": true,
    "children": [
      {
        "id": 3,
        "name": "OS",
        "level": 2,
        "parentId": 1,
        "sortOrder": 10,
        "isActive": true,
        "children": [
          {
            "id": 7,
            "name": "Linux 系",
            "level": 3,
            "parentId": 3,
            "sortOrder": 10,
            "isActive": true,
            "children": []
          }
        ]
      }
    ]
  },
  {
    "id": 2,
    "name": "アプリケーション開発",
    "level": 1,
    "parentId": null,
    "sortOrder": 20,
    "isActive": true,
    "children": []
  }
]
```

---

## POST /api/it-skill-categories

ITスキル分類を追加する。

**権限**: ADMIN

**Request Body**

```json
{
  "name": "コンテナ・オーケストレーション",
  "parentId": 1,
  "sortOrder": 30,
  "isActive": true
}
```

> `parentId: null` で分類1（ルート）を作成。

**Response 201**

```json
{
  "id": 12,
  "name": "コンテナ・オーケストレーション",
  "level": 2,
  "parentId": 1,
  "sortOrder": 30,
  "isActive": true
}
```

---

## PUT /api/it-skill-categories/{id}

ITスキル分類を更新する。

**権限**: ADMIN

**Request Body**: `POST /api/it-skill-categories` と同形式

**Response 200**: `POST` の Response 201 と同形式

---

## DELETE /api/it-skill-categories/{id}

ITスキル分類を削除する。配下（子分類含む）に有効なスキルが存在する場合はエラー。

**権限**: ADMIN

**Response 204**: No Content

**Response 422**

```json
{
  "code": "CATEGORY_HAS_SKILLS",
  "message": "「インフラ」配下に有効なスキルが存在するため削除できません",
  "affectedSkills": [
    { "id": 6,  "name": "Linux" },
    { "id": 10, "name": "ネットワーク基礎" },
    { "id": 11, "name": "Docker" }
  ]
}
```

---

## GET /api/skill-levels

レベルマスタを `level_value` 昇順で全件返す。

**権限**: 全員

**Query Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `isActive` | boolean | 省略で有効のみ |

**Response 200**

```json
[
  { "id": 1, "levelValue": 1, "description": "知識なし／未経験",       "isActive": true },
  { "id": 2, "levelValue": 2, "description": "概念を理解している",       "isActive": true },
  { "id": 3, "levelValue": 3, "description": "指導があれば実務で使える", "isActive": true },
  { "id": 4, "levelValue": 4, "description": "独力で実務に適用できる",   "isActive": true },
  { "id": 5, "levelValue": 5, "description": "他者に指導・展開できる",   "isActive": true }
]
```

---

## POST /api/skill-levels

レベルマスタを追加する。

**権限**: ADMIN

**Request Body**

```json
{
  "levelValue": 6,
  "description": "組織全体をリードできる",
  "isActive": true
}
```

**Response 201**

```json
{ "id": 6, "levelValue": 6, "description": "組織全体をリードできる", "isActive": true }
```

**Response 409**

```json
{ "code": "CONFLICT", "message": "同じ数値のレベルがすでに存在します" }
```

---

## PUT /api/skill-levels/{id}

レベルマスタを更新する。既存の棚卸データに保存済みの数値は変更されない。

**権限**: ADMIN

**Request Body**: `POST /api/skill-levels` と同形式

**Response 200**: `POST` の Response 201 と同形式

---

## DELETE /api/skill-levels/{id}

レベルマスタを削除する（物理削除）。棚卸データに参照されている場合は削除不可。

**権限**: ADMIN

**Response 204**: No Content

**Response 409**

```json
{ "code": "CONFLICT", "message": "このレベルは棚卸データで使用されているため削除できません" }
```

---

## GET /api/users

ユーザー一覧を検索・ページングで返す（SCR-014）。`03-inquiry.md` の `GET /api/users` と同一エンドポイント。

**権限**: ADMIN  
**詳細**: → [03-inquiry.md](./03-inquiry.md)

---

## POST /api/users

ユーザーを新規追加する。初期パスワードはユーザーIDと同じ値が設定される。

**権限**: ADMIN

**Request Body**

```json
{
  "userId": "tanaka.taro",
  "name": "田中 太郎",
  "email": "tanaka@example.com",
  "role": "GENERAL",
  "tlUserId": 5
}
```

> `email` は任意項目。省略または `null` の場合は未登録扱い。  
> `tlUserId` は TL または ADMIN ロールのユーザー ID。未設定の場合は `null`。

**Response 201**

```json
{
  "id": 1,
  "userId": "tanaka.taro",
  "name": "田中 太郎",
  "email": "tanaka@example.com",
  "role": "GENERAL",
  "tlUser": { "id": 5, "name": "山田 花子" },
  "isActive": true,
  "isInitialPassword": true
}
```

> 初期パスワードは `userId` と同一。管理者がユーザーに口頭等で通知する。

**Response 409**

```json
{ "code": "CONFLICT", "message": "このユーザーIDはすでに使用されています" }
```

---

## PUT /api/users/{id}

ユーザー情報を更新する。

**権限**: ADMIN

**Request Body**

```json
{
  "name": "田中 太郎",
  "email": "tanaka@example.com",
  "role": "TL",
  "tlUserId": null
}
```

> `email` は任意項目。省略または `null` の場合は未登録扱い。  
> `userId` はユーザー更新で変更不可。

**Response 200**

```json
{
  "id": 1,
  "userId": "tanaka.taro",
  "name": "田中 太郎",
  "email": "tanaka@example.com",
  "role": "TL",
  "tlUser": null,
  "isActive": true,
  "isInitialPassword": false
}
```

---

## PATCH /api/users/{id}/deactivate

ユーザーを無効化（論理削除）する。

**権限**: ADMIN

**Response 204**: No Content

---

## PATCH /api/users/{id}/activate

無効化されたユーザーを有効に戻す。

**権限**: ADMIN

**Response 204**: No Content

---

## GET /api/fiscal-years

年度一覧を返す（新しい年度順）。

**権限**: 全員

**Response 200**

```json
[
  {
    "id": 1,
    "name": "FY2025",
    "startDate": "2025-04-01",
    "endDate": "2026-03-31",
    "inputStartDate": "2025-04-01",
    "inputEndDate": "2025-05-31",
    "isActive": true,
    "computedStatus": "進行中"
  },
  {
    "id": 2,
    "name": "FY2024",
    "startDate": "2024-04-01",
    "endDate": "2025-03-31",
    "inputStartDate": "2024-04-01",
    "inputEndDate": "2024-05-31",
    "isActive": true,
    "computedStatus": "完了"
  }
]
```

> `computedStatus` はサーバーが `startDate` / `endDate` と現在日付を比較して算出（開始前: 「予定」/ 期間中: 「進行中」/ 終了後: 「完了」）。

---

## GET /api/fiscal-years/current

現在の会計年度を返す。

**権限**: 全員

**Response 200**: `GET /api/fiscal-years` の配列要素と同形式（1 件）

**Response 404**

```json
{ "code": "NOT_FOUND", "message": "現在の年度が登録されていません" }
```

---

## POST /api/fiscal-years

年度を新規追加する。

**権限**: ADMIN

**Request Body**

```json
{
  "name": "FY2026",
  "startDate": "2026-04-01",
  "endDate": "2027-03-31",
  "inputStartDate": "2026-04-01",
  "inputEndDate": "2026-05-31",
  "isActive": true
}
```

**Response 201**: `GET /api/fiscal-years` の配列要素と同形式（1 件）

---

## PUT /api/fiscal-years/{id}

年度情報を更新する。年度の削除は提供しない（棚卸データとの整合性確保のため）。

**権限**: ADMIN

**Request Body**: `POST /api/fiscal-years` と同形式

**Response 200**: `POST` の Response 201 と同形式

---

## GET /api/fiscal-year-settings

会計年度設定（開始月）を取得する。

**権限**: ADMIN

**Response 200**

```json
{
  "fiscalYearStartMonth": 4
}
```

---

## PUT /api/fiscal-year-settings

会計年度開始月を更新する。変更後の年度新規作成時のデフォルト日付に反映される。

**権限**: ADMIN

**Request Body**

```json
{
  "fiscalYearStartMonth": 4
}
```

**Response 200**

```json
{
  "fiscalYearStartMonth": 4
}
```

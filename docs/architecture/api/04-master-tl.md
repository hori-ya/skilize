# ITスキル・参考資格マスタ API

**対象画面**: SCR-009（ITスキルマスタ管理）、SCR-010（参考資格マスタ管理）

---

## エンドポイント一覧

| メソッド | パス | 概要 | 権限 |
|---------|------|------|------|
| GET | /api/it-skills | ITスキル一覧 | 全員 |
| POST | /api/it-skills | ITスキル追加 | TL / ADMIN |
| PUT | /api/it-skills/{id} | ITスキル更新 | TL / ADMIN |
| DELETE | /api/it-skills/{id} | ITスキル無効化（論理削除） | TL / ADMIN |
| PATCH | /api/it-skills/{id}/restore | ITスキル復活 | TL / ADMIN |
| GET | /api/it-skills/custom-unregistered | 未昇格カスタムスキル一覧 | TL / ADMIN |
| POST | /api/it-skills/promote | カスタムスキルをマスタ昇格 | TL / ADMIN |
| GET | /api/qualifications | 参考資格一覧 | 全員 |
| POST | /api/qualifications | 参考資格追加 | TL / ADMIN |
| PUT | /api/qualifications/{id} | 参考資格更新 | TL / ADMIN |
| DELETE | /api/qualifications/{id} | 参考資格無効化（論理削除） | TL / ADMIN |
| PATCH | /api/qualifications/{id}/restore | 参考資格復活 | TL / ADMIN |
| GET | /api/qualifications/custom-unregistered | 未昇格カスタム資格一覧 | TL / ADMIN |
| POST | /api/qualifications/promote | カスタム資格をマスタ昇格 | TL / ADMIN |

> ITスキル分類（`/api/it-skill-categories`）の参照（GET）は TL / ADMIN が可能。  
> 分類の CRUD は ADMIN 専用（→ 05-master-admin.md）。

---

## GET /api/it-skills

有効な ITスキルを全件返す。棚卸入力画面・マスタ管理画面で使用。

**権限**: 全員

**Query Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `isActive` | boolean | `true`（有効のみ）/ `false`（無効のみ）/ 省略（有効のみ） |
| `category1Id` | int | 分類1でフィルタ（任意） |

**Response 200**

```json
[
  {
    "id": 5,
    "name": "Java",
    "categoryId": 8,
    "category1Id": 2,
    "category1Name": "アプリケーション開発",
    "category2Name": "バックエンド",
    "category3Name": "Java",
    "description": "",
    "sortOrder": 10,
    "isActive": true
  },
  {
    "id": 6,
    "name": "Linux",
    "categoryId": 3,
    "category1Id": 1,
    "category1Name": "インフラ",
    "category2Name": "OS",
    "category3Name": "Linux 系",
    "description": "",
    "sortOrder": 10,
    "isActive": true
  }
]
```

---

## POST /api/it-skills

ITスキルを新規追加する。

**権限**: TL / ADMIN

**Request Body**

```json
{
  "name": "Kotlin",
  "categoryId": 8,
  "description": "Android / サーバーサイド開発向け言語",
  "sortOrder": 20,
  "isActive": true
}
```

**Response 201**

```json
{
  "id": 20,
  "name": "Kotlin",
  "categoryId": 8,
  "category1Name": "アプリケーション開発",
  "category2Name": "バックエンド",
  "category3Name": "Java",
  "description": "Android / サーバーサイド開発向け言語",
  "sortOrder": 20,
  "isActive": true
}
```

**Response 409**

```json
{ "code": "CONFLICT", "message": "同名の IT スキルがすでに存在します" }
```

---

## PUT /api/it-skills/{id}

ITスキルを更新する。

**権限**: TL / ADMIN

**Request Body**

```json
{
  "name": "Kotlin",
  "categoryId": 8,
  "description": "更新後の説明",
  "sortOrder": 15,
  "isActive": true
}
```

**Response 200**: `POST /api/it-skills` の Response 201 と同形式

---

## DELETE /api/it-skills/{id}

ITスキルを論理削除（`isActive: false`）する。既存の棚卸データには影響しない。

**権限**: TL / ADMIN

**Response 204**: No Content

---

## PATCH /api/it-skills/{id}/restore

論理削除済みの ITスキルを有効に戻す。

**権限**: TL / ADMIN

**Response 200**

```json
{ "id": 5, "isActive": true }
```

---

## GET /api/it-skills/custom-unregistered

ユーザーが入力したカスタムスキルのうち、まだマスタ昇格されていないものを返す。

**権限**: TL / ADMIN

**Query Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `fiscalYearId` | int | 年度でフィルタ（任意） |

**Response 200**

```json
[
  {
    "customSkillName": "Terraform",
    "inputUserId": 1,
    "inputUserName": "田中 太郎",
    "fiscalYear": "FY2025",
    "levelValue": 2,
    "remarks": ""
  },
  {
    "customSkillName": "Ansible",
    "inputUserId": 3,
    "inputUserName": "佐藤 次郎",
    "fiscalYear": "FY2025",
    "levelValue": 1,
    "remarks": "勉強中"
  }
]
```

---

## POST /api/it-skills/promote

カスタムスキルをマスタに昇格する。昇格後、対象ユーザーの棚卸明細を新マスタに自動紐付けする。

**権限**: TL / ADMIN

**Request Body**

```json
{
  "customSkillName": "Terraform",
  "name": "Terraform",
  "categoryId": 3,
  "description": "インフラ自動化ツール",
  "sortOrder": 99
}
```

**Response 201**

```json
{
  "id": 21,
  "name": "Terraform",
  "categoryId": 3,
  "category1Name": "インフラ",
  "isActive": true,
  "promotedCount": 2
}
```

> `promotedCount` は自動紐付けされた棚卸明細件数。

---

## GET /api/qualifications

有効な参考資格を全件返す。

**権限**: 全員

**Query Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `isActive` | boolean | 有効 / 無効フィルタ（省略で有効のみ） |

**Response 200**

```json
[
  {
    "id": 1,
    "name": "基本情報技術者試験",
    "description": "IPA 国家資格",
    "sortOrder": 10,
    "isActive": true
  },
  {
    "id": 2,
    "name": "応用情報技術者試験",
    "description": "IPA 国家資格",
    "sortOrder": 20,
    "isActive": true
  }
]
```

---

## POST /api/qualifications

参考資格を新規追加する。

**権限**: TL / ADMIN

**Request Body**

```json
{
  "name": "AWS Solutions Architect Professional",
  "description": "AWS 上級認定資格",
  "sortOrder": 35,
  "isActive": true
}
```

**Response 201**

```json
{
  "id": 10,
  "name": "AWS Solutions Architect Professional",
  "description": "AWS 上級認定資格",
  "sortOrder": 35,
  "isActive": true
}
```

---

## PUT /api/qualifications/{id}

参考資格を更新する。

**権限**: TL / ADMIN

**Request Body**: `POST /api/qualifications` と同形式

**Response 200**: `POST` の Response 201 と同形式

---

## DELETE /api/qualifications/{id}

参考資格を論理削除する。

**権限**: TL / ADMIN

**Response 204**: No Content

---

## PATCH /api/qualifications/{id}/restore

論理削除済みの参考資格を有効に戻す。

**権限**: TL / ADMIN

**Response 200**

```json
{ "id": 1, "isActive": true }
```

---

## GET /api/qualifications/custom-unregistered

未昇格のカスタム資格一覧を返す。

**権限**: TL / ADMIN

**Query Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `fiscalYearId` | int | 年度でフィルタ（任意） |

**Response 200**

```json
[
  {
    "customQualificationName": "LPIC-1",
    "inputUserId": 1,
    "inputUserName": "田中 太郎",
    "fiscalYear": "FY2025",
    "acquiredYearMonth": "2025-01-01",
    "remarks": ""
  }
]
```

---

## POST /api/qualifications/promote

カスタム資格をマスタに昇格する。昇格後、対象ユーザーの棚卸明細を新マスタに自動紐付けする。

**権限**: TL / ADMIN

**Request Body**

```json
{
  "customQualificationName": "LPIC-1",
  "name": "LPIC-1",
  "description": "Linux Professional Institute 認定資格 レベル1",
  "sortOrder": 50
}
```

**Response 201**

```json
{
  "id": 11,
  "name": "LPIC-1",
  "description": "Linux Professional Institute 認定資格 レベル1",
  "sortOrder": 50,
  "isActive": true,
  "promotedCount": 1
}
```

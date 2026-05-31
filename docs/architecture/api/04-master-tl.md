# ITスキル・参考資格マスタ API

**対象画面**: SCR-009（ITスキルマスタ管理）、SCR-010（参考資格マスタ管理）

> **ロール注記**: このファイルのエンドポイントはバックエンドAPIレベルでは **TL / ADMIN** に許可されているが、現在のフロントエンドUIへのナビゲーション（マスタ管理ページ `/master/*`）は **ADMIN のみ** に制限されている。TL がAPIを直接呼び出すシナリオは現在UIからは行えない。

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

> ADマスタ・ITスキル分類・資格分類・各セミナー分類・レベルマスタの管理（CRUD）も TL / ADMIN が可能（→ 05-master-admin.md）。  
> ユーザー管理・年度マスタ管理のみ ADMIN 専用。

---

## GET /api/it-skills

ITスキルを返す。棚卸入力画面・マスタ管理画面で使用。

**権限**: 全員

**Query Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `isActive` | boolean | `true`（有効のみ）/ `false`（無効のみ）/ 省略（全件） |
| `category1Id` | int | 分類1でフィルタ（任意） |

**ソート順**

| `isActive` パラメータ | ソート順 | 用途 |
|---|---|---|
| 未指定（全件） | 分類1 ASC → 分類2 ASC → 分類3 ASC → sortOrder ASC | マスタ管理画面の一覧表示 |
| `false`（無効のみ） | 分類1 ASC → 分類2 ASC → 分類3 ASC → sortOrder ASC | マスタ管理画面の無効フィルター |
| `true`（有効のみ） | sortOrder ASC | 棚卸入力画面の選択肢（変更なし）|

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

参考資格を返す。

**権限**: 全員

**Query Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `isActive` | boolean | `true`（有効のみ）/ `false`（無効のみ）/ 省略（全件） |
| `categoryId` | int | 資格分類でフィルタ（任意） |

**ソート順**

| `isActive` パラメータ | ソート順 | 用途 |
|---|---|---|
| 未指定（全件） | 分類 ASC NULLS LAST → sortOrder ASC | マスタ管理画面の一覧表示 |
| `false`（無効のみ） | 分類 ASC NULLS LAST → sortOrder ASC | マスタ管理画面の無効フィルター |
| `true`（有効のみ） | sortOrder ASC | 棚卸入力画面の選択肢（変更なし）|

**Response 200**

```json
[
  {
    "id": 1,
    "name": "基本情報技術者試験",
    "categoryId": 1,
    "categoryName": "IT資格",
    "description": "IPA 国家資格",
    "sortOrder": 10,
    "isActive": true
  },
  {
    "id": 2,
    "name": "応用情報技術者試験",
    "categoryId": 1,
    "categoryName": "IT資格",
    "description": "IPA 国家資格",
    "sortOrder": 20,
    "isActive": true
  }
]
```

> `categoryId` / `categoryName` は未分類の場合 `null`。

---

## POST /api/qualifications

参考資格を新規追加する。

**権限**: TL / ADMIN

**Request Body**

```json
{
  "name": "AWS Solutions Architect Professional",
  "categoryId": 2,
  "description": "AWS 上級認定資格",
  "sortOrder": 35,
  "isActive": true
}
```

> `categoryId` は任意。省略または `null` で未分類。

**Response 201**

```json
{
  "id": 10,
  "name": "AWS Solutions Architect Professional",
  "categoryId": 2,
  "categoryName": "クラウド資格",
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
  "categoryId": 3,
  "description": "Linux Professional Institute 認定資格 レベル1",
  "sortOrder": 50
}
```

> `categoryId` は任意。省略または `null` で未分類。

**Response 201**

```json
{
  "id": 11,
  "name": "LPIC-1",
  "categoryId": 3,
  "categoryName": "ベンダー資格",
  "description": "Linux Professional Institute 認定資格 レベル1",
  "sortOrder": 50,
  "isActive": true,
  "promotedCount": 1
}
```

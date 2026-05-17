# 面談メモ API

**対象画面**: SCR-006（棚卸・目標照会）、SCR-007（チームメンバー照会）、SCR-008（全ユーザー照会）のメンバー詳細

---

## エンドポイント一覧

| メソッド | パス | 概要 | 権限 |
|---------|------|------|------|
| GET | /api/interviews/inventory/{inventoryId} | 自分の面談メモ取得 | TL / ADMIN |
| PUT | /api/interviews/inventory/{inventoryId} | 面談メモ保存（Upsert） | TL / ADMIN |
| GET | /api/interviews/inventory/{inventoryId}/prev-year | 前年度の自分の面談メモ取得 | TL / ADMIN |

> GENERAL ロールは全エンドポイントに対して 403 を返す。  
> TL / ADMIN は自分が入力したメモ（`interviewer_id = 自分の ID`）のみを取得・更新できる。

---

## 認可ルール

```
リクエスト受信時の処理順:

1. JWT 未認証 → 401
2. GENERAL ロール → 403（データの存在有無も含め一切返さない）
3. TL / ADMIN
   └── GET: WHERE inventory_id = ? AND interviewer_id = 自分の ID でレコード検索
       ├── レコードあり → 200（自分のメモを返す）
       └── レコードなし → 200（null body。「メモ未記入」として扱う）
   └── PUT: (inventory_id, 自分の ID) で UPSERT
       ├── 対象 inventory が存在しない → 404
       └── 存在する → 200（保存後のメモを返す）
```

---

## GET /api/interviews/inventory/{inventoryId}

指定棚卸に対する自分の面談メモを取得する。

**権限**: TL / ADMIN

**Path Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `inventoryId` | int | 棚卸ヘッダーID（`inventories.id`） |

**Response 200（面談メモあり）**

```json
{
  "id": 3,
  "inventoryId": 10,
  "interviewerId": 5,
  "interviewerName": "山田 花子",
  "generalNote": "来期はAWS認定資格の取得を目標に設定してほしい。",
  "detailNotes": [
    {
      "id": 20,
      "detailType": "IT_SKILL",
      "detailId": 100,
      "note": "実務では活用できているが、より深い理解が必要。"
    },
    {
      "id": 21,
      "detailType": "GOAL",
      "detailId": 15,
      "note": "順調に進捗中。引き続きサポートする。"
    }
  ]
}
```

**Response 200（面談メモなし）**

```json
null
```

> レコードが存在しない場合は `null` を返す（404 ではない）。フロントは `null` を「未記入の新規入力フォーム」として扱う。

**Response 403**

```json
{ "code": "FORBIDDEN", "message": "面談メモへのアクセス権限がありません" }
```

---

## PUT /api/interviews/inventory/{inventoryId}

指定棚卸に対する自分の面談メモを一括保存する（INSERT or UPDATE）。  
`detailNotes` は全件置き換え（送信されなかった明細メモは削除される）。

**権限**: TL / ADMIN

**Path Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `inventoryId` | int | 棚卸ヘッダーID（`inventories.id`） |

**Request Body**

```json
{
  "generalNote": "来期はAWS認定資格の取得を目標に設定してほしい。",
  "detailNotes": [
    {
      "detailType": "IT_SKILL",
      "detailId": 100,
      "note": "実務では活用できているが、より深い理解が必要。"
    },
    {
      "detailType": "GOAL",
      "detailId": 15,
      "note": "順調に進捗中。引き続きサポートする。"
    }
  ]
}
```

| フィールド | 型 | 必須 | 説明 |
|-----------|-----|:---:|------|
| `generalNote` | string | — | 全体備忘録（null / 空文字可） |
| `detailNotes` | array | ○ | 明細メモ一覧。空配列の場合は既存の明細メモを全削除 |
| `detailNotes[].detailType` | string | ○ | `IT_SKILL` / `QUALIFICATION` / `SEMINAR` / `GOAL` |
| `detailNotes[].detailId` | int | ○ | 対象明細のID |
| `detailNotes[].note` | string | ○ | メモ内容（空文字不可） |

**Response 200**: GET と同形式（保存後のレコードを返す）

**Response 404**

```json
{ "code": "NOT_FOUND", "message": "棚卸が見つかりません" }
```

**Response 403**

```json
{ "code": "FORBIDDEN", "message": "面談メモへのアクセス権限がありません" }
```

---

## GET /api/interviews/inventory/{inventoryId}/prev-year

指定棚卸と同一ユーザーの前年度棚卸に対する、自分の面談メモを取得する。  
前年度面談メモ参照トグルに使用する。

**権限**: TL / ADMIN

**Path Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `inventoryId` | int | 当年度棚卸ヘッダーID（`inventories.id`） |

**処理フロー**

```
1. inventoryId から inventory を取得（user_id, fiscal_year_id を得る）
2. 同一 user_id の前年度棚卸（fiscal_year の start_date が1年前の年度）を検索
3. 前年度棚卸に対する自分の interview_notes を返す
```

**Response 200**: GET /api/interviews/inventory/{inventoryId} と同形式

> 前年度棚卸が存在しない、または前年度の自分のメモが存在しない場合も `null` を返す。

---

## バックエンド実装メモ

### パッケージ構成

```
com.skilize.interview
├── presentation/
│   ├── InterviewController.java
│   ├── InterviewResponse.java         ← record
│   ├── DetailNoteResponse.java        ← record
│   ├── SaveInterviewRequest.java      ← record
│   └── DetailNoteRequest.java         ← record
├── application/
│   └── InterviewService.java          ← @Transactional
└── domain/
    ├── InventoryInterview.java         ← Entity
    ├── InterviewDetailNote.java        ← Entity
    ├── DetailType.java                 ← Enum
    ├── InventoryInterviewRepository.java
    └── InterviewDetailNoteRepository.java
```

### フロントエンド構成

```
features/interview/
├── api/
│   └── interviewApi.ts       ← GET / PUT の axios 呼び出し（getInterview / saveInterview / getPrevYearInterview）
└── types/
    └── index.ts              ← InterviewMemo / DetailNoteItem / DetailType 型
```

> 面談メモの UI（入力フォーム・前年度メモ表示）は `features/team/pages/MemberDetailPage.tsx` に直接実装している。  
> 独立したコンポーネントは存在しない。

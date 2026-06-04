# API 設計規約

**バージョン**: 1.0.0  
**作成日**: 2026-05-09

---

## 1. ベース URL

```
/api
```

ローカル開発: `http://localhost:8080/api`

---

## 2. 認証

- ログイン不要なエンドポイント（`POST /api/auth/login`）を除き、すべてのリクエストに JWT を付与する
- `Authorization: Bearer <token>`
- JWT はログイン API で取得する
- トークン有効期限は 8 時間（設定値で変更可）

### JWT ペイロード

```json
{
  "sub": "1",
  "name": "田中 太郎",
  "role": "GENERAL",
  "iat": 1746700000,
  "exp": 1746728800
}
```

---

## 3. Content-Type

リクエスト・レスポンスともに `application/json`

---

## 4. エラーレスポンス形式

### 4.1 標準エラー

```json
{
  "code": "NOT_FOUND",
  "message": "指定のリソースが見つかりません"
}
```

### 4.2 バリデーションエラー（400）

```json
{
  "code": "VALIDATION_ERROR",
  "message": "入力値に誤りがあります",
  "errors": [
    { "field": "email",    "message": "メールアドレス形式で入力してください" },
    { "field": "password", "message": "8 文字以上で入力してください" }
  ]
}
```

### 主なエラーコード一覧

| コード | HTTP | 説明 |
|--------|:----:|------|
| `AUTH_FAILED` | 401 | 認証失敗（メール / パスワード不一致） |
| `UNAUTHORIZED` | 401 | 認証トークンなし・期限切れ |
| `FORBIDDEN` | 403 | 操作権限なし |
| `NOT_FOUND` | 404 | リソースが存在しない |
| `CONFLICT` | 409 | 重複など整合性エラー |
| `VALIDATION_ERROR` | 400 | 入力バリデーション失敗 |
| `GOAL_INCOMPLETE` | 422 | 目標設定件数不足（完了操作ブロック） |
| `CATEGORY_HAS_SKILLS` | 422 | 分類削除不可（配下に有効スキルあり） |
| `CATEGORY_HAS_QUALIFICATIONS` | 422 | 分類削除不可（配下に有効な資格あり） |
| `CATEGORY_HAS_AD_SEMINARS` | 422 | 分類削除不可（配下に有効なADあり） |
| `INVENTORY_STATUS_INVALID` | 422 | 操作に対してステータスが不正 |

---

## 5. ページネーション

件数が多くなりうる一覧 API にはクエリパラメータでページングを指定する。

**リクエスト**

| パラメータ | 型 | デフォルト | 説明 |
|-----------|----|:--------:|------|
| `page` | int | 1 | ページ番号（1 始まり） |
| `pageSize` | int | 20 | 1 ページあたりの件数 |

**レスポンス**

```json
{
  "items": [...],
  "total": 100,
  "page": 1,
  "pageSize": 20
}
```

件数が限られており全件返す API（マスタ一覧など）はページネーションなしで配列を返す。

---

## 6. 日付・日時フォーマット

| 型 | フォーマット | 例 |
|----|------------|-----|
| date | `YYYY-MM-DD` | `2025-04-01` |
| datetime | ISO 8601 UTC | `2025-04-01T09:00:00Z` |
| 年月（取得年月等） | `YYYY-MM-DD`（月初日） | `2025-04-01` |

---

## 7. 論理削除

マスタデータの削除はすべて論理削除（`isActive: false`）で行う。  
削除済みデータは棚卸過去データの参照を保持するが、入力・照会画面には表示しない。  
管理画面では `isActive` を指定して有効 / 無効をフィルタリングできる。

---

## 8. ロール定義

| 値 | 説明 |
|----|------|
| `GENERAL` | 一般ユーザー |
| `TL` | チームリーダー |
| `ADMIN` | 管理者 |

本ドキュメントの「権限」欄に記載のロール以上を必要とする。  
例：「TL / ADMIN」= TL または ADMIN のみアクセス可。

---

## 9. 共通レスポンスオブジェクト

### FiscalYear

```json
{
  "id": 1,
  "name": "FY2025",
  "startDate": "2025-04-01",
  "endDate": "2026-03-31",
  "inputStartDate": "2025-04-01",
  "inputEndDate": "2025-05-31",
  "isActive": true
}
```

### SkillLevel

```json
{
  "id": 1,
  "levelValue": 3,
  "description": "指導があれば実務で使える",
  "isActive": true
}
```

### User（簡易）

```json
{
  "id": 10,
  "name": "田中 太郎",
  "role": "GENERAL"
}
```

---

## 10. APIドキュメント一覧

| ファイル | 対象エンドポイント群 |
|---------|---------------------|
| [01-auth.md](./01-auth.md) | `/api/auth/**` |
| [02-inventory.md](./02-inventory.md) | `/api/inventory/**` |
| [03-inquiry.md](./03-inquiry.md) | `/api/users/**`（照会・ユーザー一覧） |
| [04-master-tl.md](./04-master-tl.md) | マスタ参照（TL/GENERAL） |
| [05-master-admin.md](./05-master-admin.md) | マスタ管理（ADMIN） |
| [06-charts.md](./06-charts.md) | `/api/charts/**`（グラフ集計） |
| [07-interview.md](./07-interview.md) | `/api/interviews/**`（面談メモ） |
| [08-expectations.md](./08-expectations.md) | `/api/users/{userId}/expectations/**`（期待コメント） |
| [09-ai-analysis.md](./09-ai-analysis.md) | `/api/users/me/ai-analyses`、`/api/users/{userId}/ai-analyses`（AIキャリア分析） |
| [10-ai-chat.md](./10-ai-chat.md) | `/api/ai/chat`（AI チャット） |
| [11-master-excel.md](./11-master-excel.md) | `/api/master-excel/**`（マスタ Excel 出力・取込、ADMIN） |
| [12-report.md](./12-report.md) | `/api/inventories/{id}/report`（棚卸表 PDF 出力） |

# 期待コメント API

**対象画面**: SCR-007（チームメンバー照会）、SCR-008（全ユーザー照会）のメンバー詳細「期待」タブ

---

## エンドポイント一覧

| メソッド | パス | 概要 | 権限 |
|---------|------|------|------|
| GET | /api/users/{userId}/expectations | 期待コメント取得 | TL / ADMIN |
| PUT | /api/users/{userId}/expectations/tl | TL期待コメント保存 | TL（担当TLのみ） |
| PUT | /api/users/{userId}/expectations/company | 会社期待コメント保存 | ADMIN |

> GENERAL ロールは全エンドポイントに対して 403 を返す。  
> TL は自分が担当するユーザー（`users.tl_user_id = 自分の ID`）の期待コメントのみ取得・編集できる。

---

## 認可ルール

```
GET:
  1. JWT 未認証 → 401
  2. GENERAL ロール → 403
  3. ADMIN → 常に参照可
  4. TL → 対象ユーザーの tl_user_id = 自分の ID の場合のみ参照可（それ以外 403）

PUT /tl:
  1. TL ロール必須（ADMIN は編集不可）
  2. 対象ユーザーの tl_user_id = 自分の ID であること（それ以外 403）

PUT /company:
  1. ADMIN ロール必須（TL は編集不可）
```

---

## GET /api/users/{userId}/expectations

指定ユーザーの期待コメントを取得する。

**権限**: TL（担当チームのみ）/ ADMIN

**Path Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `userId` | int | ユーザーの内部 ID（`users.id`） |

**Response 200**

```json
{
  "tlExpectation": "来期はチームをリードできる立場になってほしい。",
  "companyExpectation": "技術リーダーとしての貢献を期待する。"
}
```

> `tlExpectation` / `companyExpectation` はそれぞれ未入力の場合 `null` を返す。  
> 対象ユーザーの `user_expectations` レコードが未作成の場合も `{ tlExpectation: null, companyExpectation: null }` を返す（404 ではない）。

**Response 403**

```json
{ "code": "FORBIDDEN", "message": "このユーザーへのアクセス権限がありません" }
```

---

## PUT /api/users/{userId}/expectations/tl

TL期待コメントを保存する（INSERT or UPDATE）。

**権限**: TL（対象ユーザーの担当TLのみ）

**Path Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `userId` | int | ユーザーの内部 ID（`users.id`） |

**Request Body**

```json
{
  "expectation": "来期はチームをリードできる立場になってほしい。"
}
```

| フィールド | 型 | 必須 | 説明 |
|-----------|-----|:---:|------|
| `expectation` | string | ○ | TL期待コメント（空文字可） |

**Response 200**

```json
{
  "tlExpectation": "来期はチームをリードできる立場になってほしい。",
  "companyExpectation": "技術リーダーとしての貢献を期待する。"
}
```

**Response 403**

```json
{ "code": "FORBIDDEN", "message": "このユーザーへのアクセス権限がありません" }
```

---

## PUT /api/users/{userId}/expectations/company

会社期待コメントを保存する（INSERT or UPDATE）。

**権限**: ADMIN

**Path Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `userId` | int | ユーザーの内部 ID（`users.id`） |

**Request Body**

```json
{
  "expectation": "技術リーダーとしての貢献を期待する。"
}
```

| フィールド | 型 | 必須 | 説明 |
|-----------|-----|:---:|------|
| `expectation` | string | ○ | 会社期待コメント（空文字可） |

**Response 200**: GET と同形式（保存後の両コメントを返す）

**Response 403**

```json
{ "code": "FORBIDDEN", "message": "このユーザーへのアクセス権限がありません" }
```

---

## バックエンド実装メモ

### パッケージ構成

```
com.skilize.expectation
├── presentation/
│   ├── ExpectationController.java     ← GET / PUT /tl / PUT /company
│   ├── ExpectationResponse.java       ← record（tlExpectation, companyExpectation）
│   └── SaveExpectationRequest.java    ← record（expectation）
├── application/
│   └── ExpectationService.java        ← @Transactional（getForUser / saveTlExpectation / saveCompanyExpectation）
└── domain/
    ├── UserExpectation.java            ← Entity（@MapsId で user_id を PK に使用）
    └── UserExpectationRepository.java  ← JpaRepository<UserExpectation, Integer>
```

### フロントエンド構成

```
features/team/
├── api/userApi.ts    ← getExpectations / saveTlExpectation / saveCompanyExpectation を追加
└── types/index.ts    ← UserExpectation インターフェースを追加
```

> 期待コメントの UI は `features/team/pages/MemberDetailPage.tsx` の「期待」タブとして実装している。  
> TL ロールの場合は TL期待コメントの入力欄を表示し、ADMIN ロールの場合は会社期待コメントの入力欄を表示する。  
> 自分が編集権限を持たない項目は読み取り専用で表示する。

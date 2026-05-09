# 認証 API

**対象画面**: SCR-001（ログイン）、パスワード変更

---

## エンドポイント一覧

| メソッド | パス | 概要 | 権限 |
|---------|------|------|------|
| POST | /api/auth/login | ログイン | 不要 |
| POST | /api/auth/logout | ログアウト | 全員 |
| POST | /api/auth/change-password | パスワード変更 | 全員 |
| GET | /api/auth/me | 自分のユーザー情報取得 | 全員 |

---

## POST /api/auth/login

ユーザーIDとパスワードで認証し JWT を返す。

**権限**: 不要

**Request Body**

```json
{
  "userId": "tanaka.taro",
  "password": "s3cr3tP@ss"
}
```

**Response 200**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "name": "田中 太郎",
    "role": "GENERAL",
    "isInitialPassword": false,
    "tlUser": {
      "id": 5,
      "name": "山田 花子"
    }
  }
}
```

> `isInitialPassword: true` の場合、フロントエンドはパスワード変更画面へ強制遷移する。  
> `tlUser` はTL未設定の場合 `null`。

**Response 401**

```json
{ "code": "AUTH_FAILED", "message": "ユーザーIDまたはパスワードが違います" }
```

**Response 403**

```json
{ "code": "FORBIDDEN", "message": "このアカウントは無効化されています" }
```

---

## POST /api/auth/logout

JWT をサーバー側でブラックリスト登録する（実装上はステートレスでもよい）。

**権限**: 全員

**Request Body**: なし

**Response 204**: No Content

---

## POST /api/auth/change-password

パスワードを変更する。初回ログイン時は `currentPassword` に初期パスワードを指定する。

**権限**: 全員

**Request Body**

```json
{
  "currentPassword": "oldP@ss",
  "newPassword": "newP@ss123"
}
```

**Response 204**: No Content

変更成功後、`isInitialPassword` が `false` に更新される。

**Response 400**

```json
{ "code": "AUTH_FAILED", "message": "現在のパスワードが正しくありません" }
```

```json
{
  "code": "VALIDATION_ERROR",
  "message": "入力値に誤りがあります",
  "errors": [
    { "field": "newPassword", "message": "8 文字以上で入力してください" }
  ]
}
```

---

## GET /api/auth/me

ページリロード時などに現在ログイン中のユーザー情報を取得する。

**権限**: 全員

**Response 200**

```json
{
  "id": 1,
  "userId": "tanaka.taro",
  "name": "田中 太郎",
  "email": "tanaka@example.com",
  "role": "GENERAL",
  "isInitialPassword": false,
  "tlUser": {
    "id": 5,
    "name": "山田 花子"
  },
  "isActive": true
}
```

> `email` は登録されていない場合 `null`。

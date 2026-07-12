---
paths:
  - "**/*.java"
---

# Backend Comment Rules（Java / Spring Boot）

**対象言語・フレームワーク: Java（Spring Boot）**

[../comments.md](../comments.md) の共通ルールに対する、Java / Spring Boot 固有の記載例と追加ルール。

---

# ファイルヘッダー（Java 記載例）

```java
/**************************************************************************************************************
 * 機能ID      ：AUTH
 * 機能名      ：認証機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ログイン・パスワード変更・JWT 発行を行う認証機能のビジネスロジック。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
```

---

# クラスコメント

対象: Controller / Service / Entity / Request / Response / Command / QueryResult

```java
/**
 * 認証サービス。
 *
 * ログイン・パスワード変更・自情報取得のビジネスロジックを提供する。
 * ユーザー ID の存在有無を外部に漏らさないため、ユーザー不在とパスワード不一致で同一エラーを返す。
 */
@Service
public class AuthService {
```

---

# メソッドコメント

public メソッドは必須、protected は推奨、private は複雑な処理のみ。Javadoc の `@param` / `@return` / `@throws` を用いる。

```java
/**
 * ログイン処理。ユーザー ID・パスワードを検証し、成功時に JWT を発行して返す。
 * ユーザー不在とパスワード不一致を同一エラーにすることで、ユーザー ID の存在有無を外部に漏らさない。
 *
 * @param command ログインコマンド（ユーザー ID・パスワード）
 * @return ログイン結果（JWT・ユーザー情報）
 * @throws AuthException 認証失敗時またはアカウント無効時
 */
public LoginQueryResult login(LoginCommand command) {
```

---

# 業務ロジックコメント

```java
// 新規ユーザーは必ず初回パスワード変更が必要
u.initialPassword = true;
```

```java
// ユーザー列挙攻撃対策のため、ユーザー不在とパスワード不一致で同一エラーを返す
throw new AuthException("AUTH_FAILED", "");
```

---

# データ項目コメント

Entity のフィールドには意味・制約を必須で記載する。

```java
/** パスワードハッシュ（BCrypt コスト 12・API レスポンスに含めない） */
@Column(name = "password_hash", nullable = false)
private String passwordHash;

/** 初回パスワードフラグ（true=パスワード変更強制。InitialPasswordFilter が参照する） */
@Column(name = "is_initial_password", nullable = false)
private boolean initialPassword;
```

record（Request / Response / Command / QueryResult）はコンストラクタ引数にコメントを付けず、クラスコメントに主要項目の意味をまとめて記載する。

```java
/**
 * ログイン結果。
 *
 * token: JWT アクセストークン（ローカルストレージに保存して Bearer トークンとして送信する）
 * userInfo: ログインユーザー情報（フロントエンドの状態管理に使用する）
 */
public record LoginQueryResult(String token, UserInfo userInfo) {
```

---

# Spring 固有ルール

### Service

業務ルールがある箇所には必ずコメントを記載する。

```java
// SecurityContext のユーザーは JPA 管理外の可能性があるため、
// ID で再フェッチしてトランザクション内で更新する
User user = userRepository.findById(currentUser.getId()).orElseThrow();
```

### Filter

処理フローに関するコメントを記載する（例: フィルターチェーンの順序）。

### Repository

原則コメント不要とする。複雑な JPQL クエリのみコメントを記載する。

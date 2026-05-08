# セキュリティ設計

**バージョン**: 1.0.0  
**作成日**: 2026-05-09

---

## 1. 概要

| 項目 | 内容 |
|------|------|
| 認証方式 | JWT（Bearer トークン）|
| 認可方式 | ロールベースアクセス制御（RBAC）|
| パスワードハッシュ | BCrypt（コストファクター: 12）|
| セッション管理 | ステートレス（JWT のみ。サーバーサイドセッションなし）|
| 通信 | HTTPS（本番 EC2 + Nginx）|

---

## 2. 認証

### 2.1 認証フロー

```
① クライアント  →  POST /api/auth/login { email, password }
② バックエンド  →  DB から users を検索 → BCrypt でパスワード照合
③ バックエンド  →  JWT を生成してレスポンスで返す
④ クライアント  →  JWT を localStorage / sessionStorage に保存
⑤ クライアント  →  以降のリクエストに Authorization: Bearer <token> を付与
⑥ バックエンド  →  JwtAuthenticationFilter で JWT を検証 → SecurityContext に認証情報をセット
```

### 2.2 JWT 設計

| 項目 | 値 |
|------|-----|
| 署名アルゴリズム | HS256（HMAC-SHA256）|
| 署名キー | 256 bit 以上のランダム文字列（環境変数 `JWT_SECRET` で管理）|
| 有効期限 | 8 時間（環境変数 `JWT_EXPIRATION_HOURS` で変更可）|
| ライブラリ | jjwt（io.jsonwebtoken）|

#### JWT ペイロード

```json
{
  "sub":  "1",
  "name": "田中 太郎",
  "role": "GENERAL",
  "iat":  1746700000,
  "exp":  1746728800
}
```

> `sub` はユーザー ID（文字列）。ロールはサーバーサイドでも DB 照合するため、JWT の `role` 値を信頼しすぎない。

### 2.3 ログアウト処理

REST API はステートレスのためサーバー側でトークンを破棄しない。  
クライアント側でトークンを削除することでログアウトとする。

> 厳密なトークン無効化が必要な場合（将来対応）: Redis ブラックリストを検討。

---

## 3. 認可

### 3.1 ロール定義

| ロール | 値 | 説明 |
|--------|-----|------|
| 一般ユーザー | `GENERAL` | 自分の棚卸のみ操作可 |
| チームリーダー | `TL` | 自分の棚卸 + 自チームメンバーの照会 + マスタ管理（TL 権限範囲）|
| 管理者 | `ADMIN` | 全操作可 |

### 3.2 エンドポイント別アクセス制御マトリクス

凡例: ○ = アクセス可 / — = 不可 / 条件付きは注釈

#### 認証・共通

| エンドポイント | GENERAL | TL | ADMIN |
|---------------|:-------:|:--:|:-----:|
| POST /api/auth/login | ○（認証不要） | ○ | ○ |
| POST /api/auth/logout | ○ | ○ | ○ |
| POST /api/auth/change-password | ○ | ○ | ○ |
| GET /api/auth/me | ○ | ○ | ○ |

#### 棚卸・目標設定

| エンドポイント | GENERAL | TL | ADMIN |
|---------------|:-------:|:--:|:-----:|
| GET /api/dashboard | ○ | ○ | ○ |
| GET /api/inventories/mine | ○ | ○ | ○ |
| POST /api/inventories | ○ | ○ | ○ |
| GET /api/inventories/{id} | 自分のみ | 自分のみ | ○ |
| PUT /api/inventories/{id}/it-skill-details | 自分のみ | 自分のみ | 自分のみ |
| PUT /api/inventories/{id}/qualification-details | 自分のみ | 自分のみ | 自分のみ |
| PUT /api/inventories/{id}/seminar-details | 自分のみ | 自分のみ | 自分のみ |
| POST /api/inventories/{id}/submit | 自分のみ | 自分のみ | 自分のみ |
| GET /api/inventories/{id}/comparison | 自分のみ | 自分のみ | 自分のみ |
| PATCH /api/inventories/{id}/it-skill-details/{detailId} | 自分のみ | 自分のみ | 自分のみ |
| GET /api/inventories/{id}/goals | 自分のみ | 自分のみ | 自分のみ |
| PUT /api/inventories/{id}/goals | 自分のみ | 自分のみ | 自分のみ |
| POST /api/inventories/{id}/goals/complete | 自分のみ | 自分のみ | 自分のみ |

#### 照会

| エンドポイント | GENERAL | TL | ADMIN |
|---------------|:-------:|:--:|:-----:|
| GET /api/users/me/inventories | ○ | ○ | ○ |
| GET /api/users/{userId}/inventories | — | 自チームのみ ※1 | ○ |
| GET /api/inventories/{id}/detail | 自分のみ | 自チームのみ ※1 | ○ |
| GET /api/users/me/team-members | — | ○ | ○ |
| GET /api/users | — | — | ○ |

> ※1 「自チーム」= `users.tl_user_id = 自分の ID` のユーザー

#### マスタ管理（TL / ADMIN）

| エンドポイント | GENERAL | TL | ADMIN |
|---------------|:-------:|:--:|:-----:|
| GET /api/it-skills | ○ | ○ | ○ |
| POST /api/it-skills | — | ○ | ○ |
| PUT /api/it-skills/{id} | — | ○ | ○ |
| DELETE /api/it-skills/{id} | — | ○ | ○ |
| PATCH /api/it-skills/{id}/restore | — | ○ | ○ |
| GET /api/it-skills/custom-unregistered | — | ○ | ○ |
| POST /api/it-skills/promote | — | ○ | ○ |
| GET /api/qualifications | ○ | ○ | ○ |
| POST /api/qualifications | — | ○ | ○ |
| PUT /api/qualifications/{id} | — | ○ | ○ |
| DELETE /api/qualifications/{id} | — | ○ | ○ |
| PATCH /api/qualifications/{id}/restore | — | ○ | ○ |
| GET /api/qualifications/custom-unregistered | — | ○ | ○ |
| POST /api/qualifications/promote | — | ○ | ○ |
| GET /api/it-skill-categories | — | ○ | ○ |

#### マスタ管理（ADMIN のみ）

| エンドポイント | GENERAL | TL | ADMIN |
|---------------|:-------:|:--:|:-----:|
| POST /api/it-skill-categories | — | — | ○ |
| PUT /api/it-skill-categories/{id} | — | — | ○ |
| DELETE /api/it-skill-categories/{id} | — | — | ○ |
| GET /api/skill-levels | ○ | ○ | ○ |
| POST /api/skill-levels | — | — | ○ |
| PUT /api/skill-levels/{id} | — | — | ○ |
| DELETE /api/skill-levels/{id} | — | — | ○ |
| GET /api/ad-seminars | ○ | ○ | ○ |
| POST /api/ad-seminars | — | — | ○ |
| PUT /api/ad-seminars/{id} | — | — | ○ |
| DELETE /api/ad-seminars/{id} | — | — | ○ |
| PATCH /api/ad-seminars/{id}/restore | — | — | ○ |
| POST /api/users | — | — | ○ |
| PUT /api/users/{id} | — | — | ○ |
| PATCH /api/users/{id}/deactivate | — | — | ○ |
| PATCH /api/users/{id}/activate | — | — | ○ |
| GET /api/fiscal-years | ○ | ○ | ○ |
| GET /api/fiscal-years/current | ○ | ○ | ○ |
| POST /api/fiscal-years | — | — | ○ |
| PUT /api/fiscal-years/{id} | — | — | ○ |
| GET /api/fiscal-year-settings | — | — | ○ |
| PUT /api/fiscal-year-settings | — | — | ○ |

---

## 4. Spring Security 設定方針

### 4.1 全体構成

```
HTTP リクエスト
    │
    ▼
CorsFilter（Spring が自動適用）
    │
    ▼
JwtAuthenticationFilter      ← JWT 検証 → SecurityContext にセット
    │
    ▼
InitialPasswordFilter         ← is_initial_password=true 時に PW変更以外をブロック
    │
    ▼
SecurityFilterChain（認可チェック）
    │
    ▼
Controller → Service → Repository
```

### 4.2 SecurityFilterChain

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // REST API のため CSRF 無効
        .csrf(AbstractHttpConfigurer::disable)

        // JWT 使用のためセッションはステートレス
        .sessionManagement(sm ->
            sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // CORS 設定（corsConfigurationSource() を参照）
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // エンドポイント別認証設定
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
            .anyRequest().authenticated()
        )

        // カスタムフィルターの追加
        .addFilterBefore(jwtAuthenticationFilter,
                         UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(initialPasswordFilter,
                        JwtAuthenticationFilter.class);

    return http.build();
}
```

> ロール別アクセス制御は `@PreAuthorize` アノテーションで各 Controller / Service に実装する（後述 4.5）。

### 4.3 JwtAuthenticationFilter

リクエストごとに JWT を検証して認証情報を SecurityContext にセットする。

```
処理フロー:
1. Authorization ヘッダーから "Bearer <token>" を抽出
2. JWT を検証（署名・有効期限）
3. sub（user_id）を取得して DB からユーザーを取得
4. ユーザーが is_active=true であることを確認
5. UsernamePasswordAuthenticationToken を SecurityContext にセット
6. 失敗時は SecurityContext をクリアして次のフィルターへ（401 はエントリーポイントで返す）
```

### 4.4 InitialPasswordFilter

初回ログイン強制パスワード変更のためのフィルター。

```
処理フロー:
1. 認証済みユーザーの is_initial_password を確認
2. true の場合、以下のパスのみ通過を許可:
   - POST /api/auth/change-password
   - POST /api/auth/logout
   - GET  /api/auth/me
3. 上記以外のリクエストは 403 を返す
   Response: { "code": "INITIAL_PASSWORD_REQUIRED",
               "message": "初期パスワードの変更が必要です" }
```

### 4.5 ロール制御（@PreAuthorize）

Spring Security の `@EnableMethodSecurity` を有効にして、Controller または Service にアノテーションで制御する。

```java
// TL または ADMIN のみ
@PreAuthorize("hasAnyRole('TL', 'ADMIN')")
public ItSkillResponse createItSkill(...) { ... }

// ADMIN のみ
@PreAuthorize("hasRole('ADMIN')")
public UserResponse createUser(...) { ... }

// 自分のリソースのみ（サービス層でチェック）
@PreAuthorize("isAuthenticated()")
public InventoryResponse getInventory(Long inventoryId) {
    // サービス層で userId の一致を確認
    // TL / ADMIN は自チーム / 全員を許可
}
```

#### リソースオーナーチェック（サービス層）

棚卸など「自分のリソースのみ」操作可能なエンドポイントは、サービス層で以下を確認する。

```
1. inventories.user_id = 認証ユーザーの ID か
2. TL の場合: 対象ユーザーの tl_user_id = 自分の ID か
3. ADMIN の場合: 無条件で許可
4. いずれにも該当しない場合: 403 FORBIDDEN を返す
```

### 4.6 CORS 設定

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    // フロントエンドのオリジン（環境変数で管理）
    config.setAllowedOrigins(List.of(frontendOrigin));
    config.setAllowedMethods(
        List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(
        List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);  // プリフライトキャッシュ: 1時間

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

---

## 5. パスワード管理

### 5.1 ハッシュ化

- アルゴリズム: **BCrypt**
- コストファクター: **12**（ログイン時の照合が約 300〜500ms となる値）
- Spring Security の `BCryptPasswordEncoder` を使用

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

### 5.2 初期パスワード生成

管理者がユーザーを作成した際にサーバー側でランダム生成する。

| 項目 | 仕様 |
|------|------|
| 文字種 | 大文字英字・小文字英字・数字・記号（`!@#$%^&*`）|
| 長さ | 12文字 |
| 形式 | `XXXX-XXXX-XXXX`（ハイフン区切りで可読性を確保）|
| 保存 | BCrypt でハッシュ化してDBに保存 |
| 表示 | 平文は登録完了レスポンスに **一度だけ** 返す。以後参照不可 |

### 5.3 パスワード変更強制

| フラグ | 値 | 動作 |
|--------|-----|------|
| `is_initial_password` | `true` | InitialPasswordFilter がパスワード変更以外のリクエストをブロック |
| `is_initial_password` | `false` | 通常操作を許可 |

パスワード変更（`POST /api/auth/change-password`）成功後、`is_initial_password` を `false` に更新する。

---

## 6. 環境変数

セキュリティに関する設定値はすべて環境変数で管理し、ソースコードにハードコードしない。

| 変数名 | 説明 | 例 |
|--------|------|-----|
| `JWT_SECRET` | JWT 署名キー（256 bit 以上のランダム文字列）| `your-very-long-random-secret` |
| `JWT_EXPIRATION_HOURS` | JWT 有効期限（時間）| `8` |
| `FRONTEND_ORIGIN` | CORS 許可オリジン | `http://localhost:3000` |
| `BCRYPT_STRENGTH` | BCrypt コストファクター | `12` |

`.env.example` に変数名とダミー値を記載し、`.env` は `.gitignore` で管理する。

---

## 7. その他セキュリティ考慮点

### 7.1 HTTPS

- 本番環境では Nginx で SSL/TLS 終端を行い、バックエンドへは HTTP で転送する
- ローカル開発は HTTP 許容

### 7.2 レスポンスヘッダー

Spring Security のデフォルトで以下のヘッダーが付与される。追加の設定は不要。

| ヘッダー | 値 |
|---------|-----|
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Cache-Control` | `no-cache, no-store, max-age=0` |

### 7.3 ログイン失敗時の挙動

- 認証失敗は常に同一のエラーメッセージ（`AUTH_FAILED`）を返し、メールアドレスの存在有無を漏らさない
- アカウントロック機能は現時点では実装しない（TBD）

### 7.4 SQL インジェクション対策

- Spring Data JPA / JPQL を使用し、プレースホルダーバインドを徹底する
- ネイティブクエリを使用する場合も `@Param` でバインドし、文字列結合禁止

### 7.5 入力バリデーション

- Controller 層で `@Valid` / Bean Validation アノテーションによるバリデーションを実施
- フロントエンドのバリデーションを信頼せず、バックエンドでも必ず検証する

### 7.6 機密データの取り扱い

| データ | 方針 |
|--------|------|
| `password_hash` | API レスポンスに含めない |
| `initialPassword`（初期パスワード平文）| 登録完了レスポンス以外に含めない |
| JWT 署名キー | 環境変数のみ。ログ出力禁止 |

### 7.7 未実装・TBD

| 項目 | 内容 |
|------|------|
| アカウントロック | ログイン失敗 N 回でロック（2次開発） |
| トークンリフレッシュ | リフレッシュトークンによる JWT 延長（2次開発） |
| 監査ログ | 操作ログの記録・参照（非機能要件に記載、実装スコープは別途定義） |

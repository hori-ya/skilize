# テスト仕様書 — Backend / 認証

**コンテナ**: バックエンド（Spring Boot / JUnit 5）  
**機能**: 認証（JWT・ログイン・パスワード管理）

> テストの実行方法 → [`running-tests.md`](../running-tests.md)

---

## 1. JwtUtilTest

**ファイル**: `apps/backend/src/test/java/com/skilize/shared/infrastructure/JwtUtilTest.java`  
**テスト対象**: `com.skilize.shared.infrastructure.JwtUtil`  
**テスト種別**: 単体テスト（`@ExtendWith(MockitoExtension.class)`）  
**テスト設定**: `secret`・`expirationMs` を `ReflectionTestUtils` で注入

| テスト ID | テスト名 | 前提条件 | 入力 | 期待結果 |
|---|---|---|---|---|
| BE-JWT-001 | `generateToken_正常系_3パートのJWT文字列を返す` | 有効な秘密鍵・有効期限が設定済み | `userId="test-user-id"` | `.` 区切り 3 パートの JWT 文字列が返る |
| BE-JWT-002 | `extractUserId_正常系_ユーザー内部IDを文字列で返す` | 有効な JWT が生成済み | 正常な JWT 文字列 | `userId` 文字列が返る |
| BE-JWT-003 | `isTokenValid_有効なトークン_trueを返す` | 有効な秘密鍵・有効期限が設定済み | 正常な JWT 文字列 | `true` が返る |
| BE-JWT-004 | `isTokenValid_不正な文字列_falseを返す` | — | `"invalid.token.string"` | `false` が返る |
| BE-JWT-005 | `isTokenValid_空文字_falseを返す` | — | `""` | `false` が返る |
| BE-JWT-006 | `isTokenValid_期限切れトークン_falseを返す` | `expirationMs=-1000`（過去日時）で生成 | 期限切れ JWT 文字列 | `false` が返る |
| BE-JWT-007 | `isTokenValid_異なる秘密鍵で検証_falseを返す` | — | 別の秘密鍵で生成した JWT | `false` が返る |
| BE-JWT-008 | `generateAndExtract_ラウンドトリップ_整合性を確認` | — | `userId="round-trip-user"` | 生成 → 抽出で元の `userId` が復元される |

---

## 2. AuthServiceTest

**ファイル**: `apps/backend/src/test/java/com/skilize/auth/application/AuthServiceTest.java`  
**テスト対象**: `com.skilize.auth.application.AuthService`  
**テスト種別**: 単体テスト（Mockito）  
**モック対象**: `UserRepository`, `PasswordEncoder`, `JwtUtil`

### 2.1 Login

| テスト ID | テスト名 | 前提条件 | 入力 | 期待結果 |
|---|---|---|---|---|
| BE-AS-L-001 | `正常系_JWTとユーザー情報を返す` | アクティブなユーザーが存在、パスワード一致 | `userId`, `password` | JWT 文字列 + ユーザー情報が返る |
| BE-AS-L-002 | `異常系_ユーザー不在_AUTH_FAILEDをスロー` | 指定 `userId` のユーザーが存在しない | 存在しない `userId` | `AuthException(AUTH_FAILED)` がスローされる |
| BE-AS-L-003 | `異常系_パスワード不一致_AUTH_FAILEDをスロー` | ユーザーは存在、パスワード不一致 | 誤った `password` | `AuthException(AUTH_FAILED)` がスローされる |
| BE-AS-L-004 | `異常系_無効化アカウント_FORBIDDENをスロー` | ユーザーが存在、`isActive=false` | `userId`, `password` | `AuthException(FORBIDDEN)` がスローされる |
| BE-AS-L-005 | `異常系_ユーザー不在とパスワード不一致は同一エラー_ユーザー列挙攻撃対策` | — | 存在しない `userId` / 不正な `password` | どちらも `AUTH_FAILED` エラーコード（列挙攻撃対策）|

### 2.2 ChangePassword

| テスト ID | テスト名 | 前提条件 | 入力 | 期待結果 |
|---|---|---|---|---|
| BE-AS-CP-001 | `正常系_パスワードが更新されisInitialPasswordがfalseになる` | 一般ユーザー、現在パスワード一致 | `currentPassword`, `newPassword` | `passwordHash` 更新・`isInitialPassword=false` になる |
| BE-AS-CP-002 | `正常系_adminアカウント_パスワードが更新されisInitialPasswordがfalseになる` | admin ユーザー、現在パスワード一致 | `currentPassword`, `newPassword` | `passwordHash` 更新・`isInitialPassword=false` になる |
| BE-AS-CP-003 | `異常系_現在のパスワード不一致_AUTH_FAILEDをスロー` | 現在パスワードが不一致 | 誤った `currentPassword` | `AuthException(AUTH_FAILED)` がスローされる |

### 2.3 GetMe

| テスト ID | テスト名 | 前提条件 | 入力 | 期待結果 |
|---|---|---|---|---|
| BE-AS-GM-001 | `正常系_ユーザー情報を返す` | 一般ユーザー（TL 設定なし） | `userInternalId` | `userId`・`role`・`isInitialPassword` 等のユーザー情報が返る |
| BE-AS-GM-002 | `正常系_TLユーザー設定あり_TL情報を返す` | TL ユーザー（`tlUser` 設定あり） | `userInternalId` | TL 情報を含むユーザー情報が返る |
| BE-AS-GM-003 | `正常系_TLユーザー設定なし_tlUserがnull` | TL ユーザーだが `tlUser` 未設定 | `userInternalId` | `tlUser=null` のユーザー情報が返る |

---

## 3. AuthControllerTest

**ファイル**: `apps/backend/src/test/java/com/skilize/auth/presentation/AuthControllerTest.java`  
**テスト対象**: `com.skilize.auth.presentation.AuthController`  
**テスト種別**: Web レイヤーテスト（`@WebMvcTest`）  
**モック対象**: `AuthService`, `AuthApplicationMapper`, `JwtAuthenticationFilter`（素通り設定）, `InitialPasswordFilter`（素通り設定）

### 3.1 POST /api/auth/login

| テスト ID | テスト名 | 前提条件 | 入力 | 期待結果 |
|---|---|---|---|---|
| BE-AC-L-001 | `正常系_200とJWTを返す` | `AuthService.login()` が正常返却 | `{"userId":"admin","password":"admin"}` | 200 OK・レスポンスボディに JWT |
| BE-AC-L-002 | `異常系_userIdが空_400バリデーションエラー` | — | `{"userId":"","password":"admin"}` | 400 Bad Request |
| BE-AC-L-003 | `異常系_passwordが空_400バリデーションエラー` | — | `{"userId":"admin","password":""}` | 400 Bad Request |
| BE-AC-L-004 | `異常系_認証失敗_401を返す` | `AuthService.login()` が `AUTH_FAILED` をスロー | 誤った認証情報 | 401 Unauthorized |
| BE-AC-L-005 | `異常系_アカウント無効化_403を返す` | `AuthService.login()` が `FORBIDDEN` をスロー | 無効化アカウント情報 | 403 Forbidden |

### 3.2 GET /api/auth/me

| テスト ID | テスト名 | 前提条件 | 入力 | 期待結果 |
|---|---|---|---|---|
| BE-AC-GM-001 | `正常系_200とユーザー情報を返す` | 認証済みユーザー | `Authorization` ヘッダー付きリクエスト | 200 OK・ユーザー情報 JSON |
| BE-AC-GM-002 | `異常系_認証なし_401を返す` | 未認証状態 | `Authorization` ヘッダーなし | 401 Unauthorized |

### 3.3 POST /api/auth/change-password

| テスト ID | テスト名 | 前提条件 | 入力 | 期待結果 |
|---|---|---|---|---|
| BE-AC-CP-001 | `正常系_204を返す` | 認証済みユーザー | `{"currentPassword":"...","newPassword":"12345678"}` | 204 No Content |
| BE-AC-CP-002 | `異常系_認証なし_401を返す` | 未認証状態 | `Authorization` ヘッダーなし | 401 Unauthorized |
| BE-AC-CP-003 | `異常系_newPasswordが8文字未満_400バリデーションエラー` | 認証済みユーザー | `{"newPassword":"1234567"}` (7 文字) | 400 Bad Request |

### 3.4 POST /api/auth/logout

| テスト ID | テスト名 | 前提条件 | 入力 | 期待結果 |
|---|---|---|---|---|
| BE-AC-LO-001 | `logout_正常系_204を返す` | 認証済みユーザー | `Authorization` ヘッダー付きリクエスト | 204 No Content |

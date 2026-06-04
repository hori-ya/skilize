# テスト仕様書 — Frontend / 認証

**コンテナ**: フロントエンド（Vitest + React Testing Library）  
**機能**: 認証（ログイン画面）

> テストの実行方法 → [`running-tests.md`](../running-tests.md)

---

## 1. LoginPage.test.tsx

**ファイル**: `apps/frontend/src/features/auth/pages/LoginPage.test.tsx`  
**テスト対象**: `LoginPage` コンポーネント  
**モック対象**: `useAuth`, `useNavigate`, `useTranslation`, `SkilizeLogo`, `IconLogin`

### 1.1 表示テスト

| テスト ID | テスト名 | 前提条件 | 操作 | 期待結果 |
|---|---|---|---|---|
| FE-LP-001 | `正常系_未ログイン時_ログインフォームが表示される` | `user=null, isLoading=false` | — | ユーザー ID・パスワード入力欄・ログインボタンが表示される |
| FE-LP-002 | `正常系_isLoading中_フォームを表示しない` | `isLoading=true` | — | フォーム要素が表示されない |
| FE-LP-003 | `正常系_既ログイン済み非初回パスワード_ダッシュボードにリダイレクトする` | `user.isInitialPassword=false` | — | `/` へリダイレクトされる |
| FE-LP-004 | `正常系_既ログイン済み初回パスワード_パスワード変更画面にリダイレクトする` | `user.isInitialPassword=true` | — | `/change-password` へリダイレクトされる |

### 1.2 ログイン操作テスト

| テスト ID | テスト名 | 前提条件 | 操作 | 期待結果 |
|---|---|---|---|---|
| FE-LP-005 | `正常系_ログイン成功_通常ユーザー_ダッシュボードに遷移する` | `login()` が `isInitialPassword=false` のユーザーを返す | ユーザー ID・パスワード入力 → ログインボタン押下 | `/` へ遷移される |
| FE-LP-006 | `正常系_ログイン成功_初回パスワード変更ユーザー_パスワード変更画面に遷移する` | `login()` が `isInitialPassword=true` のユーザーを返す | ユーザー ID・パスワード入力 → ログインボタン押下 | `/change-password` へ遷移される |
| FE-LP-007 | `異常系_認証失敗_invalidCredentialsエラーメッセージが表示される` | `login()` が `AxiosError(AUTH_FAILED, 401)` をスロー | ユーザー ID・パスワード入力 → ログインボタン押下 | `invalidCredentials` エラーメッセージが表示される |
| FE-LP-008 | `異常系_アカウント無効_accountDisabledエラーメッセージが表示される` | `login()` が `AxiosError(ACCOUNT_DISABLED, 403)` をスロー | ユーザー ID・パスワード入力 → ログインボタン押下 | `accountDisabled` エラーメッセージが表示される |
| FE-LP-009 | `異常系_ネットワークエラー_networkErrorエラーメッセージが表示される` | `login()` がネットワークエラーをスロー | ユーザー ID・パスワード入力 → ログインボタン押下 | `networkError` エラーメッセージが表示される |
| FE-LP-010 | `異常系_送信中_ボタンが無効化される` | `login()` が非同期で遅延する | ログインボタン押下（応答待ち中） | ログインボタンが `disabled` 状態になる |

---
paths:
  - "**/*.ts"
  - "**/*.tsx"
---

# Frontend Comment Rules（TypeScript / React）

**対象言語・フレームワーク: TypeScript（React）**

[../comments.md](../comments.md) の共通ルールに対する、TypeScript / React 固有の記載例と追加ルール。

---

# ファイルヘッダー（TypeScript 記載例）

```typescript
/*******************************************************************************
 * 機能ID      ：AUTH
 * 機能名      ：認証機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * ログインページ。ユーザー ID・パスワードを入力して認証を行う。
 * 初回ログイン時はパスワード変更ページへリダイレクトする。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
```

---

# クラス・コンポーネントコメント

全てのコンポーネントに JSDoc 形式のコメントを記載する。

```typescript
/**
 * ログインページ。
 *
 * ユーザー ID・パスワードを入力して認証を行う。
 * 初回ログイン時はパスワード変更ページへリダイレクトする。
 */
export default function LoginPage() {
```

---

# 関数コメント

```typescript
/**
 * ユーザー一覧取得。
 *
 * @param fiscalYearId 対象年度 ID
 * @returns ユーザー一覧
 */
const getUsers = async (fiscalYearId: number): Promise<User[]> => {
```

---

# 業務ロジックコメント

```typescript
// isInitialPassword が true の場合はパスワード変更ページへリダイレクトする業務ルール
if (user.isInitialPassword) navigate('/change-password');
```

---

# React 固有ルール

### コンポーネント

全てのコンポーネントにクラスコメント（JSDoc 形式）を記載する。

```typescript
/**
 * 認証ガード。未認証ユーザーをログインページへリダイレクトする。
 */
export default function PrivateRoute({ children }: { children: React.ReactNode }) {
```

### State 変数

意味が自明でない場合のみコメントを記載する。

```typescript
// 選択中のユーザー ID（メンバー詳細表示の制御に使用する）
const [selectedUserId, setSelectedUserId] = useState<number>();
```

### useEffect

処理理由を必ず記載する。

```typescript
// 初期表示時に年度一覧とユーザー一覧を取得する
useEffect(() => {
  fetchFiscalYears();
  fetchUsers();
}, []);
```

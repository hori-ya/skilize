# React Frontend Architecture Rules

このプロジェクトのフロントエンドアーキテクチャルールを定義する。実装時は必ず遵守すること。

---

# Goal

* 小規模〜大規模まで拡張可能
* 責務分離
* 保守性向上
* feature isolation
* state management整理
* Claude Codeとの相性向上

---

# Core Principles

最重要原則:

* package by feature
* state locality
* small components
* minimal shared
* separation of concerns
* readability
* maintainability

---

# Project Structure

```text
src/

├── app/
│   ├── providers/
│   │   └── AuthProvider.tsx     ← 認証状態の全体共有（React Context + useAuth hook）
│   └── layouts/
│       └── NavBar.tsx           ← グローバルナビゲーションバー
│
├── shared/
│   ├── api/
│   │   ├── client.ts            ← Axios インスタンス（JWT インターセプター）
│   │   └── masterApi.ts         ← マスタデータ API（複数 feature で共有）
│   ├── types/
│   │   └── master.ts            ← マスタデータ型定義
│   └── ui/
│       ├── PrivateRoute.tsx     ← 認証ガード
│       ├── AdminRoute.tsx       ← ADMIN ロールガード
│       ├── TlAdminRoute.tsx     ← TL/ADMIN ロールガード
│       └── ScrollToTopButton.tsx
│
├── features/
│   ├── auth/                    ← ログイン・パスワード変更
│   ├── inventory/               ← ダッシュボード・棚卸・グラフ
│   ├── team/                    ← チーム照会・メンバー詳細
│   ├── master/                  ← マスタ管理ページ群
│   ├── interview/               ← 面談メモ API・型（UI は team に統合）
│   └── ai-support/              ← AI サポートチャット（ウィジェット・API・型・モジュールストア）
│
└── i18n/
    ├── index.ts                 ← i18next 初期化
    └── locales/ja/              ← 翻訳 JSON（namespace 別）
```

---

# Feature Structure

各 feature は以下の構成を持つ（必要なものだけ作成する）。

```text
features/inventory

├── api/          ← Axios を使った API 呼び出し関数
├── types/        ← 型定義（index.ts / charts.ts 等）
├── components/   ← feature 内の再利用コンポーネント（任意）
└── pages/        ← ページコンポーネント（useTranslation を含む）
```

---

# Module Responsibilities

## components

責務:

* UI rendering
* props rendering
* presentational logic

ルール:

* small componentを維持
* 可能な限りstateless
* business logicを持ち込まない

禁止:

* API call
* heavy state logic
* direct fetch

---

## hooks

責務:

* state management
* side effects
* API coordination
* business flow

例:

```text
useUsers.ts
useCreateUser.ts
useAuth.ts
```

ルール:

* feature内部へ閉じ込める
* reusable logicを抽出

---

## api

責務:

* backend communication
* axios/fetch
* API DTO handling

例:

```text
userApi.ts
orderApi.ts
```

禁止:

* UI logic
* component state

---

## state

責務:

* global UI state
* shared feature state

推奨:

* Zustand
* minimal global state

禁止:

* 全データをglobal state化
* server state保存

---

## pages

責務:

* route composition
* page layout
* feature composition

禁止:

* heavy business logic
* direct API implementation

---

# State Management Rules

stateは種類ごとに分離する。

## Server State（API データ）

推奨:

```text
useEffect + useState + Axios
```

外部ライブラリ（TanStack Query 等）は**使用しない**。

---

## UI State（ローカル状態）

推奨:

```text
useState
useReducer（複雑な場合のみ）
```

---

## Global State

必要最小限のみ。**認証状態のみ** グローバル管理する。

```text
AuthContext（app/providers/AuthProvider.tsx）
  └── useAuth() hook でアクセス
```

禁止:

* Zustand・Redux 等の外部状態管理ライブラリ
* API response の全保存

---

# Shared Rules

shared は最小限にする。

```text
shared
├── ui
├── hooks
├── api
├── lib
├── types
└── constants
```

---

# Shared UI Rules

shared/ui は汎用UIのみ。

許可:

```text
Button
Modal
Input
Table
Card
```

禁止:

```text
UserTable
OrderCard
BillingForm
```

feature固有UIは禁止。

---

# Commonization Rules

共通化ルールを厳守する。

## Rule 1

まず feature 内部へ実装する。

## Rule 2

3回以上重複した場合のみ shared 化を検討する。

## Rule 3

feature固有ロジックを shared に置かない。

## Rule 4

巨大 utils 禁止。

禁止例:

```text
shared/utils/common.ts
shared/utils/helpers.ts
```

## Rule 5

shared/hooks は truly reusable のみ。

許可:

```text
useDebounce
useLocalStorage
```

禁止:

```text
useUserManagement
useOrderWorkflow
```

---

# Component Rules

## Small Component Principle

1 component = 1 responsibility

推奨:

* 200行以下
* component分割を優先

---

## Presentational / Container Separation

推奨:

```text
UserPage
 ├── UserContainer
 └── UserTable
```

UIとロジックを分離する。

---

# API Rules

## API calls must be isolated

禁止:

```tsx
useEffect(() => {
  fetch(...)
})
```

直接fetchをcomponentへ書かない。

---

## API layer example

```text
features/user/api/userApi.ts
```

```ts
export async function fetchUsers() {}
```

---

# React Query Rules

推奨:

* useQuery
* useMutation
* query key管理

禁止:

* useEffect fetch乱立
* 手動cache管理

---

# Form Rules

推奨:

* `useState` でフォーム状態を管理する
* 送信時に手動でバリデーションを行う（必須チェック等）

禁止:

* React Hook Form・Zod 等の外部ライブラリ（このプロジェクトでは使用しない）

---

# Dependency Rules

依存方向:

```text
pages
  ↓
features
  ↓
shared
```

禁止:

* shared → features
* feature間の密結合

---

# Naming Rules

## Components

```text
UserCard.tsx
OrderTable.tsx
```

## Hooks

```text
useUsers.ts
useAuth.ts
```

## API

```text
userApi.ts
billingApi.ts
```

---

# i18n Rules (国際化)

ライブラリ: **i18next + react-i18next**

## ファイル構成

```text
src/
├── i18n/
│   ├── index.ts          ← i18next 初期化（main.tsx でインポート）
│   └── locales/
│       └── ja/
│           ├── common.json     ← ボタン・ラベル・ステータス等の共通文字列
│           ├── nav.json        ← ナビゲーション
│           ├── auth.json       ← ログイン・パスワード変更
│           ├── inventory.json  ← ダッシュボード・棚卸入力・グラフ等
│           ├── team.json       ← チーム照会・メンバー詳細
│           └── master.json     ← マスタ管理全般
```

## Namespace 割り当て

| Namespace | 対象 feature / ファイル |
|---|---|
| `common` | ボタン名・共通ラベル・エラーメッセージ・ステータス表示 |
| `nav` | NavBar |
| `auth` | LoginPage, ChangePasswordPage, MyPasswordPage |
| `inventory` | DashboardPage, InventoryPage, ComparisonPage, GoalPage, GoalReviewPage, InventoryHistoryPage, 各 ChartCard |
| `team` | TeamMemberListPage, AllUserListPage, MemberDetailPage |
| `master` | FiscalYearMasterPage, SkillLevelMasterPage, ItSkillMasterPage, QualificationMasterPage, AdSeminarMasterPage, UserMasterPage |

## 使用ルール

```tsx
// 単一 namespace
const { t } = useTranslation('inventory');

// 複数 namespace（共通も使う場合）
const { t } = useTranslation(['inventory', 'common']);

// 変数補間
t('table.rowCount', { count: 5 })  // "5件"

// ネスト取得
t('form.submitButton')  // "保存"
```

## キー命名規則

- **ネスト階層 2〜3 段**（`section.element` または `section.subsection.element`）
- camelCase のみ（スネークケース・ドット区切り以外禁止）
- 意味が自明なキー名にする（`btn1` などの略称禁止）

```json
{
  "loginForm": {
    "title": "ログイン",
    "userIdLabel": "ユーザーID",
    "passwordLabel": "パスワード",
    "submitButton": "ログイン"
  },
  "error": {
    "invalidCredentials": "ユーザーIDまたはパスワードが正しくありません"
  }
}
```

## 禁止事項

- コンポーネント内にハードコードされた日本語文字列（`className` 値・コメントを除く）
- `any` でのキャスト回避
- 翻訳ファイルへのロジック記述
- 動的キー生成（`t('status.' + code)` → `t(`status.${code}`)` は許容するが過剰な動的化は禁止）

---

# Styling Rules

推奨:

* `src/index.css` に定義された BEM ライクなクラス名を使用する（例: `.btn`, `.btn--primary`, `.master-card__header`）
* inline style は例外的な微調整（width・gap 等）のみ許容

禁止:

* Tailwind CSS・CSS Modules・styled-components（このプロジェクトでは使用しない）
* 新しい CSS ファイルを feature ごとに乱立させる（`index.css` に追記する）

---

# Performance Rules

推奨:

* memoization必要時のみ
* lazy loading（必要に応じて）

禁止:

* premature optimization
* 全component memo化
* query cache（使用しない）

---

# Refactoring Rules

リファクタ時は以下を遵守:

1. feature単位で整理
2. 巨大component分割
3. API分離
4. state局所化
5. shared最小化
6. hooks抽出

大規模一括変更は禁止。

---

# Important Principles

最重要なのは:

* feature isolation
* state locality
* dependency direction
* readability
* maintainability

再利用性より、
変更容易性を優先すること。

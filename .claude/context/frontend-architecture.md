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

├── app
│   ├── router
│   ├── providers
│   ├── layouts
│   └── store
│
├── shared
│   ├── ui
│   ├── api
│   ├── hooks
│   ├── lib
│   ├── types
│   └── constants
│
├── features
│   ├── auth
│   ├── user
│   ├── order
│   └── billing
│
├── pages
│
└── main.tsx
```

---

# Feature Structure

各 feature は以下の構成を持つ。

```text
features/user

├── api
├── components
├── hooks
├── pages
├── state
├── types
├── schemas
└── utils
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

## Server State

APIデータ。

推奨:

* TanStack Query (React Query)

例:

```text
useQuery
useMutation
```

---

## UI State

ローカル状態。

推奨:

```text
useState
useReducer
```

---

## Global State

必要最小限のみ。

例:

* auth session
* theme
* sidebar state

推奨:

* Zustand

禁止:

* API response全保存
* 巨大Redux store

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

* React Hook Form
* Zod validation

validationはschema化する。

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

# Styling Rules

推奨:

* Tailwind CSS
* CSS Modules
* styled-components (必要時のみ)

禁止:

* global CSS乱立
* inline style乱用

---

# Performance Rules

推奨:

* memoization必要時のみ
* lazy loading
* route split
* query cache利用

禁止:

* premature optimization
* 全component memo化

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

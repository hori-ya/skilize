# React Frontend Architecture Rules

このプロジェクトのフロントエンドアーキテクチャルールを定義する。実装時は必ず遵守すること。

---

# Goal

* 小規模〜大規模まで拡張可能
* 責務分離
* 保守性向上
* feature isolation
* state management 整理

---

# Core Principles

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
│   ├── utils/
│   │   └── apiError.ts          ← APIエラーコード取得・翻訳ユーティリティ
│   └── ui/
│       ├── PrivateRoute.tsx     ← 認証ガード
│       ├── AdminRoute.tsx       ← ADMIN ロールガード
│       ├── TlAdminRoute.tsx     ← TL/ADMIN ロールガード
│       ├── ScrollToTopButton.tsx
│       └── ConfirmDialog.tsx
│
├── features/
│   ├── auth/                   ← 認証・パスワード変更
│   ├── inventory/              ← 棚卸入力・提出・前年比較・目標振り返り・棚卸履歴
│   ├── dashboard/              ← ダッシュボード表示
│   ├── charts/                 ← グラフ（スキルバランス・成長推移・ヒートマップ・タイムライン）
│   ├── report/                 ← 棚卸表 PDF ダウンロード
│   ├── ai/                     ← AI チャット・AI キャリア分析
│   ├── user/                   ← ユーザー管理・チーム照会・メンバー詳細
│   ├── expectation/            ← TL/会社からの期待コメント
│   ├── interview/              ← 面談メモ API・型（UI は user に統合）
│   ├── master/                 ← マスタ管理ページ群
│   └── fiscalyear/             ← 年度管理
│
└── i18n/
    ├── index.ts                ← i18next 初期化
    └── locales/ja/             ← 翻訳 JSON（namespace 別）
```

---

# Frontend ↔ Backend Feature Mapping

フロントエンドの `features/{name}/` はバックエンドの `com.skilize.{name}` と 1:1 で対応させること。

| フロントエンド `features/` | バックエンド `com.skilize.` | 主な責務 |
|---|---|---|
| `auth` | `auth` | 認証・パスワード変更 |
| `inventory` | `inventory` | 棚卸入力・提出・前年比較・目標振り返り |
| `dashboard` | `dashboard` | ダッシュボード表示 |
| `charts` | `charts` | スキルバランス・成長推移・ヒートマップ・タイムライン |
| `report` | `report` | 棚卸表 PDF ダウンロード |
| `ai` | `ai` | AI チャット・AI キャリア分析 |
| `user` | `user` | ユーザー管理・チーム照会 |
| `expectation` | `expectation` | TL/会社からの期待コメント |
| `interview` | `interview` | 面談メモ |
| `master` | `master` | マスタ管理（スキルレベル・ITスキル・資格・ADセミナー） |
| `fiscalyear` | `fiscalyear` | 年度管理 |

**ルール:**
- バックエンドに新 feature（パッケージ）を追加した場合は、フロントエンドにも同名の feature フォルダを作成する
- 既存 feature に新しい API を追加する場合も、バックエンドのパッケージ割当に従って feature を選ぶ

---

# Feature Structure

各 feature は以下の構成を持つ（必要なものだけ作成する）。

```text
features/inventory/

├── api/          ← Axios を使った API 呼び出し関数
├── types/        ← 型定義（index.ts 等）
├── components/   ← feature 内の再利用コンポーネント（任意）
└── pages/        ← ページコンポーネント
```

---

# Module Responsibilities

## components

責務: UI rendering、props rendering、presentational logic

禁止: API call、heavy state logic、direct fetch

---

## api

責務: backend communication、axios/fetch、API DTO handling

禁止: UI logic、component state

---

## pages

責務: route composition、page layout、feature composition

禁止: heavy business logic、direct API implementation

---

# State Management Rules

## Server State（API データ）

`useEffect + useState + Axios` を使用する。TanStack Query 等の外部ライブラリは使用しない。

## UI State（ローカル状態）

`useState`、複雑な場合のみ `useReducer`。

## Global State

**認証状態のみ**グローバル管理する。

```text
AuthContext（app/providers/AuthProvider.tsx）
  └── useAuth() hook でアクセス
```

禁止: Zustand・Redux 等の外部状態管理ライブラリ、API response の全保存

---

# Shared Rules

shared は最小限にする。3回以上重複した場合のみ shared 化を検討。feature 固有ロジックを shared に置かない。

```text
shared/
├── api/     ← 複数 feature で使う API クライアント・マスタ API
├── types/   ← 複数 feature で使う型定義
├── utils/   ← 汎用ユーティリティ（apiError.ts 等）
└── ui/      ← 汎用 UI コンポーネント（ルートガード等）
```

---

# Dependency Rules

依存方向:

```text
App.tsx → features → shared
```

禁止:
- `shared` が `features` をインポートする
- feature 間の密結合（型参照は許容）

---

# Form Rules

- `useState` でフォーム状態を管理する
- 送信時に手動でバリデーションを行う

禁止: React Hook Form・Zod 等の外部ライブラリ（このプロジェクトでは使用しない）

---

# Styling Rules

- `src/index.css` に定義された BEM ライクなクラス名を使用する（例: `.btn`, `.btn--primary`）
- inline style は例外的な微調整（width・gap 等）のみ許容

禁止: Tailwind CSS・CSS Modules・styled-components、新しい CSS ファイルを feature ごとに作成する（`index.css` に追記する）

---

# i18n Rules（国際化）

ライブラリ: **i18next + react-i18next**

## ファイル構成

```text
src/i18n/
├── index.ts          ← i18next 初期化（main.tsx でインポート）
└── locales/ja/
    ├── common.json     ← ボタン・ラベル・ステータス等の共通文字列
    ├── nav.json        ← ナビゲーション
    ├── auth.json       ← ログイン・パスワード変更
    ├── inventory.json  ← 棚卸入力・前年比較・目標・棚卸履歴
    ├── user.json       ← チーム照会・メンバー詳細・全ユーザー照会
    ├── master.json     ← マスタ管理全般（年度管理含む）
    ├── ai.json         ← AI チャットウィジェット
    └── errors.json     ← バックエンドエラーコードの翻訳
```

## Namespace 割り当て

| Namespace | 対象ページ / コンポーネント |
|---|---|
| `common` | ボタン名・共通ラベル・ステータス表示 |
| `nav` | NavBar |
| `auth` | LoginPage, ChangePasswordPage, MyPasswordPage |
| `inventory` | InventoryPage, ComparisonPage, GoalPage, GoalReviewPage, InventoryHistoryPage, DashboardPage |
| `user` | TeamMemberListPage, AllUserListPage, MemberDetailPage |
| `master` | FiscalYearMasterPage, SkillLevelMasterPage, ItSkillMasterPage, QualificationMasterPage, AdSeminarMasterPage, UserMasterPage |
| `ai` | AiSupportWidget |
| `errors` | バックエンドから返るエラーコードの翻訳（`response.data.code` → 日本語メッセージ） |

## 使用ルール

```tsx
const { t } = useTranslation('inventory');
t('table.rowCount', { count: 5 })
```

## キー命名規則

- ネスト階層 2〜3 段（`section.element` または `section.subsection.element`）
- camelCase のみ（`btn1` などの略称禁止）
- コンポーネント内に日本語文字列をハードコードしない（`className` 値・コメントを除く）

## APIエラーメッセージ

バックエンドから返るエラーコード（`response.data.code`）を `errors.json` の対応キーで翻訳する。
エラー取得には `shared/utils/apiError.ts` の `getApiErrorMessage()` / `getValidationErrors()` を使用する。
新しいエラーコードを追加した際は必ず `errors.json` にも翻訳を追加する。

---

# Refactoring Rules

1. feature 単位で整理
2. 巨大 component 分割
3. API 分離
4. state 局所化
5. shared 最小化

大規模一括変更は禁止。

---

# Important Principles

* feature isolation
* state locality
* dependency direction
* readability
* maintainability

再利用性より、変更容易性を優先すること。

---

# Directory Responsibilities

## フロントエンド（React / Vite）

| ディレクトリ | 責務 |
|---|---|
| `apps/frontend/src/app/providers/` | AuthProvider（認証状態の全体共有）と useAuth hook |
| `apps/frontend/src/app/layouts/` | NavBar（グローバルナビゲーション） |
| `apps/frontend/src/shared/api/` | Axios クライアント・マスタデータ API（複数 feature で共有） |
| `apps/frontend/src/shared/types/` | マスタデータ型定義（複数 feature で共有） |
| `apps/frontend/src/shared/utils/apiError.ts` | APIエラーコード取得・翻訳ユーティリティ（`getApiErrorMessage` / `getValidationErrors`） |
| `apps/frontend/src/shared/ui/` | ルートガード（PrivateRoute・TlAdminRoute・AdminRoute）・ScrollToTopButton |
| `apps/frontend/src/features/auth/` | ログイン・初回パスワード変更・マイページパスワード変更（API / 型 / ページ） |
| `apps/frontend/src/features/inventory/` | 棚卸入力・提出・前年度比較・目標振り返り・目標設定・棚卸履歴（API / 型 / ページ） |
| `apps/frontend/src/features/dashboard/` | ダッシュボード表示（API / 型 / ページ） |
| `apps/frontend/src/features/charts/` | スキルバランス・成長推移・ヒートマップ・タイムラインのグラフ（API / 型 / コンポーネント） |
| `apps/frontend/src/features/report/` | 棚卸表 PDF ダウンロード（API） |
| `apps/frontend/src/features/ai/` | AI チャット・AI キャリア分析（API / 型 / コンポーネント / ストア） |
| `apps/frontend/src/features/user/` | ユーザー管理・チーム照会・メンバー詳細・全ユーザー照会（API / 型 / ページ） |
| `apps/frontend/src/features/expectation/` | TL/会社からの期待コメント（API / 型） |
| `apps/frontend/src/features/interview/` | 面談メモの API 呼び出し・型定義 |
| `apps/frontend/src/features/master/` | マスタ管理ページ（スキルレベル・ITスキル・資格・ADセミナー・ユーザーマスタ） |
| `apps/frontend/src/features/fiscalyear/` | 年度管理ページ（API は shared/api/masterApi 経由） |
| `apps/frontend/src/i18n/` | i18next 初期設定（`index.ts`）と翻訳 JSON ファイル（`locales/ja/`） |

## Python AI サービス（FastAPI）

| ディレクトリ | 責務 |
|---|---|
| `apps/ai/app/main.py` | FastAPI エントリーポイント・ルーター登録・バリデーションエラーハンドラー |
| `apps/ai/app/api/v1/career_analysis.py` | `POST /analyze` エンドポイント（バックグラウンドタスクで分析を起動し 202 を即返す） |
| `apps/ai/app/api/v1/chat.py` | `POST /chat` エンドポイント（同期・Spring Boot からのプロキシ受付） |
| `apps/ai/app/api/dependencies.py` | `X-Internal-Key` ヘッダーによる内部認証 |
| `apps/ai/app/core/config.py` | 環境変数管理（pydantic-settings の `Settings`） |
| `apps/ai/app/schemas/career_analysis.py` | Pydantic リクエスト型（`AnalyzeRequest`） |
| `apps/ai/app/schemas/chat.py` | Pydantic 型（`ChatRequest` / `ChatResponse` / `ChatMessage`） |
| `apps/ai/app/services/llm.py` | `LLM_PROVIDER` 環境変数で OpenAI / Anthropic を切り替える `build_llm()` |
| `apps/ai/app/services/career_analysis_service.py` | 分析オーケストレーション・PostgreSQL への DB 操作・LangChain チェーン実行 |
| `apps/ai/app/services/chat_service.py` | チャット処理・モード別プロンプト選択・キャリアモード DB 取得 |
| `apps/ai/app/services/prompts/` | LLM へ送るシステムプロンプト・ユーザープロンプトテンプレート |
| `apps/ai/tests/test_chat_service.py` | chat_service のユニットテスト（LLM/DB モック化） |

## インフラ

| ディレクトリ | 責務 |
|---|---|
| `infra/docker/` | 各サービスの Dockerfile・nginx 設定 |
| `infra/compose/` | Docker Compose ファイル（ローカル用・本番用） |

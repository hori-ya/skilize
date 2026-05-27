# システム全体構成

**バージョン**: 1.3.0  
**作成日**: 2026-05-09  
**更新日**: 2026-05-21

---

## 1. システム概要

社員が年次でスキルの棚卸を行うWebアプリケーション。  
フロントエンド（React）・バックエンド（Spring Boot）・AI モジュール（Python）をDockerコンテナで構成し、AWS EC2上で稼働する。  
データベースはAWS RDS（PostgreSQL）を使用する。

---

## 2. 構成図

```
[ ユーザー（ブラウザ）]
        │ HTTPS
        ▼
[ EC2インスタンス ]
  ┌─────────────────────────────────┐
  │  Docker Compose                 │
  │                                 │
  │  ┌───────────┐  ┌────────────┐  │
  │  │  Nginx    │  │  Frontend  │  │
  │  │ (reverse  │─▶│  (React)   │  │
  │  │  proxy)   │  │  :5173     │  │
  │  └─────┬─────┘  └────────────┘  │
  │        │ /api/*                 │
  │        ▼                        │
  │  ┌─────────────┐                │
  │  │  Backend    │─── POST ──▶ ┌──┴──────────┐
  │  │ (Spring     │  (内部のみ)  │  AI Module  │
  │  │  Boot)      │◀── DB ────  │  (Python    │
  │  │  :8080      │             │   FastAPI)  │
  │  └──────┬──────┘             │   :8000     │
  └─────────┼────────────────────┴─────────────┘
            │                         │
            └────────────┬────────────┘
                         ▼
               [ AWS RDS ]
               PostgreSQL 16.4
```

---

## 3. コンポーネント説明

| コンポーネント | 技術 | 役割 |
|--------------|------|------|
| Nginx | nginx:alpine | リバースプロキシ。`/api/*` をバックエンド、それ以外をフロントエンドへルーティング |
| Frontend | React | SPA。画面描画・ユーザー操作の受付 |
| Backend | Spring Boot 4.0.6 / Java 21 | REST API提供。ビジネスロジック・認証・認可 |
| AI Module | Python 3.12 / FastAPI / LangChain | AIキャリア分析。Spring Boot から内部 HTTP で非同期呼び出し。外部公開しない |
| Database | PostgreSQL 16.4（AWS RDS） | データ永続化 |

---

## 4. 認証フロー

```
Browser ──POST /api/auth/login──▶ Backend
                                    │ 認証成功
                                    ▼
Browser ◀──JWT（アクセストークン）── Backend
    │
    │ 以降のAPIリクエストに Authorization: Bearer <token> を付与
    ▼
Backend（Spring Security で検証・認可）
```

---

## 5. フロントエンドアーキテクチャ

フロントエンドは **feature by feature** 構成を採用する。

```
apps/frontend/src/
├── app/
│   ├── providers/AuthProvider.tsx    ← React Context（認証状態）+ useAuth hook
│   └── layouts/NavBar.tsx            ← グローバルナビゲーションバー
├── shared/
│   ├── api/
│   │   ├── client.ts                 ← Axios インスタンス（JWT インターセプター）
│   │   └── masterApi.ts              ← マスタデータ API（fiscal-years, skills 等）
│   ├── types/
│   │   └── master.ts                 ← マスタデータ型定義
│   └── ui/
│       ├── PrivateRoute.tsx          ← 認証ガード
│       ├── AdminRoute.tsx            ← ADMIN ロールガード
│       ├── TlAdminRoute.tsx          ← TL/ADMIN ロールガード
│       └── ScrollToTopButton.tsx     ← スクロールトップボタン
└── features/
    ├── auth/
    │   ├── api/authApi.ts            ← 認証 API（login / changePassword / getMe）
    │   ├── types/index.ts            ← Role / AuthUser / UserAdmin / TlUser
    │   └── pages/                    ← LoginPage / ChangePasswordPage
    ├── inventory/
    │   ├── api/inventoryApi.ts       ← 棚卸 API
    │   ├── api/chartApi.ts           ← グラフ API（radar / growth / heatmap / timeline）
    │   ├── types/index.ts            ← 棚卸関連型（InventorySummary, GoalItem 等）
    │   ├── types/charts.ts           ← グラフレスポンス型
    │   ├── components/               ← RadarChartCard / GrowthChartCard
    │   │                                HeatmapChartCard / TimelineChartCard
    │   └── pages/                    ← DashboardPage / InventoryPage / ComparisonPage
    │                                    GoalReviewPage / GoalPage / InventoryHistoryPage
    ├── team/
    │   ├── api/userApi.ts            ← ユーザー管理 API・期待コメント API
    │   ├── types/index.ts            ← TeamMember（FiscalYearRef, InventoryStatus を参照）・UserExpectation
    │   └── pages/                    ← TeamMemberListPage / MemberDetailPage / AllUserListPage
    ├── master/
    │   └── pages/                    ← FiscalYearMasterPage / SkillLevelMasterPage
    │                                    ItSkillMasterPage / QualificationMasterPage
    │                                    AdSeminarMasterPage / UserMasterPage
    ├── interview/
    │   ├── api/interviewApi.ts       ← 面談メモ API（getInterview / saveInterview / getPrevYearInterview）
    │   └── types/index.ts            ← InterviewMemo / DetailNoteItem / DetailType 型
    └── ai-support/
        ├── api/aiSupportApi.ts       ← AI チャット API（postAiChat）
        ├── components/AiSupportWidget.tsx ← AI ボタン・パネル UI
        ├── types/index.ts            ← AiMode / ChatMessage / AiChatRequest / AiChatResponse
        └── store.ts                  ← モジュールレベルの状態保持（ページ遷移をまたいで復元）

apps/ai/                              ← Python FastAPI（AI モジュール・内部サービス）
├── requirements.txt
└── app/
    ├── main.py                       ← FastAPI エントリーポイント・ルーター登録
    ├── api/v1/
    │   ├── career_analysis.py        ← POST /analyze（非同期・fire-and-forget）
    │   └── chat.py                   ← POST /chat（同期・チャット応答）
    ├── core/config.py                ← 環境変数管理（pydantic-settings）
    ├── schemas/
    │   ├── career_analysis.py        ← AnalyzeRequest
    │   └── chat.py                   ← ChatRequest / ChatResponse / ChatMessage
    └── services/
        ├── llm.py                    ← LLM インスタンス初期化（OpenAI / Anthropic 切り替え）
        ├── career_analysis_service.py ← 分析オーケストレーション・DB 操作
        ├── chat_service.py           ← チャット処理・モード別プロンプト選択
        └── prompts/
            ├── career_analysis_prompt.py ← キャリア分析プロンプト
            └── chat_prompts.py       ← 通常・校正・キャリア・ヘルプ用プロンプト
```

**依存方向**: `App.tsx → features → shared`（`shared` は `features` をインポートしない）  
**feature 間参照**: `features/team/types` → `features/inventory/types` のみ許容（cross-feature 型参照）  
**面談メモ UI**: `features/interview/` は API・型のみ。UI は `features/team/pages/MemberDetailPage.tsx` に直接実装

---

## 6. バックエンドアーキテクチャ

バックエンドは **package by feature** 構成を採用し、feature 内部を 4 レイヤーに分離する。

```
apps/backend/src/main/java/com/skilize/
├── shared/
│   ├── domain/exception/           ← 共通例外（AuthException, GoalIncompleteException）
│   ├── infrastructure/             ← SecurityConfig・JwtUtil・JWT/初期PWフィルター・LoggingFilter（MDC）
│   └── presentation/               ← GlobalExceptionHandler・ErrorResponse
├── auth/
│   ├── presentation/               ← AuthController（request/ · response/ サブパッケージ）
│   └── application/                ← AuthService（command/ · query/ · mapper/ サブパッケージ）
├── user/
│   ├── presentation/               ← UserController（request/ · response/ サブパッケージ）
│   ├── domain/                     ← User, Role, UserRepository
│   └── infrastructure/             ← UserDetailsServiceImpl
├── inventory/
│   ├── presentation/               ← InventoryController（request/ · response/ サブパッケージ）
│   ├── application/                ← InventoryService（command/ · query/ · mapper/ サブパッケージ）
│   └── domain/                     ← エンティティ・Repository・列挙型
├── master/
│   ├── presentation/               ← MasterController（request/ · response/ サブパッケージ）
│   └── domain/                     ← マスタエンティティ・Repository
├── fiscalyear/
│   ├── presentation/               ← FiscalYearController（request/ · response/ サブパッケージ）
│   └── domain/                     ← FiscalYear, FiscalYearSettings, Repository
├── dashboard/
│   └── presentation/               ← DashboardController（response/ サブパッケージ）
├── charts/
│   ├── presentation/               ← ChartController（radar/growth/heatmap/timeline）
│   └── application/                ← ChartService（query/ サブパッケージ）
├── expectation/
│   ├── presentation/               ← ExpectationController（request/ サブパッケージ）
│   ├── application/                ← ExpectationService（query/ サブパッケージ）
│   └── domain/                     ← UserExpectation・UserExpectationRepository
├── interview/
│   ├── presentation/               ← InterviewController（request/ · response/ サブパッケージ）
│   ├── application/                ← InterviewService（command/ サブパッケージ）
│   └── domain/                     ← InventoryInterview・InterviewDetailNote・DetailType・Repository
└── ai/
    ├── presentation/               ← AiAnalysisController
    ├── application/                ← AiAnalysisService（@Async）・InventoryCompletedEventListener（query/ サブパッケージ）
    └── domain/                     ← AiCareerAnalysis・AiAnalysisStatus・AiCareerAnalysisRepository
```

各 `presentation/` は Controller のみを直接置き、HTTP 入力は `request/`、HTTP 出力は `response/` サブパッケージへ分離する。  
各 `application/` は Service のみを直接置き、Write 入力は `command/`、クエリ結果は `query/`、Request→Command 変換は `mapper/` へ分離する。  
詳細は [`.claude/context/backend-architecture.md`](../../.claude/context/backend-architecture.md) を参照。

**依存方向**（厳守）:
```
presentation → application → domain
infrastructure → domain / application
```

- `@Transactional` は `application` レイヤーのみ配置（Controller での業務トランザクション禁止）
- コンストラクタ注入のみ（`@Autowired` フィールドインジェクション禁止）
- Entity を API へ直接返さない（`presentation/response/` に変換して返す）
- Service は `presentation/request/` をインポートしない（Mapper で Command に変換してから渡す）
- feature 間の直接依存禁止（`shared` を介して連携）

---

## 7. 関連ドキュメント

| ドキュメント | パス |
|------------|------|
| 機能要件 | [docs/requirements/functional/functional-requirements.md](../requirements/functional/functional-requirements.md) |
| 非機能要件 | [docs/requirements/non-functional/non-functional-requirements.md](../requirements/non-functional/non-functional-requirements.md) |
| データモデル（概念） | [docs/architecture/database/data-model.md](./database/data-model.md) |
| ER図 | [docs/architecture/database/er-diagram.md](./database/er-diagram.md) |
| AIモジュールアーキテクチャ | [docs/architecture/ai-module.md](./ai-module.md) |
| 技術スタック詳細 | [.claude/context/tech-stack.md](../../.claude/context/tech-stack.md) |
| バックエンドアーキテクチャルール | [.claude/context/backend-architecture.md](../../.claude/context/backend-architecture.md) |
| フロントエンドアーキテクチャルール | [.claude/context/frontend-architecture.md](../../.claude/context/frontend-architecture.md) |

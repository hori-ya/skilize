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

1. `POST /api/auth/login` → JWT 発行 → クライアントの localStorage に保存
2. 以降のリクエストに `Authorization: Bearer <token>` を付与
3. `JwtAuthenticationFilter` → `InitialPasswordFilter` → Controller の順でフィルタリング

> 詳細（JWT 設計・フィルター実装・ロール制御・CORS 設定）は [security-design.md](./security/security-design.md) を参照。

---

## 5. フロントエンドアーキテクチャ

フロントエンドは **feature by feature** 構成を採用する。フロントエンドの各 feature はバックエンドの `com.skilize.{feature}` パッケージと 1:1 で対応する。

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
│   ├── utils/
│   │   └── apiError.ts               ← APIエラーコード取得・翻訳ユーティリティ
│   └── ui/
│       ├── PrivateRoute.tsx          ← 認証ガード
│       ├── AdminRoute.tsx            ← ADMIN ロールガード
│       ├── TlAdminRoute.tsx          ← TL/ADMIN ロールガード
│       └── ScrollToTopButton.tsx
└── features/
    ├── auth/        ← ログイン・パスワード変更
    ├── inventory/   ← 棚卸入力・提出・前年比較・目標振り返り・棚卸履歴
    ├── dashboard/   ← ダッシュボード表示
    ├── charts/      ← グラフ（スキルバランス・成長推移・ヒートマップ・タイムライン）
    ├── report/      ← 棚卸表 PDF ダウンロード
    ├── ai/          ← AI チャット・AI キャリア分析
    ├── user/        ← ユーザー管理・チーム照会・メンバー詳細
    ├── expectation/ ← TL/会社からの期待コメント
    ├── interview/   ← 面談メモ（API・型のみ。UI は user/pages/ に統合）
    ├── master/      ← マスタ管理ページ群
    └── fiscalyear/  ← 年度管理
```

**依存方向**: `App.tsx → features → shared`（`shared` は `features` をインポートしない）  
**feature 間参照**: 型の参照は許容（例: `features/user/types` → `features/inventory/types`）

> Python AI サービス（`apps/ai/`）のフォルダ構成詳細は [ai-module.md](./ai-module.md) を参照。  
> フロントエンドの詳細ルールは [`.claude/context/frontend-architecture.md`](../../.claude/context/frontend-architecture.md) を参照。

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

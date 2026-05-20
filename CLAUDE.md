# CLAUDE.md

---

## Project Overview (プロジェクト概要)

**Skilize** — 社内向けスキル棚卸管理Webアプリ。

- ユーザーが毎年度のITスキル・資格・セミナー受講履歴を登録し、上長（TL）がレビューする
- ロール: `GENERAL`（一般ユーザー）/ `TL`（チームリーダー）/ `ADMIN`（管理者）
- 初回ログイン時はパスワード変更が必須（`is_initial_password` フラグで制御）

---

## Tech Stack (技術スタック)

| レイヤー | 技術 |
|---|---|
| Frontend | React 18 + TypeScript (Vite) |
| Backend | Spring Boot 4.0.6 / Java 21 |
| Auth | Spring Security + JWT (jjwt 0.12.6) |
| DB | PostgreSQL 16（ローカルは Docker、本番は RDS） |
| ORM | Spring Data JPA / Hibernate |
| Migration | Flyway（本番用）/ init.sql（ローカル用） |
| Build | Gradle |
| Hosting | EC2 |
| Infra | Docker Compose + nginx |

---

## Architecture (アーキテクチャ構成)

```
Browser
  └─ localhost:8081 (nginx)
       ├─ /api/** → backend:8080  (Spring Boot)
       └─ /       → frontend:5173 (Vite dev server)

backend:8080
  └─ PostgreSQL:5432 (db コンテナ / RDS)
```

- **認証フロー**: POST /api/auth/login → JWT 発行 → localStorage 保存 → `Authorization: Bearer <token>` で送信
- **フィルターチェーン**: `JwtAuthenticationFilter` → `InitialPasswordFilter` → コントローラー
- **セッション**: Stateless（Spring Session 不使用）

---

## Project Structure Rules (フォルダ構成ルール)

```
skilize/                          ← プロジェクトルート
├── .claude/context/              ← Claude 向けコンテキスト資料
│   ├── backend-architecture.md  ← バックエンドアーキテクチャ詳細ルール（必読）
│   └── frontend-architecture.md ← フロントエンドアーキテクチャ詳細ルール（必読）
├── .cursor/rules/                ← Cursor IDE 向けルール
├── docs/                         ← 設計・要件ドキュメント
├── scripts/db/init.sql           ← ローカル Docker DB 初期化スクリプト
├── apps/
│   ├── backend/                  ← Spring Boot アプリ
│   └── frontend/                 ← React / Vite アプリ
├── infra/
│   ├── docker/
│   │   ├── backend/Dockerfile
│   │   ├── frontend/Dockerfile
│   │   └── nginx/nginx.conf
│   └── compose/
│       ├── docker-compose.yml    ← ローカル開発用
│       └── docker-compose.prod.yml
├── .env                          ← 環境変数（COMPOSE_FILE 含む）
├── .env.example
└── CLAUDE.md
```

---

## Backend Architecture (バックエンドアーキテクチャ)

**パッケージ構成**: package by feature（機能単位）＋ feature 内レイヤー分離

```
com.skilize
├── shared/
│   ├── domain/exception/       ← 共通例外（AuthException, GoalIncompleteException）
│   ├── infrastructure/         ← Security設定・JWT・フィルター
│   └── presentation/           ← GlobalExceptionHandler・ErrorResponse DTO
├── auth/
│   ├── presentation/           ← AuthController + Request/Response DTO
│   └── application/            ← AuthService（@Transactional）
├── user/
│   ├── presentation/           ← UserController + Request/Response DTO
│   ├── domain/                 ← User, Role, UserRepository
│   └── infrastructure/         ← UserDetailsServiceImpl
├── inventory/
│   ├── presentation/           ← InventoryController + Request/Response DTO
│   ├── application/            ← InventoryService（@Transactional）
│   └── domain/                 ← エンティティ・Repository・列挙型
├── master/
│   ├── presentation/           ← MasterController + Request/Response DTO
│   └── domain/                 ← マスタエンティティ・Repository
├── fiscalyear/
│   ├── presentation/           ← FiscalYearController + Request/Response DTO
│   └── domain/                 ← FiscalYear, FiscalYearSettings, Repository
├── dashboard/
│   └── presentation/           ← DashboardController + Response DTO
├── charts/
│   ├── presentation/           ← ChartController + Response DTO
│   └── application/            ← ChartService（radar/growth/heatmap/timeline 集計）
├── ai/
│   ├── presentation/           ← AiAnalysisController + Response DTO
│   ├── application/            ← AiAnalysisService（非同期AI分析トリガー・結果取得）
│   └── domain/                 ← AiCareerAnalysis エンティティ・Repository・InventoryCompletedEventListener
└── interview/
    ├── presentation/           ← InterviewController + Request/Response DTO
    └── application/            ← InterviewService（面談メモ保存）
```

**レイヤー責務**:
- `presentation` — Controller・Request/Response DTO・バリデーション・HTTPハンドリング（ビジネスロジック禁止）
- `application` — UseCase/ApplicationService・トランザクション境界・ワークフロー調整（SQL直接実装禁止）
- `domain` — ビジネスルール・ドメインモデル・Repositoryインターフェース（Spring Framework依存禁止・SQL禁止）
- `infrastructure` — JPA実装・Repositoryインターフェース実装・外部API・Security

**依存方向**（厳守）:
```
presentation → application → domain
infrastructure → domain / application
```
禁止: `domain → infrastructure`、`domain → presentation`、feature 間の直接依存

**トランザクション**: `@Transactional` は `application` レイヤーのみ（Controller・Repository での業務トランザクション禁止）

**DI**: コンストラクタ注入のみ（`@Autowired` フィールドインジェクション禁止）

**共通化ルール**: `shared` は最小限。3回以上重複した場合のみ shared 化を検討。業務ロジックを shared に置かない。

> バックエンド実装時は `.claude/context/backend-architecture.md` の詳細ルールも参照すること。

---

## Coding Rules (コーディングルール)

**Backend**
- DTO は Java `record` を使用
- Lombok `@RequiredArgsConstructor` でコンストラクタ注入
- エンティティに `@Setter` を付けず、ドメインメソッド（`changePassword` 等）でフィールド更新
- 例外は `AuthException`（認証系）と Spring の標準例外を使い分ける
- バリデーションは `jakarta.validation` アノテーション + `@Valid`
- Entity を API へ直接返さない（必ず Request/Response DTO を分離）
- **DTO は Controller・Service クラス内に定義しない。必ず `feature/presentation/` に独立ファイルとして作成する**
- DTO 命名: `XxxRequest`（リクエスト）/ `XxxResponse`（レスポンス）/ `XxxDto`（共有・ネスト用）
- 特定 DTO 内でしか使わないネスト DTO は、親 DTO ファイル内に record としてまとめて定義してよい

**Frontend**
- React 関数コンポーネント + hooks のみ
- `async/await` + Axios で API 呼び出し
- グローバル状態は `AuthContext`（`app/providers/AuthProvider.tsx`）のみ（外部状態管理ライブラリなし）
- スタイルは `index.css` の BEM ライクなクラス名
- **フォルダ構成**: feature by feature（`app/` / `shared/` / `features/` の3層）
  - `app/` — アプリ全体の初期化（AuthProvider・NavBar）
  - `shared/` — 複数 feature で使う共通資産（Axiosクライアント・マスタAPI・マスタ型・ルートガード）
  - `features/{name}/` — 機能ごとの閉じた実装（api / types / pages）
- **依存方向**: `App.tsx → features → shared`（`shared` は `features` をインポートしない）
- feature 間の型参照は許容する（例: `features/team/types` が `features/inventory/types` を参照）
- 新機能は必ず対応する feature フォルダ内に追加する

> フロントエンド実装時は `.claude/context/frontend-architecture.md` の詳細ルールも参照すること。

---

## Naming Conventions (命名規則)

| 対象 | 規則 | 例 |
|---|---|---|
| Java クラス | PascalCase | `AuthService`, `LoginRequest` |
| Java メソッド・フィールド | camelCase | `findByUserId`, `passwordHash` |
| DB テーブル・カラム | snake_case | `users`, `password_hash`, `is_active` |
| REST パス | kebab-case（小文字） | `/api/auth/change-password` |
| React コンポーネント | PascalCase | `LoginPage`, `NavBar` |
| CSS クラス | BEM ライク | `.navbar__link`, `.btn-primary` |

---

## API Rules (API設計ルール)

- ベースパス: `/api`
- 認証不要: `POST /api/auth/login`、`GET /api/health`
- 全エンドポイントに `Authorization: Bearer <JWT>` 必須（上記以外）
- レスポンス形式: JSON
- エラーレスポンス: `{ "code": "ERROR_CODE", "message": "..." }`
- バリデーションエラー: `{ "errors": [{ "field": "...", "message": "..." }] }`

主要エンドポイント:

```
# 認証
POST   /api/auth/login
GET    /api/auth/me
POST   /api/auth/change-password
POST   /api/auth/logout

# ダッシュボード・グラフ
GET    /api/dashboard
GET    /api/charts/radar
GET    /api/charts/growth
GET    /api/charts/heatmap
GET    /api/charts/timeline

# 棚卸
GET    /api/inventories/mine
POST   /api/inventories
GET    /api/inventories/{id}
PUT    /api/inventories/{id}/it-skill-details
PUT    /api/inventories/{id}/qualification-details
PUT    /api/inventories/{id}/seminar-details
PATCH  /api/inventories/{id}/it-skill-details/{detailId}
POST   /api/inventories/{id}/submit
GET    /api/inventories/{id}/comparison
GET    /api/inventories/{id}/goal-review
PUT    /api/inventories/{id}/goal-review
POST   /api/inventories/{id}/goal-review/complete
GET    /api/inventories/{id}/goals
PUT    /api/inventories/{id}/goals
POST   /api/inventories/{id}/goals/complete

# AI分析
GET    /api/users/me/ai-analyses
GET    /api/users/{userId}/ai-analyses        (TL/ADMIN)

# ユーザー管理（ADMIN）
GET    /api/users
POST   /api/users
PUT    /api/users/{id}
PATCH  /api/users/{id}/deactivate
PATCH  /api/users/{id}/activate
POST   /api/users/{id}/reset-password

# チーム照会（TL/ADMIN）
GET    /api/users/me/team-members
GET    /api/users/{id}/inventories

# 面談（TL/ADMIN）
GET    /api/interviews/inventory/{inventoryId}
PUT    /api/interviews/inventory/{inventoryId}
GET    /api/interviews/inventory/{inventoryId}/prev-year

# マスタ（TL/ADMIN）
GET    /api/it-skills
POST   /api/it-skills
PUT    /api/it-skills/{id}
DELETE /api/it-skills/{id}
PATCH  /api/it-skills/{id}/restore
GET    /api/it-skills/custom-unregistered
POST   /api/it-skills/promote
GET    /api/qualifications
POST   /api/qualifications
PUT    /api/qualifications/{id}
DELETE /api/qualifications/{id}
PATCH  /api/qualifications/{id}/restore
GET    /api/qualifications/custom-unregistered
POST   /api/qualifications/promote
GET    /api/ad-seminars
POST   /api/ad-seminars
PUT    /api/ad-seminars/{id}
DELETE /api/ad-seminars/{id}
PATCH  /api/ad-seminars/{id}/restore
GET    /api/it-skill-categories
POST   /api/it-skill-categories
PUT    /api/it-skill-categories/{id}
DELETE /api/it-skill-categories/{id}
GET    /api/qualification-categories
POST   /api/qualification-categories
PUT    /api/qualification-categories/{id}
DELETE /api/qualification-categories/{id}
GET    /api/ad-seminar-categories
POST   /api/ad-seminar-categories
PUT    /api/ad-seminar-categories/{id}
DELETE /api/ad-seminar-categories/{id}
GET    /api/seminar-categories
POST   /api/seminar-categories
PUT    /api/seminar-categories/{id}
DELETE /api/seminar-categories/{id}
GET    /api/skill-levels
POST   /api/skill-levels
PUT    /api/skill-levels/{id}
DELETE /api/skill-levels/{id}

# 年度（ADMIN）
GET    /api/fiscal-years
GET    /api/fiscal-years/current
POST   /api/fiscal-years
PUT    /api/fiscal-years/{id}
GET    /api/fiscal-year-settings
PUT    /api/fiscal-year-settings

# ヘルスチェック
GET    /api/health
```

---

## Auth & Security (認証・セキュリティルール)

- パスワードハッシュ: BCrypt コストファクター 12
- JWT 有効期限: `JWT_EXPIRATION_MS`（デフォルト 28800000ms = 8時間）
- `is_initial_password = true` の場合、`/api/auth/change-password` 以外は 403 を返す（`InitialPasswordFilter`）
- CORS 許可オリジン: `FRONTEND_ORIGIN` 環境変数（デフォルト `http://localhost:5173,http://localhost:8081`）
- ロール制御: `@PreAuthorize` or `@EnableMethodSecurity` で実装

---

## Development Workflow (開発フロー)

**Docker 一括起動（推奨）**

プロジェクトルートで実行（`.env` の `COMPOSE_FILE` により自動解決）:

```bash
docker compose up --build
```

- フロントエンド: http://localhost:8081
- バックエンド API: http://localhost:8080/api

**IntelliJ デバッグ実行**

1. DB コンテナのみ起動: `docker compose up db`
2. IntelliJ 実行構成に環境変数を設定:
   ```
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/skilize
   SPRING_DATASOURCE_USERNAME=skilize
   SPRING_DATASOURCE_PASSWORD=password
   JWT_SECRET=change-this-to-a-random-256-bit-secret-key-before-use
   SPRING_PROFILES_ACTIVE=local
   ```
3. フロントエンド起動: `cd apps/frontend && npm run dev`
4. http://localhost:5173 にアクセス（Vite → `localhost:8080` へプロキシ済み）

**テストユーザー（init.sql 適用後）**

| user_id | password | role |
|---|---|---|
| admin | admin | ADMIN |
| tl01 | tl01 | TL |
| user01 | user01 | GENERAL |
| user02 | user02 | GENERAL |

**DB の再初期化（ローカル）**

```bash
docker compose down -v   # ボリューム削除
docker compose up db     # init.sql が再実行される
```

---

## Forbidden Rules (禁止ルール)

- `User` エンティティに `@Setter` を付けない（フィールドはドメインメソッドで変更）
- パスワードを平文で DB に保存しない（必ず BCrypt ハッシュ化）
- JWT をセッションや Cookie に保存しない（localStorage のみ）
- `application.yml` に機密情報をハードコードしない（必ず環境変数参照）
- Flyway マイグレーション済みファイルを後から編集しない（新バージョンで対応）
- フロントエンドで `any` 型を使わない

---

## AI Assistant Rules (AI向けルール)

- エンティティの新規フィールド追加時は Flyway マイグレーション（`apps/backend/src/main/resources/db/migration/V{n}__xxx.sql`）と `scripts/db/init.sql` の両方を更新する
- `application-local.yml` は `flyway.enabled: false` のため、ローカル向けスキーマ変更は `scripts/db/init.sql` に反映する
- `SecurityConfig` の `permitAll()` に追加する際は CORS 設定も確認する
- 新しいロール制御が必要な場合は `@PreAuthorize("hasRole('ADMIN')")` 等を使用する

---

## Environment Variables (環境変数ルール)

| 変数名 | 説明 | デフォルト |
|---|---|---|
| `COMPOSE_FILE` | Docker Compose ファイルパス | `infra/compose/docker-compose.yml` |
| `SPRING_DATASOURCE_URL` | DB 接続 URL | なし（必須） |
| `SPRING_DATASOURCE_USERNAME` | DB ユーザー名 | なし（必須） |
| `SPRING_DATASOURCE_PASSWORD` | DB パスワード | なし（必須） |
| `JWT_SECRET` | JWT 署名秘密鍵（32文字以上推奨） | なし（必須） |
| `JWT_EXPIRATION_MS` | JWT 有効期限（ms） | `28800000`（8時間） |
| `SPRING_PROFILES_ACTIVE` | Spring プロファイル | `local`（開発時） |
| `FRONTEND_ORIGIN` | CORS 許可オリジン（カンマ区切り） | `http://localhost:5173,http://localhost:8081` |
| `DB_NAME` | Docker DB 名（docker-compose 用） | — |
| `DB_USER` | Docker DB ユーザー（docker-compose 用） | — |
| `DB_PASSWORD` | Docker DB パスワード（docker-compose 用） | — |
| `AI_ENABLED` | AI機能の有効化（`false` で LLM 呼び出し停止） | `true` |
| `LLM_PROVIDER` | AI モジュールの LLM プロバイダー | `openai` |
| `LLM_MODEL` | 使用 LLM モデル名 | `gpt-4o` |
| `OPENAI_API_KEY` | OpenAI API キー（`LLM_PROVIDER=openai` 時は必須） | — |
| `ANTHROPIC_API_KEY` | Anthropic API キー（`LLM_PROVIDER=anthropic` 時は必須） | — |
| `AI_SECRET_KEY` | Spring Boot → Python AI サービス間の内部認証キー | なし（必須） |
| `AI_SERVICE_URL` | Python FastAPI の内部 URL | `http://ai:8000` |

`.env` ファイルに記載し、`.gitignore` で除外する（`.env.example` に雛形を置く）。

---

## Directory Responsibilities (ディレクトリの責務説明)

| ディレクトリ | 責務 |
|---|---|
| `apps/backend/src/main/java/com/skilize/shared/domain/exception/` | 共通例外（AuthException, GoalIncompleteException） |
| `apps/backend/src/main/java/com/skilize/shared/infrastructure/` | SecurityConfig・JwtUtil・JwtAuthenticationFilter・InitialPasswordFilter |
| `apps/backend/src/main/java/com/skilize/shared/presentation/` | GlobalExceptionHandler・ErrorResponse・ValidationErrorResponse |
| `apps/backend/src/main/java/com/skilize/auth/presentation/` | AuthController |
| `apps/backend/src/main/java/com/skilize/auth/dto/` | LoginRequest・LoginResponse・ChangePasswordRequest・MeResponse（auth は dto/ サブフォルダ） |
| `apps/backend/src/main/java/com/skilize/auth/application/` | AuthService（ログイン・JWT 発行・パスワード変更ロジック） |
| `apps/backend/src/main/java/com/skilize/user/presentation/` | UserController・UserDto・CreateUserRequest・UpdateUserRequest 等 Request/Response DTO |
| `apps/backend/src/main/java/com/skilize/user/domain/` | User エンティティ・Role・UserRepository |
| `apps/backend/src/main/java/com/skilize/user/infrastructure/` | UserDetailsServiceImpl（Spring Security 実装） |
| `apps/backend/src/main/java/com/skilize/inventory/presentation/` | InventoryController・Request/Response DTO |
| `apps/backend/src/main/java/com/skilize/inventory/application/` | InventoryService（棚卸ビジネスロジック） |
| `apps/backend/src/main/java/com/skilize/inventory/domain/` | Inventory・ItSkillDetail・QualificationDetail・SeminarDetail・InventoryGoal・Repository・列挙型 |
| `apps/backend/src/main/java/com/skilize/master/presentation/` | MasterController・Request/Response DTO（ITスキル・資格・AD・分類・レベル） |
| `apps/backend/src/main/java/com/skilize/master/domain/` | マスタエンティティ（SkillLevel, ItSkill, Qualification, AdSeminar 等）・Repository |
| `apps/backend/src/main/java/com/skilize/fiscalyear/presentation/` | FiscalYearController・Request/Response DTO |
| `apps/backend/src/main/java/com/skilize/fiscalyear/domain/` | FiscalYear・FiscalYearSettings・Repository |
| `apps/backend/src/main/java/com/skilize/dashboard/presentation/` | DashboardController・Response DTO |
| `apps/backend/src/main/java/com/skilize/charts/presentation/` | ChartController・Response DTO（radar/growth/heatmap/timeline） |
| `apps/backend/src/main/java/com/skilize/charts/application/` | ChartService（スキルバランス・成長推移・ヒートマップ・タイムライン集計） |
| `apps/backend/src/main/java/com/skilize/ai/` | AiAnalysisController・AiAnalysisService・AiCareerAnalysis エンティティ・InventoryCompletedEventListener（棚卸提出時の非同期AI分析トリガー） |
| `apps/backend/src/main/java/com/skilize/interview/` | InterviewController・面談メモ保存ロジック（TL/ADMIN が棚卸明細ごとのメモと全体備忘録を記録） |
| `apps/backend/src/main/resources/db/migration/` | Flyway マイグレーション（本番・CI 用） |
| `scripts/db/init.sql` | ローカル Docker DB 用の完全初期化スクリプト（DROP→CREATE→INSERT） |
| `apps/frontend/src/app/providers/` | AuthProvider（認証状態の全体共有）と useAuth hook |
| `apps/frontend/src/app/layouts/` | NavBar（グローバルナビゲーション） |
| `apps/frontend/src/shared/api/` | Axios クライアント・マスタデータ API（複数 feature で共有） |
| `apps/frontend/src/shared/types/` | マスタデータ型定義（複数 feature で共有） |
| `apps/frontend/src/shared/ui/` | ルートガード（PrivateRoute・TlAdminRoute・AdminRoute）・ScrollToTopButton |
| `apps/frontend/src/features/auth/` | ログイン・初回パスワード変更・マイページパスワード変更（API / 型 / ページ） |
| `apps/frontend/src/features/inventory/` | ダッシュボード・棚卸入力・前年度比較・目標振り返り・目標設定・棚卸履歴・グラフ（API / 型 / コンポーネント / ページ） |
| `apps/frontend/src/features/team/` | チーム照会・メンバー詳細・全ユーザー照会（API / 型 / ページ） |
| `apps/frontend/src/features/master/` | 各種マスタ管理ページ（年度・スキルレベル・ITスキル・資格・AD・ユーザー管理） |
| `apps/frontend/src/features/interview/` | 面談機能の API 呼び出し・型定義（ページは features/team に統合） |
| `infra/docker/` | 各サービスの Dockerfile・nginx 設定 |
| `infra/compose/` | Docker Compose ファイル（ローカル用・本番用） |

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
| Frontend | React 18 + TypeScript (Vite) + i18next / react-i18next |
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
│   ├── presentation/           ← AuthController
│   │   └── request/            ← LoginRequest・ChangePasswordRequest
│   └── application/            ← AuthService（@Transactional）
│       ├── command/            ← LoginCommand・ChangePasswordCommand
│       ├── query/              ← LoginQueryResult・MeQueryResult
│       └── mapper/             ← AuthApplicationMapper
├── user/
│   ├── presentation/           ← UserController
│   │   ├── request/            ← CreateUserRequest・UpdateUserRequest
│   │   └── response/           ← UserResponse・TeamMemberResponse・MemberInventorySummaryResponse 等
│   ├── domain/                 ← User, Role, UserRepository
│   └── infrastructure/         ← UserDetailsServiceImpl
├── inventory/
│   ├── presentation/           ← InventoryController
│   │   ├── request/            ← CreateInventoryRequest・ItSkillDetailsRequest 等
│   │   └── response/           ← InventorySummaryResponse・ItSkillDetailsResponse 等
│   ├── application/            ← InventoryService（@Transactional）
│   │   ├── command/            ← ItSkillDetailCommand・GoalCommand 等
│   │   ├── query/              ← ComparisonQueryResult・GoalReviewQueryResult
│   │   └── mapper/             ← InventoryApplicationMapper
│   └── domain/                 ← エンティティ・Repository・列挙型
├── master/
│   ├── presentation/           ← MasterController
│   │   ├── request/            ← SkillLevelRequest・ItSkillRequest 等
│   │   └── response/           ← SkillLevelResponse・ItSkillResponse 等
│   └── domain/                 ← マスタエンティティ・Repository
├── fiscalyear/
│   ├── presentation/           ← FiscalYearController
│   │   ├── request/            ← FiscalYearRequest・FiscalYearSettingsRequest
│   │   └── response/           ← FiscalYearResponse・FiscalYearSettingsResponse
│   └── domain/                 ← FiscalYear, FiscalYearSettings, Repository
├── dashboard/
│   └── presentation/           ← DashboardController
│       └── response/           ← DashboardResponse（nested UserInfo・FiscalYearRef・CurrentInventoryInfo）
├── charts/
│   ├── presentation/           ← ChartController（QueryResult を直接返す）
│   └── application/            ← ChartService（radar/growth/heatmap/timeline 集計）
│       └── query/              ← RadarQueryResult・GrowthQueryResult・HeatmapQueryResult・TimelineQueryResult
├── expectation/
│   ├── presentation/           ← ExpectationController
│   │   └── request/            ← SaveExpectationRequest
│   ├── application/            ← ExpectationService（@Transactional）
│   │   └── query/              ← ExpectationQueryResult
│   └── domain/                 ← UserExpectation・UserExpectationRepository
├── interview/
│   ├── presentation/           ← InterviewController
│   │   ├── request/            ← SaveInterviewRequest・DetailNoteRequest
│   │   └── response/           ← InterviewResponse・DetailNoteResponse
│   └── application/            ← InterviewService（面談メモ保存）
│       └── command/            ← DetailNoteCommand
└── ai/
    ├── presentation/           ← AiAnalysisController（QueryResult を直接返す）
    ├── application/            ← AiAnalysisService（非同期AI分析）・InventoryCompletedEventListener
    │   └── query/              ← AiAnalysisQueryResult
    └── domain/                 ← AiCareerAnalysis エンティティ・Repository
```

**レイヤー責務**:
- `presentation` — Controller のみ。`request/` に HTTP 入力、`response/` に HTTP 出力を配置。バリデーションはここで完結（ビジネスロジック禁止）
- `application` — UseCase/ApplicationService・トランザクション境界・ワークフロー調整。`command/` に Service 入力、`query/` に Service 出力、`mapper/` に Request→Command 変換を配置（SQL直接実装禁止）
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
- **DTO は責務別パッケージに分離する（`feature/dto/` は廃止）**
  - `presentation/request/` — HTTP 入力（`@RequestBody`）。バリデーションアノテーションはここに付ける
  - `presentation/response/` — Controller が組み立てて返す HTTP 出力
  - `application/command/` — Service への Write オペレーション入力（`XxxRequest` を直接 Service に渡さない）
  - `application/query/` — Service が組み立てて返すクエリ結果
  - `application/mapper/` — `XxxRequest` → `XxxCommand` 変換（`@Component`、Controller でコンストラクタ注入）
- DTO 命名: `XxxRequest` / `XxxResponse` / `XxxCommand` / `XxxQueryResult`（`XxxDto` という命名は使わない）
- 特定クラス内でしか使わないネスト型は、親ファイル内に `record` としてまとめて定義してよい
- **依存方向厳守**: Service は `presentation/request/` パッケージをインポートしない。Request → Command 変換は必ず Mapper 経由で行う

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
- **i18n（国際化）**: i18next + react-i18next を使用。コンポーネント内に日本語文字列をハードコードしない
  - 翻訳ファイル: `src/i18n/locales/ja/{namespace}.json`（namespace は feature 単位）
  - 初期設定: `src/i18n/index.ts`（`main.tsx` でインポート）
  - 使用方法: `const { t } = useTranslation('namespace')` → `t('key.subKey')`
  - 共通文字列（ボタン・ラベル等）は `common` namespace に集約

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
- フロントエンドコンポーネント内に日本語文字列をハードコードしない（`className` 値・コメントを除く。翻訳は `src/i18n/locales/ja/` の JSON ファイルで管理）
- **`feature/dto/` パッケージを新規作成しない**（廃止済み。責務別パッケージ `request/` / `response/` / `command/` / `query/` を使うこと）
- **`XxxDto` という命名のクラスを新規作成しない**（`XxxRequest` / `XxxResponse` / `XxxCommand` / `XxxQueryResult` のいずれかで命名する）
- **Service メソッドの引数に `presentation/request/` のクラスを直接渡さない**（必ず Mapper で Command に変換してから渡す）
- **Service クラスが `presentation` パッケージをインポートしない**（application → presentation の依存禁止）

---

## AI Assistant Rules (AI向けルール)

- エンティティの新規フィールド追加時は Flyway マイグレーション（`apps/backend/src/main/resources/db/migration/V{n}__xxx.sql`）と `scripts/db/init.sql` の両方を更新する
- `application-local.yml` は `flyway.enabled: false` のため、ローカル向けスキーマ変更は `scripts/db/init.sql` に反映する
- テストデータ（テストユーザー・サンプル棚卸など）は `db/testdata/` に配置する。`db/migration/` は本番 Flyway の対象のため、テストデータを `db/migration/` に置かないこと
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
| `CA_CERT_ENABLED` | 社内ルートCA証明書をコンテナに組み込む（`true` で有効化） | `false` |

`.env` ファイルに記載し、`.gitignore` で除外する（`.env.example` に雛形を置く）。

---

## Directory Responsibilities (ディレクトリの責務説明)

### バックエンド（Spring Boot）

| ディレクトリ | 責務 |
|---|---|
| `com/skilize/shared/domain/exception/` | 共通例外（AuthException, GoalIncompleteException） |
| `com/skilize/shared/infrastructure/` | SecurityConfig・JwtUtil・JwtAuthenticationFilter・InitialPasswordFilter |
| `com/skilize/shared/presentation/` | GlobalExceptionHandler・ErrorResponse・ValidationErrorResponse |
| `com/skilize/auth/presentation/` | AuthController |
| `com/skilize/auth/presentation/request/` | LoginRequest・ChangePasswordRequest |
| `com/skilize/auth/application/` | AuthService（ログイン・JWT 発行・パスワード変更ロジック） |
| `com/skilize/auth/application/command/` | LoginCommand・ChangePasswordCommand |
| `com/skilize/auth/application/query/` | LoginQueryResult・MeQueryResult |
| `com/skilize/auth/application/mapper/` | AuthApplicationMapper（Request→Command 変換） |
| `com/skilize/user/presentation/` | UserController |
| `com/skilize/user/presentation/request/` | CreateUserRequest・UpdateUserRequest |
| `com/skilize/user/presentation/response/` | UserResponse・TeamMemberResponse・MemberInventorySummaryResponse・FiscalYearRef・ResetPasswordResponse |
| `com/skilize/user/domain/` | User エンティティ・Role・UserRepository |
| `com/skilize/user/infrastructure/` | UserDetailsServiceImpl（Spring Security 実装） |
| `com/skilize/inventory/presentation/` | InventoryController |
| `com/skilize/inventory/presentation/request/` | CreateInventoryRequest・ItSkillDetailsRequest・ItSkillDetailItem・QualificationDetailsRequest・SeminarDetailsRequest・RemarksPatchRequest・GoalsRequest・GoalItem・GoalReviewUpdateRequest・GoalReviewUpdateItem |
| `com/skilize/inventory/presentation/response/` | InventorySummaryResponse・InventoryDetailResponse・ItSkillDetailsResponse・ItSkillDetailResponse・QualificationDetailsResponse・QualificationDetailResponse・SeminarDetailsResponse・SeminarDetailResponse・RemarksPatchResponse・SubmitResponse・GoalsResponse・GoalResponse・GoalCompleteResponse・GoalReviewCompleteResponse・FiscalYearRef |
| `com/skilize/inventory/application/` | InventoryService（棚卸ビジネスロジック） |
| `com/skilize/inventory/application/command/` | ItSkillDetailCommand・QualificationDetailCommand・SeminarDetailCommand・GoalCommand・GoalReviewUpdateCommand |
| `com/skilize/inventory/application/query/` | ComparisonQueryResult・GoalReviewQueryResult |
| `com/skilize/inventory/application/mapper/` | InventoryApplicationMapper（Request→Command 変換） |
| `com/skilize/inventory/domain/` | Inventory・ItSkillDetail・QualificationDetail・SeminarDetail・InventoryGoal・Repository・列挙型 |
| `com/skilize/master/presentation/` | MasterController |
| `com/skilize/master/presentation/request/` | SkillLevelRequest・ItSkillRequest・ItSkillCategoryRequest・ItSkillCategoryUpdateRequest・QualificationRequest・AdSeminarRequest・PromoteItSkillRequest・PromoteQualificationRequest・SimpleCategoryRequest |
| `com/skilize/master/presentation/response/` | SkillLevelResponse・ItSkillResponse・ItSkillCategoryResponse・QualificationResponse・QualificationCategoryResponse・AdSeminarResponse・AdSeminarCategoryResponse・SeminarCategoryResponse・CustomUnregisteredResponse |
| `com/skilize/master/domain/` | マスタエンティティ（SkillLevel, ItSkill, Qualification, AdSeminar 等）・Repository |
| `com/skilize/fiscalyear/presentation/` | FiscalYearController |
| `com/skilize/fiscalyear/presentation/request/` | FiscalYearRequest・FiscalYearSettingsRequest |
| `com/skilize/fiscalyear/presentation/response/` | FiscalYearResponse・FiscalYearSettingsResponse |
| `com/skilize/fiscalyear/domain/` | FiscalYear・FiscalYearSettings・Repository |
| `com/skilize/dashboard/presentation/` | DashboardController |
| `com/skilize/dashboard/presentation/response/` | DashboardResponse（nested UserInfo・FiscalYearRef・CurrentInventoryInfo） |
| `com/skilize/charts/presentation/` | ChartController（QueryResult を直接返す） |
| `com/skilize/charts/application/` | ChartService（スキルバランス・成長推移・ヒートマップ・タイムライン集計） |
| `com/skilize/charts/application/query/` | RadarQueryResult・GrowthQueryResult・HeatmapQueryResult・TimelineQueryResult |
| `com/skilize/expectation/presentation/` | ExpectationController |
| `com/skilize/expectation/presentation/request/` | SaveExpectationRequest |
| `com/skilize/expectation/application/` | ExpectationService（期待コメント保存ロジック） |
| `com/skilize/expectation/application/query/` | ExpectationQueryResult |
| `com/skilize/expectation/domain/` | UserExpectation エンティティ・UserExpectationRepository |
| `com/skilize/interview/presentation/` | InterviewController |
| `com/skilize/interview/presentation/request/` | SaveInterviewRequest・DetailNoteRequest |
| `com/skilize/interview/presentation/response/` | InterviewResponse・DetailNoteResponse |
| `com/skilize/interview/application/` | InterviewService（面談メモ保存ロジック） |
| `com/skilize/interview/application/command/` | DetailNoteCommand |
| `com/skilize/ai/presentation/` | AiAnalysisController（QueryResult を直接返す） |
| `com/skilize/ai/application/` | AiAnalysisService（非同期AI分析トリガー・結果取得）・InventoryCompletedEventListener |
| `com/skilize/ai/application/query/` | AiAnalysisQueryResult |
| `com/skilize/ai/domain/` | AiCareerAnalysis エンティティ・AiAnalysisStatus・AiCareerAnalysisRepository |
| `apps/backend/src/main/resources/db/migration/` | Flyway マイグレーション（本番・CI 用） |
| `scripts/db/init.sql` | ローカル Docker DB 用の完全初期化スクリプト（DROP→CREATE→INSERT） |

### フロントエンド（React / Vite）

| ディレクトリ | 責務 |
|---|---|
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
| `apps/frontend/src/i18n/` | i18next 初期設定（`index.ts`）と翻訳 JSON ファイル（`locales/ja/`） |

### インフラ

| ディレクトリ | 責務 |
|---|---|
| `infra/docker/` | 各サービスの Dockerfile・nginx 設定 |
| `infra/compose/` | Docker Compose ファイル（ローカル用・本番用） |

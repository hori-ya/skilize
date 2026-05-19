# Skilize — Cursor Rules

Cursor で保守作業を行う際に参照するルール集。
`.cursor/rules/` の各 `.mdc` ファイルは Cursor が自動適用する機械向け設定であり、本ファイルはその人間向け補完ドキュメントとして機能する。

---

## 目次

1. [プロジェクト概要](#1-プロジェクト概要)
2. [技術スタック](#2-技術スタック)
3. [ローカル開発環境の起動](#3-ローカル開発環境の起動)
4. [ディレクトリ構成](#4-ディレクトリ構成)
5. [バックエンドアーキテクチャ](#5-バックエンドアーキテクチャ)
6. [フロントエンドアーキテクチャ](#6-フロントエンドアーキテクチャ)
7. [命名規則](#7-命名規則)
8. [API 設計ルール](#8-api-設計ルール)
9. [認証・セキュリティ](#9-認証セキュリティ)
10. [DB スキーマ変更手順](#10-db-スキーマ変更手順)
11. [テストルール](#11-テストルール)
12. [コメントルール](#12-コメントルール)
13. [絶対禁止事項](#13-絶対禁止事項)
14. [.cursor/rules との対応表](#14-cursorrules-との対応表)

---

## 1. プロジェクト概要

**Skilize** — 社内向けスキル棚卸管理 Web アプリ。

- ユーザーが毎年度の IT スキル・資格・セミナー受講履歴を登録し、上長（TL）がレビューする。
- ロール: `GENERAL`（一般ユーザー）/ `TL`（チームリーダー）/ `ADMIN`（管理者）
- 初回ログイン時はパスワード変更が必須（`is_initial_password` フラグで制御）

---

## 2. 技術スタック

| レイヤー | 技術 |
|---|---|
| Frontend | React 18 + TypeScript（Vite） |
| Backend | Spring Boot 4.0.6 / Java 21 |
| Auth | Spring Security + JWT（jjwt 0.12.6） |
| DB | PostgreSQL 16（ローカルは Docker、本番は RDS） |
| ORM | Spring Data JPA / Hibernate |
| Migration | Flyway（本番用）/ init.sql（ローカル用） |
| Build | Gradle |
| Hosting | EC2 |
| Infra | Docker Compose + nginx |

---

## 3. ローカル開発環境の起動

### Docker 一括起動（推奨）

```bash
docker compose up --build
```

- フロントエンド: http://localhost:8081
- バックエンド API: http://localhost:8080/api

### IntelliJ デバッグ実行

1. DB コンテナのみ起動: `docker compose up db`
2. IntelliJ 実行構成に以下の環境変数を設定:

   ```
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/skilize
   SPRING_DATASOURCE_USERNAME=skilize
   SPRING_DATASOURCE_PASSWORD=password
   JWT_SECRET=change-this-to-a-random-256-bit-secret-key-before-use
   SPRING_PROFILES_ACTIVE=local
   ```

3. フロントエンド起動: `cd apps/frontend && npm run dev`
4. http://localhost:5173 にアクセス（Vite → `localhost:8080` へプロキシ済み）

### テストユーザー（init.sql 適用後）

| user_id | password | role |
|---|---|---|
| admin | admin | ADMIN |
| tl01 | tl01 | TL |
| user01 | user01 | GENERAL |
| user02 | user02 | GENERAL |

### DB の再初期化（ローカル）

```bash
docker compose down -v   # ボリューム削除
docker compose up db     # init.sql が再実行される
```

---

## 4. ディレクトリ構成

```
skilize/
├── .claude/context/              ← Claude Code 向けコンテキスト資料
├── .cursor/rules/                ← Cursor IDE 向けルール（*.mdc）
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
├── .env                          ← 環境変数（.gitignore 対象）
├── .env.example                  ← 環境変数テンプレート
├── CLAUDE.md                     ← Claude Code 向けルール
└── CURSOR_RULES.md               ← 本ファイル（Cursor 向けルール）
```

---

## 5. バックエンドアーキテクチャ

> 対応 Cursor ルール: `.cursor/rules/java.mdc`

### パッケージ構成（package by feature + レイヤー分離）

```
com.skilize
├── shared/
│   ├── domain/exception/       ← AuthException, GoalIncompleteException
│   ├── infrastructure/         ← SecurityConfig・JwtUtil・JwtAuthenticationFilter・InitialPasswordFilter
│   └── presentation/           ← GlobalExceptionHandler・ErrorResponse・ValidationErrorResponse
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
│   ├── application/            ← AiAnalysisService
│   └── domain/                 ← AiCareerAnalysis エンティティ・Repository・InventoryCompletedEventListener
└── interview/
    ├── presentation/           ← InterviewController + Request/Response DTO
    └── application/            ← InterviewService
```

### レイヤー責務と禁止事項

| レイヤー | 責務 | 禁止 |
|---|---|---|
| `presentation` | Controller・Request/Response DTO・バリデーション・HTTP ハンドリング | ビジネスロジック・SQL・トランザクション制御 |
| `application` | ユースケース・トランザクション境界・ワークフロー調整 | SQL 直接実装・`@Transactional` を Controller/Repository に置く |
| `domain` | ビジネスルール・エンティティ・Repository インターフェース定義 | Spring Framework 依存・JPA 実装詳細・SQL 実装 |
| `infrastructure` | JPA・Repository 実装・外部 API・Security 設定 | （技術詳細以外の責務） |

### 依存方向（厳守）

```
presentation → application → domain
infrastructure → domain / application
```

- `domain → infrastructure` 禁止
- `domain → presentation` 禁止
- feature 間の直接依存禁止

### コーディングルール（Java）

#### DI（依存性注入）

コンストラクタ注入のみ。`@Autowired` フィールドインジェクション禁止。

```java
// NG
@Autowired
private UserService userService;

// OK（@RequiredArgsConstructor + final フィールド推奨）
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
}
```

#### トランザクション

`@Transactional` は `application` レイヤーのみ。参照系は `@Transactional(readOnly = true)` を使う。

```java
// OK
@Transactional
public UserResponse createUser(CreateUserRequest request) { ... }

@Transactional(readOnly = true)
public List<UserResponse> findAll() { ... }
```

#### DTO

DTO は Java `record` を使用。Entity を API に直接返さない。

```java
// OK
public record UserResponse(Long id, String name, String email) {}

// NG
public class UserResponse {
    private Long id; // Lombok @Data 付き クラスも NG
}
```

#### エンティティ

`@Setter` を付けない。フィールド変更はドメインメソッドで行う。

```java
// OK
public void changePassword(String newHash) {
    this.passwordHash = newHash;
}

// NG
user.setPasswordHash(newHash);
```

#### バリデーション

`jakarta.validation` アノテーション + Controller の `@Valid` を使用。

#### 例外

- 認証系: `AuthException`（`shared/domain/exception/`）
- その他: Spring 標準例外（`ResponseStatusException` 等）
- `GlobalExceptionHandler` で一元ハンドリング

#### セキュリティ（ロール制御）

```java
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAnyRole('TL', 'ADMIN')")
```

`SecurityConfig` の `permitAll()` を変更する場合は CORS 設定も必ず確認する。

#### 共通化ルール

- まず feature 内部へ実装する
- 3回以上重複した場合のみ `shared` 化を検討する
- 業務ロジックを `shared` に置かない
- `BusinessUtil` 等の汎用 Util クラスを乱立させない

---

## 6. フロントエンドアーキテクチャ

> 対応 Cursor ルール: `.cursor/rules/react.mdc`

### フォルダ構成（3層 feature アーキテクチャ）

```
src/
├── app/
│   ├── providers/   ← AuthProvider・useAuth hook（認証状態のグローバル共有）
│   └── layouts/     ← NavBar（グローバルナビゲーション）
│
├── shared/
│   ├── api/         ← Axios クライアント・複数 feature で使うマスタ API
│   ├── types/       ← 複数 feature で使うマスタデータ型定義
│   └── ui/          ← ルートガード（PrivateRoute・TlAdminRoute・AdminRoute）・共通 UI
│
└── features/
    ├── auth/        ← ログイン・パスワード変更（api / types / pages）
    ├── inventory/   ← 棚卸・ダッシュボード・グラフ（api / types / components / pages）
    ├── team/        ← チーム照会・メンバー詳細（api / types / pages）
    ├── master/      ← マスタ管理ページ群（api / types / pages）
    └── interview/   ← 面談機能（api / types）※ページは features/team に統合
```

### 依存方向（厳守）

```
App.tsx → features → shared
```

- `shared` が `features` をインポート禁止
- feature 間で直接インポート禁止（型の参照は許容）
- 新機能は必ず対応する `features/{name}/` フォルダ内に追加する

### コーディングルール（React / TypeScript）

#### コンポーネント

- 関数コンポーネント + hooks のみ（クラスコンポーネント禁止）
- 1コンポーネント = 1責務。200行を超えたら分割を検討する
- `any` 型の使用禁止

```tsx
// OK
const UserCard = ({ user }: { user: User }) => { ... }

// NG
const UserCard = ({ user }: { user: any }) => { ... }
```

#### API 呼び出し

- `async/await` + Axios で呼び出す
- API 呼び出しは必ず `features/{name}/api/` に分離する
- コンポーネント内に直接 `fetch`・`axios` を書かない

```ts
// OK: features/inventory/api/inventoryApi.ts
export async function fetchInventories(): Promise<Inventory[]> { ... }

// NG: コンポーネント内に直接書く
useEffect(() => {
  axios.get('/api/inventories/mine').then(...);
}, []);
```

#### 状態管理

- グローバル状態は **`AuthContext`（`app/providers/AuthProvider.tsx`）のみ**
- 外部状態管理ライブラリ（Redux・Zustand 等）を新たに導入しない
- ローカル状態は `useState` / `useReducer` を使う
- サーバーデータを不必要にグローバル状態に保存しない

#### スタイル

- `index.css` の BEM ライクなクラス名を使う
- インラインスタイルの乱用禁止

```tsx
// OK
<button className="btn-primary">送信</button>
<nav className="navbar__link">ホーム</nav>

// NG
<button style={{ backgroundColor: 'blue' }}>送信</button>
```

#### ルートガード

- 認証が必要なページは `<PrivateRoute>` でラップする
- TL/ADMIN 限定は `<TlAdminRoute>` を使う
- ADMIN 限定は `<AdminRoute>` を使う
- すべて `shared/ui/` に定義済み

#### feature 新規追加時の手順

1. `features/{name}/` フォルダを作成する
2. `api/`・`types/`・`pages/` の各サブフォルダに分けて実装する
3. `shared` へ置くのは3つ以上の feature で使う場合のみ
4. `App.tsx` のルーティングに追加する

#### 共通化ルール

- まず feature 内部へ実装する
- 3回以上重複した場合のみ `shared` 化を検討する
- feature 固有ロジックを `shared` に置かない

---

## 7. 命名規則

| 対象 | 規則 | 例 |
|---|---|---|
| Java クラス | PascalCase | `AuthService`, `LoginRequest` |
| Java メソッド・フィールド | camelCase | `findByUserId`, `passwordHash` |
| DB テーブル・カラム | snake_case | `users`, `password_hash`, `is_active` |
| REST パス | kebab-case（小文字） | `/api/auth/change-password` |
| React コンポーネント | PascalCase | `LoginPage`, `NavBar` |
| CSS クラス | BEM ライク | `.navbar__link`, `.btn-primary` |
| React hooks | camelCase（use プレフィックス） | `useAuth`, `useInventory` |
| API ファイル | camelCase + Api サフィックス | `inventoryApi.ts`, `userApi.ts` |

---

## 8. API 設計ルール

- ベースパス: `/api`
- 認証不要: `POST /api/auth/login`、`GET /api/health` のみ
- 全エンドポイントに `Authorization: Bearer <JWT>` 必須（上記以外）
- レスポンス形式: JSON
- エラーレスポンス: `{ "code": "ERROR_CODE", "message": "..." }`
- バリデーションエラー: `{ "errors": [{ "field": "...", "message": "..." }] }`

### 主要エンドポイント一覧

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

# AI 分析
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
GET/POST/PUT/DELETE /api/it-skills
GET/POST/PUT/DELETE /api/qualifications
GET/POST/PUT/DELETE /api/ad-seminars
GET/POST/PUT/DELETE /api/it-skill-categories
GET/POST/PUT/DELETE /api/qualification-categories
GET/POST/PUT/DELETE /api/ad-seminar-categories
GET/POST/PUT/DELETE /api/seminar-categories
GET/POST/PUT/DELETE /api/skill-levels

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

## 9. 認証・セキュリティ

- パスワードハッシュ: BCrypt コストファクター 12
- JWT 有効期限: `JWT_EXPIRATION_MS`（デフォルト 28800000ms = 8時間）
- `is_initial_password = true` の場合、`InitialPasswordFilter` が `/api/auth/change-password` 以外を 403 にブロックする
- CORS 許可オリジン: `FRONTEND_ORIGIN` 環境変数で制御
- ロール制御: `@PreAuthorize` アノテーションで実装

### 認証フロー

```
POST /api/auth/login
  → JWT 発行
  → localStorage 保存（フロントエンド）
  → 以降は Authorization: Bearer <token> ヘッダーで送信
```

### フィルターチェーン

```
JwtAuthenticationFilter → InitialPasswordFilter → Controller
```

---

## 10. DB スキーマ変更手順

エンティティに新規フィールドを追加する場合、必ず**以下の両方**を更新すること。

1. **Flyway マイグレーション**（本番・CI 用）:
   `apps/backend/src/main/resources/db/migration/V{n}__xxx.sql`

2. **ローカル初期化スクリプト**（Docker ローカル用）:
   `scripts/db/init.sql`

> `application-local.yml` は `flyway.enabled: false` のため、ローカル向けスキーマ変更は `init.sql` に反映する。

**Flyway 済みファイルは後から編集しない**（新バージョン番号で対応する）。

---

## 11. テストルール

> 対応 Cursor ルール: `.cursor/rules/test.mdc`

### バックエンド（Java）

#### テスト配置

- ユニットテスト: `src/test/java/com/skilize/{feature}/`
- 結合テスト: `src/test/java/com/skilize/{feature}/integration/`

#### テスト対象レイヤー

| レイヤー | テスト方針 |
|---|---|
| `application` | ビジネスロジックを重点的にテストする |
| `domain` | ドメインルール・不変条件のユニットテスト |
| `presentation` | Controller テスト（MockMvc 推奨） |
| `infrastructure` | Repository テスト（実 DB 接続を推奨） |

#### モック方針

- **DB はモック化しない**（Repository テストは実 DB 接続で行う）
- 外部 API・LLM 等の外部サービスはモック化する
- `@MockBean` の乱用禁止

#### テスト命名

```java
// {テスト対象}_{条件}_{期待結果}
@Test
void createUser_whenEmailDuplicated_throwsException() { ... }
```

#### アサーション

- `assertThat`（AssertJ）を優先する
- 複数アサーションは `assertAll` でまとめる

### フロントエンド（TypeScript）

#### テスト配置

- `features/{name}/` 配下の `__tests__/` または同階層に `*.test.tsx`

#### テスト対象

- API 関数（`api/` 内）のユニットテスト
- カスタム hooks のテスト（`@testing-library/react-hooks`）
- 重要なページコンポーネントの統合テスト

#### モック方針

- Axios はモック化してよい（`jest.mock`）
- `any` をモック型に使わない

#### アサーション

- `@testing-library/react` の `screen.getByRole` 等のセマンティクスベースのクエリを優先する
- `getByTestId` の乱用禁止

---

## 12. コメントルール

> 対応 Cursor ルール: `.cursor/rules/general.mdc`

- コードの WHAT（何をしているか）は書かない。WHY（なぜそうしているか）のみ書く
- 自明なコードにコメントを付けない
- Javadoc は公開 API のみ、かつ必要最小限

```java
// NG: 何をしているか（コードを読めばわかる）
// ユーザーを取得する
User user = userRepository.findById(id);

// OK: なぜそうしているか（読んでもわからない制約・経緯）
// BCryptは同じ入力でも毎回異なるハッシュを生成するため、直接比較ではなくmatches()を使う
return passwordEncoder.matches(rawPassword, user.getPasswordHash());
```

---

## 13. 絶対禁止事項

| 禁止事項 | 理由 |
|---|---|
| パスワードを平文で DB に保存 | セキュリティ違反 |
| JWT をセッション・Cookie に保存 | XSS・CSRF リスク（localStorage のみ許可） |
| `User` エンティティに `@Setter` を付ける | ドメインメソッドによる変更を強制するため |
| Flyway マイグレーション済みファイルを後から編集 | データ破損リスク |
| `application.yml` に機密情報をハードコード | 情報漏洩リスク（環境変数参照を必須とする） |
| フロントエンドで `any` 型を使う | 型安全性の破壊 |
| `domain` レイヤーから `infrastructure` へ依存 | アーキテクチャ違反 |
| `shared` に業務ロジックを置く | feature isolation の破壊 |
| `@Autowired` フィールドインジェクション | テスト困難・循環依存リスク |
| `@Transactional` を Controller・Repository に置く | トランザクション境界の混乱 |

---

## 14. .cursor/rules との対応表

`.cursor/rules/` の各ファイルは Cursor が自動的に適用する。本ファイルとの役割分担は以下の通り。

| ファイル | `alwaysApply` | 適用グロブ | 本ファイルの対応セクション |
|---|---|---|---|
| `general.mdc` | `true` | — | §7 命名規則、§8 API 設計ルール、§12 コメントルール、§13 絶対禁止事項 |
| `java.mdc` | `false` | `apps/backend/**/*.java` | §5 バックエンドアーキテクチャ |
| `react.mdc` | `false` | `apps/frontend/src/**/*.tsx`, `**/*.ts` | §6 フロントエンドアーキテクチャ |
| `test.mdc` | `false` | `**/*Test.java`, `**/*.test.tsx` 等 | §11 テストルール |

> `.cursor/rules/*.mdc` を変更した場合は、本ファイルの対応セクションも必ず同期すること。

---

## 環境変数一覧

| 変数名 | 説明 | デフォルト |
|---|---|---|
| `COMPOSE_FILE` | Docker Compose ファイルパス | `infra/compose/docker-compose.yml` |
| `SPRING_DATASOURCE_URL` | DB 接続 URL | 必須 |
| `SPRING_DATASOURCE_USERNAME` | DB ユーザー名 | 必須 |
| `SPRING_DATASOURCE_PASSWORD` | DB パスワード | 必須 |
| `JWT_SECRET` | JWT 署名秘密鍵（32文字以上推奨） | 必須 |
| `JWT_EXPIRATION_MS` | JWT 有効期限（ms） | `28800000`（8時間） |
| `SPRING_PROFILES_ACTIVE` | Spring プロファイル | `local`（開発時） |
| `FRONTEND_ORIGIN` | CORS 許可オリジン（カンマ区切り） | `http://localhost:5173,http://localhost:8081` |
| `DB_NAME` | Docker DB 名 | — |
| `DB_USER` | Docker DB ユーザー | — |
| `DB_PASSWORD` | Docker DB パスワード | — |
| `AI_ENABLED` | AI 機能の有効化 | `true` |
| `LLM_PROVIDER` | AI モジュールの LLM プロバイダー | `openai` |
| `LLM_MODEL` | 使用 LLM モデル名 | `gpt-4o` |
| `OPENAI_API_KEY` | OpenAI API キー | `LLM_PROVIDER=openai` 時は必須 |
| `ANTHROPIC_API_KEY` | Anthropic API キー | `LLM_PROVIDER=anthropic` 時は必須 |
| `AI_SECRET_KEY` | Spring Boot → Python AI サービス間の内部認証キー | 必須 |
| `AI_SERVICE_URL` | Python FastAPI の内部 URL | `http://ai:8000` |

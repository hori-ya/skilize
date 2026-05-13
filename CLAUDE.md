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
| Backend | Spring Boot 4.0.6 / Java 22 |
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

## Coding Rules (コーディングルール)

**Backend**
- DTO は Java `record` を使用
- Lombok `@RequiredArgsConstructor` でコンストラクタ注入
- エンティティに `@Setter` を付けず、ドメインメソッド（`changePassword` 等）でフィールド更新
- 例外は `AuthException`（認証系）と Spring の標準例外を使い分ける
- バリデーションは `jakarta.validation` アノテーション + `@Valid`

**Frontend**
- React 関数コンポーネント + hooks のみ
- `async/await` + Axios で API 呼び出し
- グローバル状態は `AuthContext` のみ（外部状態管理ライブラリなし）
- スタイルは `index.css` の BEM ライクなクラス名

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
POST   /api/auth/login
GET    /api/auth/me
POST   /api/auth/change-password
POST   /api/auth/logout
GET    /api/users
POST   /api/users
PUT    /api/users/{id}
DELETE /api/users/{id}
GET    /api/inventory
POST   /api/inventory/...
GET    /api/master/...
GET    /api/fiscal-years
GET    /api/dashboard
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

`.env` ファイルに記載し、`.gitignore` で除外する（`.env.example` に雛形を置く）。

---

## Directory Responsibilities (ディレクトリの責務説明)

| ディレクトリ | 責務 |
|---|---|
| `apps/backend/src/main/java/com/skilize/auth/` | ログイン・JWT 発行・パスワード変更のビジネスロジックとコントローラー |
| `apps/backend/src/main/java/com/skilize/auth/filter/` | JWT 検証フィルター・初期パスワード強制変更フィルター |
| `apps/backend/src/main/java/com/skilize/config/` | Spring Security 設定（フィルターチェーン・CORS・PasswordEncoder） |
| `apps/backend/src/main/java/com/skilize/domain/` | JPA エンティティ・Spring Data リポジトリ（ビジネスロジックなし） |
| `apps/backend/src/main/java/com/skilize/common/` | 共通例外・グローバル例外ハンドラー・エラーレスポンス DTO |
| `apps/backend/src/main/resources/db/migration/` | Flyway マイグレーション（本番・CI 用） |
| `scripts/db/init.sql` | ローカル Docker DB 用の完全初期化スクリプト（DROP→CREATE→INSERT） |
| `apps/frontend/src/api/` | Axios クライアント設定・API 呼び出し関数 |
| `apps/frontend/src/contexts/` | React Context（認証状態の全体共有） |
| `apps/frontend/src/pages/` | 画面単位のコンポーネント |
| `apps/frontend/src/components/` | 複数画面で再利用するコンポーネント（NavBar・ルートガード等） |
| `infra/docker/` | 各サービスの Dockerfile・nginx 設定 |
| `infra/compose/` | Docker Compose ファイル（ローカル用・本番用） |

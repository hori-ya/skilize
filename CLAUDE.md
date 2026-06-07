# CLAUDE.md

---

## Project Overview

**Skilize** — 社内向けスキル棚卸管理Webアプリ。

- ユーザーが毎年度のITスキル・資格・セミナー受講履歴を登録し、上長（TL）がレビューする
- ロール: `GENERAL`（一般ユーザー）/ `TL`（チームリーダー）/ `ADMIN`（管理者）
- 初回ログイン時はパスワード変更が必須（`is_initial_password` フラグで制御）

---

## Tech Stack

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

> 詳細は `.claude/context/tech-stack.md` を参照。

---

## Architecture

```
Browser
  └─ localhost:8081 (nginx)
       ├─ /api/** → backend:8080  (Spring Boot)
       └─ /       → frontend:5173 (Vite dev server)

backend:8080
  └─ PostgreSQL:5432 (db コンテナ / RDS)
  └─ ai:8000        (Python FastAPI AI サービス ← 内部通信のみ)
```

- **認証フロー**: POST /api/auth/login → JWT 発行 → localStorage 保存 → `Authorization: Bearer <token>` で送信
- **フィルターチェーン**: `JwtAuthenticationFilter` → `InitialPasswordFilter` → コントローラー
- **セッション**: Stateless（Spring Session 不使用）

---

## Project Structure

```
skilize/
├── .claude/
│   ├── context/               ← Claude 向けコンテキスト資料（詳細ルール）
│   └── settings.local.json   ← Claude Code 権限設定
├── docs/                      ← 設計・要件ドキュメント
├── scripts/db/init.sql        ← ローカル Docker DB 初期化スクリプト
├── apps/
│   ├── backend/               ← Spring Boot アプリ（Java 21）
│   ├── frontend/              ← React / Vite アプリ
│   └── ai/                   ← Python FastAPI AI サービス
├── infra/
│   ├── docker/                ← Dockerfile・nginx 設定
│   └── compose/               ← Docker Compose ファイル
├── .env                       ← 環境変数（.gitignore 対象）
├── .env.example
└── CLAUDE.md
```

> バックエンド詳細は `.claude/context/backend-architecture.md`、フロントエンド詳細は `.claude/context/frontend-architecture.md` を参照。

---

## Naming Conventions

| 対象 | 規則 | 例 |
|---|---|---|
| Java クラス | PascalCase | `AuthService`, `LoginRequest` |
| Java メソッド・フィールド | camelCase | `findByUserId`, `passwordHash` |
| DB テーブル・カラム | snake_case | `users`, `password_hash`, `is_active` |
| REST パス | kebab-case（小文字） | `/api/auth/change-password` |
| React コンポーネント | PascalCase | `LoginPage`, `NavBar` |
| CSS クラス | BEM ライク | `.navbar__link`, `.btn-primary` |
| TypeScript 型・interface | PascalCase | `InventoryDetail`, `TeamMember` |
| TypeScript 変数・関数 | camelCase | `getMyInventories`, `userId` |

> DTO 命名・i18n キー命名等の詳細は `.claude/context/conventions.md` を参照。

---

## Development Workflow

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
   AI_SECRET_KEY=local-dev-secret
   AI_SERVICE_URL=http://localhost:8000
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
docker compose down -v
docker compose up db
```

> 環境変数の詳細は `.claude/context/environment.md` を参照。

---

## Development Rules: Design First

**実装・改修を行う前に、必ず設計を先に作成・修正すること（必須）:**

1. **新機能・仕様変更**: 実装コードを書き始める前に、`docs/architecture/` の設計書を作成・更新する
2. **バグ修正**: 原因と修正方針を設計書の該当箇所に反映させてから修正する（軽微なバグは省略可）
3. **リファクタリング**: フォルダ構成・依存関係が変わる場合は `CLAUDE.md` を先に更新する
4. **実装フェーズでの仕様変更**: PR 作成前に設計書を実装内容に合わせて更新する（設計書と実装の乖離を残したまま PR を作らない）

> 設計書なきコード変更のプルリクエストは受け入れない。

---

## Development Rules: Doc & Test Sync

**ソースを変更する際は、必ず以下の3つを同時に修正すること（必須）:**

1. **設計書の更新**: `docs/architecture/` 配下のドキュメントを修正する
   - 新しい API エンドポイントは `docs/architecture/api/` に追加
   - 新規 API ドキュメントを追加した場合は `docs/architecture/api/00-conventions.md` の一覧にも追記
   - CLAUDE.md を更新する際は `Naming Conventions` と `Project Structure` を漏れなく更新する
2. **自動テストの更新**: 影響するテストコードを修正・追加する
   - **Backend**: `apps/backend/src/test/` の `*Test.java`
   - **Frontend**: `apps/frontend/src/features/**/*.test.tsx`
   - **Python**: `apps/ai/tests/` の `test_*.py`
   - テストを変更した場合は `docs/testing/test-spec.md` と `docs/testing/{backend|frontend|ai}/` も同時に更新する
   - **テストケース数は連番 ID の最大値から算出する**（例: `BE-MEC-009` まであれば9件）。目視での概算は禁止
3. **i18n の更新**: フロントエンドに新しい UI テキストが追加される場合は `src/i18n/locales/ja/` の JSON を必ず更新する

> ソース変更のみでドキュメント・テストを更新しないプルリクエストは受け入れない。

---

## Forbidden Rules

- `User` エンティティに `@Setter` を付けない（フィールドはドメインメソッドで変更）
- パスワードを平文で DB に保存しない（必ず BCrypt ハッシュ化）
- JWT をセッションや Cookie に保存しない（localStorage のみ）
- `application.yml` に機密情報をハードコードしない（必ず環境変数参照）
- Flyway マイグレーション済みファイルを後から編集しない（新バージョンで対応）
- フロントエンドで `any` 型を使わない
- フロントエンドコンポーネント内に日本語文字列をハードコードしない（`className` 値・コメントを除く）
- `feature/dto/` パッケージを新規作成しない（廃止済み。`request/` / `response/` / `command/` / `query/` を使う）
- `XxxDto` という命名のクラスを新規作成しない（`XxxRequest` / `XxxResponse` / `XxxCommand` / `XxxQueryResult` で命名）
- Service メソッドの引数に `presentation/request/` のクラスを直接渡さない（必ず Mapper で Command に変換する）
- Service クラスが `presentation` パッケージをインポートしない（application → presentation の依存禁止）
- パスワード・JWT・氏名・メールアドレス等の個人情報をログに出力しない
- ログ出力には `@Slf4j`（Lombok）を使用し、`System.out.println` 等の直接出力は使わない
- バックエンドの例外メッセージに日本語文字列を書かない（エラーコード文字列のみ。日本語は `errors.json` で管理）
- フロントエンドで `errors.json` に存在しないエラーコードを表示しない

---

## AI Assistant Rules

- エンティティの新規フィールド追加時は Flyway マイグレーション（`apps/backend/src/main/resources/db/migration/V{n}__xxx.sql`）と `scripts/db/init.sql` の両方を更新する
- `application-local.yml` は `flyway.enabled: false` のため、ローカル向けスキーマ変更は `scripts/db/init.sql` に反映する
- テストデータ（テストユーザー・サンプル棚卸など）は `db/testdata/` に配置する（`db/migration/` は本番 Flyway 対象のため混在禁止）
- `SecurityConfig` の `permitAll()` に追加する際は CORS 設定も確認する
- 新しいロール制御が必要な場合は `@PreAuthorize("hasRole('ADMIN')")` 等を使用する

---

## Context Files Index

実装時は以下のファイルを必要に応じて参照すること:

| ファイル | 内容 |
|---|---|
| `.claude/context/backend-architecture.md` | パッケージ構成・レイヤー責務・DTO ルール・ロギング・バックエンドディレクトリ責務 |
| `.claude/context/frontend-architecture.md` | フォルダ構成・feature 対応表・i18n ルール・コンポーネントルール・フロントエンドディレクトリ責務 |
| `.claude/context/api-reference.md` | REST API エンドポイント全量・認証・セキュリティルール |
| `.claude/context/tech-stack.md` | 技術スタック詳細・Docker 構成・ローカルポート一覧 |
| `.claude/context/conventions.md` | 命名規則詳細・DTO 命名・i18n キー命名・禁止パターン |
| `.claude/context/environment.md` | 環境変数テーブル・`.env` 設定ガイド |

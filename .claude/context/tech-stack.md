# 技術スタック定義

---

## フロントエンド

| 項目 | 内容 |
|------|------|
| フレームワーク | React |
| 言語 | TypeScript |
| ビルドツール | Vite |
| ルーティング | React Router |
| 状態管理 | TBD（React Context / Zustand 等） |
| HTTPクライアント | Axios |
| UIコンポーネント | TBD（MUI / shadcn/ui 等） |

---

## バックエンド

| 項目 | 内容 |
|------|------|
| フレームワーク | Spring Boot 4 |
| 言語 | Java 22 |
| ビルドツール | Gradle |
| 認証・認可 | Spring Security（JWT） |
| ORM | Spring Data JPA / Hibernate |
| DBマイグレーション | Flyway |
| バリデーション | Spring Validation（Jakarta Bean Validation） |
| APIドキュメント | SpringDoc OpenAPI（Swagger UI） |

---

## データベース

| 項目 | 内容 |
|------|------|
| RDBMS | PostgreSQL 16.4 |
| ホスティング | AWS RDS |
| 接続プール | HikariCP（Spring Boot デフォルト） |

---

## インフラ・ホスティング

| 項目 | 内容 |
|------|------|
| ホスティング | AWS EC2 |
| コンテナ管理 | Docker Compose |
| リバースプロキシ | Nginx |
| 環境変数管理 | `.env` ファイル（`.env.example` をテンプレートとして管理） |

---

## Docker構成

| コンテナ | イメージ | 役割 |
|---------|---------|------|
| `frontend` | node:alpine（ビルド） / nginx:alpine（配信） | Reactアプリのビルド・静的配信 |
| `backend` | eclipse-temurin:21-jdk-alpine | Spring Boot APIサーバー |
| `nginx` | nginx:alpine | リバースプロキシ（`/api/*` → backend、それ以外 → frontend） |
| `db` | postgres:16.4（ローカル開発のみ） | ローカル開発用PostgreSQL（本番はRDS） |

---

## 開発ツール

| 項目 | 内容 |
|------|------|
| IDE | VS Code / IntelliJ IDEA |
| コード補完・AI支援 | Claude Code / Cursor |
| バージョン管理 | Git / GitHub |

---

## ポート一覧（ローカル開発）

| サービス | ポート |
|---------|--------|
| Frontend（Vite dev server） | 5173 |
| Backend（Spring Boot） | 8080 |
| Nginx | 80 |
| PostgreSQL（ローカル） | 5432 |

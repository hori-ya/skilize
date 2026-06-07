# 技術スタック詳細

---

## フロントエンド

| 項目 | 内容 |
|---|---|
| フレームワーク | React 18 |
| 言語 | TypeScript |
| ビルドツール | Vite |
| ルーティング | React Router v6 |
| 状態管理 | React Context のみ（AuthProvider）。外部ライブラリ（Zustand・Redux）は使用しない |
| HTTPクライアント | Axios |
| UI スタイル | カスタム CSS（BEM ライク）。Tailwind / CSS Modules / styled-components は使用しない |
| フォーム | useState で管理。React Hook Form / Zod は使用しない |
| 国際化 | i18next + react-i18next |
| グラフ | Recharts |
| Markdown | react-markdown + remark-gfm |
| テスト | Vitest + @testing-library/react |

---

## バックエンド

| 項目 | 内容 |
|---|---|
| フレームワーク | Spring Boot 4.0.6 |
| 言語 | Java 21 |
| ビルドツール | Gradle |
| 認証・認可 | Spring Security + JWT（jjwt 0.12.6）|
| ORM | Spring Data JPA / Hibernate |
| DBマイグレーション | Flyway（本番・CI）/ init.sql（ローカル Docker） |
| バリデーション | jakarta.validation（Bean Validation） |
| ロギング | SLF4J + Logback（@Slf4j by Lombok） |
| ボイラープレート削減 | Lombok（@RequiredArgsConstructor 等） |
| 帳票 PDF | JasperReports |
| Excel | Apache POI |

---

## Python AI サービス

| 項目 | 内容 |
|---|---|
| フレームワーク | FastAPI |
| 言語 | Python 3.11+ |
| LLM 連携 | LangChain（OpenAI / Anthropic を切り替え可能） |
| バリデーション | Pydantic v2（pydantic-settings） |
| DB アクセス | psycopg2（PostgreSQL 直接アクセス） |
| テスト | pytest |

---

## データベース

| 項目 | 内容 |
|---|---|
| RDBMS | PostgreSQL 16 |
| ホスティング | AWS RDS（本番）/ Docker コンテナ（ローカル） |
| 接続プール | HikariCP（Spring Boot デフォルト） |

---

## インフラ・ホスティング

| 項目 | 内容 |
|---|---|
| ホスティング | AWS EC2 |
| コンテナ管理 | Docker Compose |
| リバースプロキシ | Nginx |
| 環境変数管理 | `.env` ファイル（`.env.example` をテンプレートとして管理） |

---

## Docker 構成

| コンテナ | イメージ | 役割 |
|---|---|---|
| `frontend` | node:alpine（ビルド時） | Vite dev server（開発）/ nginx（本番ビルド配信） |
| `backend` | eclipse-temurin:21-jdk-alpine | Spring Boot API サーバー |
| `ai` | python:3.11-slim | Python FastAPI AI サービス |
| `nginx` | nginx:alpine | リバースプロキシ（`/api/*` → backend、それ以外 → frontend） |
| `db` | postgres:16（ローカル開発のみ） | ローカル開発用 PostgreSQL（本番は RDS） |

---

## ポート一覧（ローカル開発）

| サービス | ポート | 備考 |
|---|---|---|
| Nginx（外部公開） | 8081 | ブラウザアクセス用 |
| Frontend（Vite dev server） | 5173 | `npm run dev` 起動時の直接アクセス用 |
| Backend（Spring Boot） | 8080 | API サーバー |
| AI（FastAPI） | 8000 | 内部通信のみ（外部公開なし） |
| PostgreSQL（ローカル） | 5433 | ホストマシンから接続する場合（コンテナ内は 5432） |

---

## 開発ツール

| 項目 | 内容 |
|---|---|
| IDE | VS Code / IntelliJ IDEA |
| AI支援 | Claude Code |
| バージョン管理 | Git / GitHub |

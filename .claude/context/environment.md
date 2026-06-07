# 環境変数ガイド

---

# 設定方法

プロジェクトルートの `.env` ファイルに記載する。`.env` は `.gitignore` で除外されており、リポジトリには含まれない。
`.env.example` に全変数の雛形があるので、コピーして使用する。

```bash
cp .env.example .env
```

---

# 環境変数一覧

## 必須（未設定だと起動しない）

| 変数名 | 説明 |
|---|---|
| `SPRING_DATASOURCE_URL` | DB 接続 URL（例: `jdbc:postgresql://localhost:5433/skilize`） |
| `SPRING_DATASOURCE_USERNAME` | DB ユーザー名 |
| `SPRING_DATASOURCE_PASSWORD` | DB パスワード |
| `JWT_SECRET` | JWT 署名秘密鍵（32文字以上推奨） |
| `AI_SECRET_KEY` | Spring Boot → Python AI サービス間の内部認証キー |

## オプション（デフォルト値あり）

| 変数名 | 説明 | デフォルト |
|---|---|---|
| `COMPOSE_FILE` | Docker Compose ファイルパス | `infra/compose/docker-compose.yml` |
| `JWT_EXPIRATION_MS` | JWT 有効期限（ms） | `28800000`（8時間） |
| `SPRING_PROFILES_ACTIVE` | Spring プロファイル | `local`（開発時） |
| `FRONTEND_ORIGIN` | CORS 許可オリジン（カンマ区切り） | `http://localhost:5173,http://localhost:8081` |
| `AI_SERVICE_URL` | Python FastAPI の内部 URL | `http://ai:8000` |
| `AI_ENABLED` | AI機能の有効化（`false` で LLM 呼び出し停止） | `true` |
| `LLM_PROVIDER` | AI モジュールの LLM プロバイダー（`openai` / `anthropic`） | `openai` |
| `LLM_MODEL` | 使用 LLM モデル名 | `gpt-4o` |
| `CA_CERT_ENABLED` | 社内ルートCA証明書をコンテナに組み込む | `false` |
| `LOAD_TEST_DATA` | テストユーザー（tl01/user01/user02）を DB に投入する | `false` |

## LLM プロバイダー別（いずれか必須）

| 変数名 | 説明 |
|---|---|
| `OPENAI_API_KEY` | OpenAI API キー（`LLM_PROVIDER=openai` の場合は必須） |
| `ANTHROPIC_API_KEY` | Anthropic API キー（`LLM_PROVIDER=anthropic` の場合は必須） |

## Docker Compose 用（`docker-compose.yml` 内で参照）

| 変数名 | 説明 |
|---|---|
| `DB_NAME` | Docker DB 名 |
| `DB_USER` | Docker DB ユーザー |
| `DB_PASSWORD` | Docker DB パスワード |

---

# IntelliJ デバッグ実行時の設定例

IntelliJ の実行構成（Run Configurations）の「Environment variables」に設定する:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/skilize
SPRING_DATASOURCE_USERNAME=skilize
SPRING_DATASOURCE_PASSWORD=password
JWT_SECRET=change-this-to-a-random-256-bit-secret-key-before-use
SPRING_PROFILES_ACTIVE=local
AI_SECRET_KEY=local-dev-secret
AI_SERVICE_URL=http://localhost:8000
```

---

# プロファイル別の挙動

| プロファイル | 説明 |
|---|---|
| `local` | Flyway 無効（`flyway.enabled: false`）。`init.sql` でスキーマ管理。ローカル Docker 開発用 |
| `prod` | Flyway 有効。本番 RDS に接続 |

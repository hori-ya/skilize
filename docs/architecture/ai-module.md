# AIモジュールアーキテクチャ

**バージョン**: 1.0.0  
**作成日**: 2026-05-17

関連資料: [機能要件 3.21](../requirements/functional/functional-requirements.md) / [AI分析 API](./api/09-ai-analysis.md)

---

## 1. 概要

AIキャリア分析機能のために追加する Python ベースのマイクロサービス。  
`apps/ai` に独立した実装として配置し、Spring Boot バックエンドから HTTP で非同期呼び出しを行う。  
画面表示時は LLM を呼び出さず DB に保存済みの結果を返すことで、応答速度とコストを最適化する。

---

## 2. システム構成

```
[ユーザー操作：目標設定完了 → COMPLETED 遷移]
          │
          ▼
[Spring Boot backend:8080]
  ①  ai_career_analyses に status=PENDING で UPSERT
  ②  POST http://ai:8000/analyze（非同期・fire-and-forget）
          │
          ▼
[Python FastAPI ai:8000]
  ③  status を PROCESSING に更新
  ④  PostgreSQL から棚卸・目標・期待コメントデータを取得
  ⑤  LangChain で LLM API を呼び出し
  ⑥  ai_career_analyses に結果 JSON を保存し status=COMPLETED

※ Spring Boot は② 以降の完了を待たない（ユーザーは即座に次の操作へ進める）

[React Frontend]
  GET /api/users/me/ai-analyses
          │
          ▼
[Spring Boot backend:8080]
  SELECT from ai_career_analyses（LLM は呼ばない）
```

---

## 3. フォルダ構成

```
apps/ai/
├── requirements.txt
└── app/
    ├── __init__.py
    ├── main.py                              ← FastAPI エントリーポイント・ルーター登録
    ├── api/
    │   ├── __init__.py
    │   ├── v1/
    │   │   ├── __init__.py
    │   │   └── career_analysis.py          ← POST /analyze エンドポイント
    │   └── dependencies.py                 ← X-Internal-Key 内部認証
    ├── core/
    │   ├── __init__.py
    │   └── config.py                       ← 環境変数管理（pydantic-settings）
    ├── schemas/
    │   ├── __init__.py
    │   ├── career_analysis.py              ← Pydantic リクエスト型（AnalyzeRequest）
    │   └── chat.py                         ← Pydantic 型（ChatRequest / ChatResponse / ChatMessage）
    └── services/
        ├── __init__.py
        ├── llm.py                          ← LLM インスタンス初期化（OpenAI / Anthropic 切り替え）
        ├── career_analysis_service.py      ← 分析ロジック・DB 操作・フォーマッター
        ├── chat_service.py                 ← チャット処理・モード別プロンプト選択
        └── prompts/
            ├── __init__.py
            ├── career_analysis_prompt.py   ← キャリア分析プロンプトテンプレート
            └── chat_prompts.py             ← 通常・文書校正・キャリア相談・ヘルプ用プロンプト

tests/
└── test_chat_service.py                    ← chat_service ユニットテスト（LLM/DB モック化）
```

> AI チャット API の詳細仕様は [10-ai-chat.md](./api/10-ai-chat.md) を参照。

---

## 4. 処理フロー詳細

### 4.1 トリガー（Spring Boot 側）

1. ユーザーが目標設定を完了し `InventoryService.completeGoal()` が呼ばれる
2. 内部で Spring `ApplicationEvent`（`InventoryCompletedEvent`）を発行
3. `AiAnalysisService`（`@Async` + `@EventListener`）が非同期でイベントを受け取り:
   - `ai_career_analyses` に `{user_id, fiscal_year_id, status='PENDING'}` を UPSERT（再提出時も上書き）
   - `POST http://ai:8000/analyze` に `{userId, fiscalYearId}` を送信（レスポンスは待たない）
4. HTTP 送信が失敗した場合はログ出力のみ（処理は継続、ユーザー体験を妨げない）

### 4.2 AI 分析処理（Python 側）

1. FastAPI が `POST /analyze` を受信し `X-Internal-Key` ヘッダーで認証
2. `ai_career_analyses.status` を `PROCESSING` に更新
3. PostgreSQL から以下のデータを取得（ユーザー名・メールは取得しない）:

   | 取得データ | 用途 |
   |-----------|------|
   | 今年度 `it_skill_details`（skill_name・level_value・remarks） | スキル実績 |
   | 今年度 `qualification_details`（qualification_name・acquired_year_month） | 資格実績 |
   | 今年度 `seminar_details`（seminar_name・attended_year_month） | セミナー実績 |
   | 今年度 `inventory_goals`（goal_category・target_name・reason） | 目標 |
   | 前年度の `user_expectations`（tl_expectation・company_expectation） | 期待コメント |
   | 前年度の ITスキル採点（差分把握用。任意） | 成長差分 |

4. LangChain でプロンプトを構築し LLM API を呼び出す
5. レスポンスを以下の JSON 構造にパースして検証:

   ```json
   {
     "summary": "string",
     "strengths": ["string"],
     "growth_areas": ["string"],
     "expectation_fit": "string",
     "recommended_actions": ["string"]
   }
   ```

6. `ai_career_analyses.analysis_result` に JSON を保存し `status='COMPLETED'` に更新
7. エラー時は `status='FAILED'`・`error_message` を保存してログ出力

---

## 5. プロンプト設計方針

### 5.1 個人情報の保護

LLM へ送信するデータに以下を**含めない**:

- ユーザー名（`users.name`）
- メールアドレス（`users.email`）
- ユーザーID（`users.user_id`、ログイン ID）

送信するのはスキル名・採点値・目標内容・期待コメントのみとする。

### 5.2 心理的安全性ガードレール

`recommended_actions`（具体的ネクストステップ）を出力する際のプロンプト指示:

- 「〇〇を徹底する」「〇〇を主導する」のような命令形・高圧的な表現を禁止する
- **「行動の工夫」や「上長への相談のきっかけ」を提案するトーン**に統一させる
  - 例: 「もし既に取り組んでいれば、〇〇の視点を少し加えてみる」
  - 例: 「面談でTLに〇〇について相談してみる」
- 現在の努力を認め、エンパシー（共感）のある文体を必ず含める

### 5.3 構造化出力

LangChain の `JsonOutputParser` を使用し、上記 5項目の JSON 構造を強制する。  
パース失敗時は再試行を1回行い、それでも失敗した場合は `status='FAILED'` とする。

---

## 6. LLM プロバイダー

### 6.1 設計方針（プロバイダー切り替え対応）

LangChain はすべてのプロバイダーが同一の `BaseChatModel` インターフェースを実装しているため、**環境変数を変えるだけでプロバイダーを切り替えられる**。アプリケーションコードの変更は不要。

Python 側の初期化コード（概念）:

```python
# app/services/llm.py
from app.core.config import settings

def build_llm():
    if settings.llm_provider == "anthropic":
        from langchain_anthropic import ChatAnthropic
        return ChatAnthropic(model=settings.llm_model)
    from langchain_openai import ChatOpenAI
    return ChatOpenAI(model=settings.llm_model)
```

### 6.2 対応プロバイダーと環境変数

| `LLM_PROVIDER` | 使用ライブラリ | 必要な API キー変数 | 代表モデル例（`LLM_MODEL`） |
|----------------|--------------|--------------------|-----------------------------|
| `openai`（デフォルト） | `langchain-openai` | `OPENAI_API_KEY` | `gpt-4o` / `gpt-4o-mini` |
| `anthropic` | `langchain-anthropic` | `ANTHROPIC_API_KEY` | `claude-opus-4-7` / `claude-sonnet-4-6` |

> **初期採用**: `openai` + `gpt-4o`。変更は `.env` ファイルの2変数を書き換えるだけ。

### 6.3 環境変数一覧（AI モジュール）

| 環境変数 | 必須 | デフォルト | 説明 |
|---------|:---:|----------|------|
| `LLM_PROVIDER` | — | `openai` | 使用プロバイダー（`openai` / `anthropic`） |
| `LLM_MODEL` | — | `gpt-4o` | 使用モデル名（プロバイダーに合ったモデルを指定） |
| `OPENAI_API_KEY` | ※1 | — | OpenAI API キー（`LLM_PROVIDER=openai` 時は必須） |
| `ANTHROPIC_API_KEY` | ※1 | — | Anthropic API キー（`LLM_PROVIDER=anthropic` 時は必須） |
| `AI_SECRET_KEY` | ○ | — | Spring Boot → Python 間の内部認証キー |
| `DATABASE_URL` | ○ | — | Python から PostgreSQL への接続 URL |

※1 使用するプロバイダーの API キーのみ設定すればよい

---

## 7. インフラ構成（Docker Compose）

### 7.1 新規追加コンテナ

```yaml
# infra/compose/docker-compose.yml への追加
ai:
  build:
    context: ../../
    dockerfile: infra/docker/ai/Dockerfile
  environment:
    - DATABASE_URL=postgresql://skilize:${DB_PASSWORD}@db:5432/skilize
    - AI_SECRET_KEY=${AI_SECRET_KEY}
    - LLM_PROVIDER=${LLM_PROVIDER:-openai}
    - LLM_MODEL=${LLM_MODEL:-gpt-4o}
    - OPENAI_API_KEY=${OPENAI_API_KEY}
    - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
  depends_on:
    - db
  # ポートは内部のみ（外部公開しない）
```

> Docker Compose は `.env` ファイルを自動的に読み込む。`${変数名:-デフォルト値}` の形式で、`.env` に値がなければデフォルト値を使用する。

### 7.2 Spring Boot バックエンドへの追加環境変数

| 環境変数 | 説明 | デフォルト |
|---------|------|----------|
| `AI_SERVICE_URL` | Python FastAPI の内部 URL | `http://ai:8000` |
| `AI_SECRET_KEY` | Spring Boot → Python 間の内部認証キー | なし（必須） |

### 7.3 環境変数の管理方針

```
【ローカル開発】
  .env ファイルに記載（git 管理外 / .gitignore に追加済み）
  → Docker Compose が自動で読み込む

【本番環境（EC2）】
  .env ファイルは使用しない（セキュリティ上、ファイルをサーバーに置かない）
  → docker-compose.prod.yml の environment セクションに直接記載、または
  → AWS Secrets Manager / Parameter Store から取得（Phase 2 検討）
```

### 7.4 `.env.example` への追加項目

```dotenv
# ─── AI Module ────────────────────────────────────
# LLM プロバイダー設定（openai または anthropic）
LLM_PROVIDER=openai
LLM_MODEL=gpt-4o

# OpenAI を使う場合（LLM_PROVIDER=openai）
OPENAI_API_KEY=

# Anthropic を使う場合（LLM_PROVIDER=anthropic）
ANTHROPIC_API_KEY=

# 内部通信認証キー（ランダムな文字列を設定）
AI_SECRET_KEY=change-this-to-a-random-secret

# Spring Boot → AI サービス URL（通常変更不要）
AI_SERVICE_URL=http://ai:8000
```

---

## 8. セキュリティ

| 対策 | 内容 |
|------|------|
| ネットワーク分離 | `ai` コンテナは Docker 内部ネットワークのみ。外部（nginx 経由）に公開しない |
| 内部認証 | Spring Boot → Python の `/analyze` に `X-Internal-Key: ${AI_SECRET_KEY}` ヘッダーを付与。Python 側で検証して不一致は 403 を返す |
| 個人情報除外 | ユーザー名・メールを LLM に送信しない（プロンプト構築時に除外する） |
| 結果の保護 | `ai_career_analyses` は分析対象ユーザー本人・TL（担当チームのみ）・ADMIN のみが読み取り可能 |

---

## 9. エラーハンドリング

| エラー種別 | Python 側の対応 | フロントエンドの表示 |
|-----------|----------------|---------------------|
| LLM API タイムアウト / レートリミット | `status='FAILED'`・`error_message` を保存 | 「分析データがありません」を表示 |
| DB 接続エラー | ログ出力・HTTP 500 を返す | Spring Boot がログ出力のみ |
| JSON パース失敗（1回リトライ後） | `status='FAILED'` を保存 | 「分析データがありません」を表示 |
| Python サービス到達不可 | — | Spring Boot がログ出力のみ（ユーザーへの影響なし） |
| status=PENDING のまま（タイムアウト） | — | 「分析を準備中です」を表示 |

> 自動リトライは Phase 1 のスコープ外。管理者向けの手動リトリガー機能は Phase 2 として検討する。

---

## 10. バックエンド実装メモ（Spring Boot 側）

### 追加パッケージ

```
com.skilize.ai/
├── presentation/
│   ├── AiAnalysisController.java      ← GET /api/users/me/ai-analyses 等
│   └── AiAnalysisResponse.java        ← record（fiscalYear, status, analysisResult）
├── application/
│   ├── AiAnalysisService.java         ← 取得ロジック + Python 呼び出し（@Async）
│   └── InventoryCompletedEventListener.java  ← @EventListener
└── domain/
    ├── AiCareerAnalysis.java           ← Entity
    ├── AiAnalysisStatus.java           ← Enum（PENDING/PROCESSING/COMPLETED/FAILED）
    └── AiCareerAnalysisRepository.java ← JpaRepository
```

### ApplicationEvent

```java
// inventory/application/ 内で発行
public record InventoryCompletedEvent(int userId, int fiscalYearId) {}
```

`InventoryService.completeGoal()` の処理末尾で `applicationEventPublisher.publishEvent(...)` を呼び出す。

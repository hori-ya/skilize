---
paths:
  - "**/*.py"
---

# AI Service Comment Rules（Python / FastAPI）

**対象言語・フレームワーク: Python（FastAPI）**

[../comments.md](../comments.md) の共通ルールに対する、Python / FastAPI 固有の記載例と追加ルール。

---

# ファイルヘッダー（Python 記載例）

```python
# ******************************************************************************
# 機能ID      ：AI
# 機能名      ：AI 機能
# 作成日      ：2026/06/08
# 作成者      ：hori-ya
# ------------------------------------------------------------------------------
# 機能概要：
# AI チャットサービス。モードに応じたシステムプロンプトで LLM に問い合わせる。
# ------------------------------------------------------------------------------
# 更新履歴：
# 2026/06/08 hori-ya 初版作成
# ------------------------------------------------------------------------------
# Copyright (C) 2026 Skilize Project. All Rights Reserved.
# ******************************************************************************
```

---

# クラスコメント

```python
class CareerAnalysisService:
    """
    AI キャリア分析サービス。

    ユーザーのスキル・目標データをもとに LLM でキャリア分析を生成する。
    """
```

---

# 関数コメント

Docstring（Google スタイル: `Args` / `Returns`）を用いる。

```python
def process_chat(message: str, mode: str, user_id: int, history: list[dict]) -> str:
    """
    チャット処理。モードに応じたシステムプロンプトで LLM に問い合わせ、応答テキストを返す。

    Args:
        message: ユーザーメッセージ
        mode: チャットモード（NORMAL / PROOFREADING / CAREER / HELP）
        user_id: ユーザーの内部 ID
        history: 会話履歴（role / content のリスト）

    Returns:
        LLM の応答テキスト
    """
```

---

# 業務ロジックコメント

```python
# hallucination 抑制のためユーザーのスキル・目標データをプロンプトに付与する
return CAREER_SYSTEM_PROMPT.format(inventory_context=context)
```

---

# AI 機能特別ルール

再現性および保守性確保のため、AI 処理の判断理由を必ずコメントとして残す。

| 項目 | コメント記載内容 |
|---|---|
| プロンプト | 設計意図・制約事項 |
| モデル選定 | 選定理由（コスト・品質バランス等） |
| temperature 等の生成パラメータ | 設定値と理由 |
| 会話履歴・コンテキストの上限 | 上限値と理由（コスト・コンテキスト長のバランス） |
| フォールバック処理 | なぜフォールバックが必要か |

```python
# 会話履歴の最大件数（古いものから切り捨て）
# LLM のコンテキスト長制限と API コスト削減のバランスを考慮して 20 件を上限とする
MAX_HISTORY = 20
```

```python
# DB 接続失敗時はデータなしプロンプトにフォールバックする
# ユーザーへのエラー表示を避け、データなしでも AI 応答を返す設計
return CAREER_SYSTEM_PROMPT_NO_DATA
```

---

# FastAPI 固有ルール

全ての公開エンドポイントに Docstring を記載する。

```python
@router.post("/analyze", status_code=202)
async def analyze(request: AnalyzeRequest, background_tasks: BackgroundTasks):
    """
    AI キャリア分析トリガー。

    バックグラウンドタスクで分析を起動し、即座に 202 を返す。
    分析完了後は DB（ai_career_analyses）にステータスを更新する。
    """
```

推論結果に影響を与えるパラメータは理由を記載する（詳細は上記「AI 機能特別ルール」を参照）。

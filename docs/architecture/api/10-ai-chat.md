# AI チャット API

**バージョン**: 1.0.0  
**作成日**: 2026-05-27

関連資料: [AI モジュールアーキテクチャ](../ai-module.md)

---

## 1. 概要

NavBar に設置した AI ボタンから呼び出せるインタラクティブな AI チャット機能。  
通常質問・文書校正・キャリア相談・システムヘルプの 4 モードを提供する。

会話履歴はフロントエンドのメモリ（React state）にのみ保持し、DB への永続化は行わない（Phase 1）。

---

## 2. エンドポイント

```
POST /api/ai/chat
Authorization: Bearer <JWT>
```

### 2.1 リクエスト

```json
{
  "message": "Javaの設計パターンについて教えてください",
  "mode": "NORMAL",
  "history": [
    { "role": "user",      "content": "こんにちは" },
    { "role": "assistant", "content": "こんにちは！何かお手伝いできますか？" }
  ]
}
```

| フィールド | 型 | 必須 | 説明 |
|---|---|:---:|---|
| `message` | string | ○ | ユーザーの入力メッセージ |
| `mode` | enum | ○ | `NORMAL` / `PROOFREADING` / `CAREER` / `HELP` |
| `history` | array | — | 過去の会話履歴（最大 20 件送信を推奨） |
| `history[].role` | string | ○ | `"user"` または `"assistant"` |
| `history[].content` | string | ○ | メッセージ内容 |

### 2.2 レスポンス（200 OK）

```json
{
  "response": "Javaの設計パターンには主に...",
  "mode": "NORMAL"
}
```

### 2.3 AI 無効時のレスポンス（200 OK）

`AI_ENABLED=false` の場合、Python への転送は行わず、以下の形式で即座に返す。

```json
{
  "response": "AI機能は現在無効化されています。ご利用には管理者による設定が必要です。",
  "mode": "NORMAL"
}
```

フロントエンドはこれを通常の AI 応答として会話履歴に表示する。

### 2.4 エラーレスポンス

```json
{ "code": "AI_SERVICE_UNAVAILABLE", "message": "AIサービスが一時的に利用できません" }
```

---

## 3. モード仕様

| モード | 定数 | 説明 |
|---|---|---|
| 通常 | `NORMAL` | 汎用アシスタント。業務一般・技術質問に回答する |
| 文書校正 | `PROOFREADING` | 入力文章を日本語ビジネス文書として添削・修正案を提示する |
| キャリア相談 | `CAREER` | ユーザーの棚卸データを取得し、キャリア観点でアドバイスする |
| ヘルプ | `HELP` | Skilize システムの操作方法・機能について案内する |

### 3.1 キャリアモードのデータ取得

キャリアモード時、Python AI サービスは `userId` を使って PostgreSQL から以下を取得し、プロンプトコンテキストに含める。

| 取得データ | 用途 |
|---|---|
| 今年度の `it_skill_details`（スキル名・レベル） | 現在のスキル状況 |
| 今年度の `inventory_goals`（目標種別・目標名） | 現在の目標設定 |
| `user_expectations`（TL/会社からの期待） | 期待との整合性 |

取得できなかった場合（棚卸未作成等）は、コンテキストなしで一般的なキャリアアドバイスを提供する。

---

## 4. システムフロー

```
[Frontend]
  POST /api/ai/chat { message, mode, history }
          │
          ▼
[Spring Boot backend:8080]  ← JWT 認証済みユーザーの ID を付与
  POST http://ai:8000/chat  { message, mode, userId, history }
  X-Internal-Key: <secret>
          │
          ▼
[Python FastAPI ai:8000]
  ① モードに応じたシステムプロンプトを選択
  ② CAREER モード時: DB からスキル・目標データを取得しコンテキストに追加
  ③ LangChain で会話メッセージを構築し LLM API を呼び出す
  ④ レスポンス文字列を返す
          │
          ▼
[Spring Boot] → [Frontend]
  { response, mode }
```

---

## 5. Python 内部 API（内部専用）

```
POST /chat
X-Internal-Key: <secret>
```

```json
{
  "message": "string",
  "mode": "NORMAL",
  "userId": 123,
  "history": [{ "role": "user", "content": "string" }]
}
```

レスポンス:
```json
{ "response": "string" }
```

---

## 6. UI 仕様

### 6.1 AI ボタン

- NavBar 右端（ログアウトボタン右横）
- 青〜紫グラデーション（`linear-gradient(135deg, #4f8ef7, #7c3aed)`）
- スパークルアイコン（SVG）+ "AI" ラベル

### 6.2 チャットパネル

- 画面右上に固定表示（`position: fixed; top: 70px; right: 20px`）
- 幅: 400px / 高さ: 600px
- 構成（上から）:
  1. ヘッダー（タイトル + 閉じるボタン）
  2. モード選択（4 ボタン）
  3. 会話履歴エリア（スクロール可、最新が末尾）
  4. 入力エリア（textarea + 送信ボタン）
- z-index: 300（ナビバー 100 より上）

### 6.3 メッセージ表示

- ユーザーメッセージ: 右寄せ、青背景
- AI メッセージ: 左寄せ、白背景
- ローディング中: 点滅アニメーション（...）

---

## 7. Spring Boot 実装

```
com.skilize.ai/
├── presentation/
│   ├── AiChatController.java           ← POST /api/ai/chat
│   └── request/
│       └── AiChatRequest.java          ← record (message, mode, history)
├── application/
│   ├── AiChatService.java              ← Python /chat を同期呼び出し
│   ├── command/
│   │   └── AiChatCommand.java          ← (message, mode, userId, history)
│   ├── query/
│   │   └── AiChatQueryResult.java      ← (response, mode)
│   └── mapper/
│       └── AiChatApplicationMapper.java ← Request + userId → Command
```

---

## 8. Python 実装

```
apps/ai/app/
├── api/v1/
│   └── chat.py                         ← POST /chat エンドポイント
├── schemas/
│   └── chat.py                         ← ChatRequest, ChatResponse
└── services/
    ├── chat_service.py                 ← process_chat, DB取得, LLM呼び出し
    └── prompts/
        └── chat_prompts.py             ← モード別システムプロンプト
```

---

## 9. テスト

| レイヤー | テストファイル | 対象 |
|---|---|---|
| Spring Boot | `AiChatControllerTest.java` | 正常系・認証なし・バリデーション |
| Frontend | `AiSupportWidget.test.tsx` | ボタン表示・パネル開閉・モード切替 |
| Python | `tests/test_chat_service.py` | モード別プロンプト・DB データなし時の挙動 |

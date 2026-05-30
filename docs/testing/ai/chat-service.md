# テスト仕様書 — AI / チャットサービス

**コンテナ**: Python AI サービス（pytest）  
**機能**: チャットサービス（プロンプト選択・キャリア文脈処理・LLM 呼び出し）

> テストの実行方法 → [`running-tests.md`](../running-tests.md)

---

## 1. test_chat_service.py

**ファイル**: `apps/ai/tests/test_chat_service.py`  
**テスト対象**: `apps/ai/app/services/chat_service.py`  
**モック対象**: `_fetch_career_context`, `build_llm`, `_build_system_prompt`

### 1.1 TestBuildSystemPrompt

| テスト ID | テスト名 | 前提条件 | 入力 | 期待結果 |
|---|---|---|---|---|
| AI-CS-BSP-001 | `test_normal_mode` | — | `mode="NORMAL"` | NORMAL モード用システムプロンプトが返る |
| AI-CS-BSP-002 | `test_proofreading_mode` | — | `mode="PROOFREADING"` | PROOFREADING モード用システムプロンプトが返る |
| AI-CS-BSP-003 | `test_help_mode` | — | `mode="HELP"` | HELP モード用システムプロンプトが返る |
| AI-CS-BSP-004 | `test_unknown_mode_falls_back_to_normal` | — | `mode="UNKNOWN"` | NORMAL モード用プロンプトにフォールバックする |

### 1.2 TestBuildCareerPrompt

| テスト ID | テスト名 | 前提条件 | 入力 | 期待結果 |
|---|---|---|---|---|
| AI-CS-BCP-001 | `test_returns_no_data_prompt_when_context_empty` | `_fetch_career_context()` が空データを返す | `user_id` | 「データなし」用プロンプトが返る |
| AI-CS-BCP-002 | `test_returns_no_data_prompt_on_db_error` | `_fetch_career_context()` が例外をスロー | `user_id` | 「データなし」用プロンプトが返る（DB エラー時フォールバック）|
| AI-CS-BCP-003 | `test_injects_context_when_available` | `_fetch_career_context()` がスキル・目標データを返す | `user_id` | キャリア情報を含むプロンプトが返る |

### 1.3 TestFormatCareerContext

| テスト ID | テスト名 | 前提条件 | 入力 | 期待結果 |
|---|---|---|---|---|
| AI-CS-FCC-001 | `test_empty_skills_and_goals` | — | `skills=[], goals=[]` | スキル・目標が空のフォーマット結果が返る |
| AI-CS-FCC-002 | `test_with_skills` | — | スキルリスト（名称・レベル）| スキル情報を含むフォーマット結果が返る |
| AI-CS-FCC-003 | `test_with_goals` | — | 目標リスト（内容）| 目標情報を含むフォーマット結果が返る |
| AI-CS-FCC-004 | `test_with_expectation` | — | `expectation="..."` | 期待値情報を含むフォーマット結果が返る |

### 1.4 TestProcessChat

| テスト ID | テスト名 | 前提条件 | 入力 | 期待結果 |
|---|---|---|---|---|
| AI-CS-PC-001 | `test_calls_llm_with_history` | LLM モック設定済み | `message` + 会話履歴 2 件 | LLM が `SystemMessage + 履歴 2 件 + 現在のメッセージ = 計 4 件` で呼ばれる |
| AI-CS-PC-002 | `test_history_truncated_to_max` | LLM モック設定済み | `message` + 会話履歴 30 件（上限超過）| LLM に渡されるメッセージが `SystemMessage + 最大 20 件 + 現在のメッセージ = 22 件以下` に制限される |

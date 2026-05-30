# テスト仕様書 — Backend / AI チャット

**コンテナ**: バックエンド（Spring Boot / JUnit 5）  
**機能**: AI チャット（有効/無効制御・チャット API）

> テストの実行方法 → [`running-tests.md`](../running-tests.md)

---

## 1. AiChatServiceTest

**ファイル**: `apps/backend/src/test/java/com/skilize/ai/application/AiChatServiceTest.java`  
**テスト対象**: `com.skilize.ai.application.AiChatService`  
**テスト種別**: 単体テスト（`@ExtendWith(MockitoExtension.class)`）  
**テスト設定**: `aiEnabled=false` を `ReflectionTestUtils` で注入

| テスト ID | テスト名 | 前提条件 | 入力 | 期待結果 |
|---|---|---|---|---|
| BE-ACHS-001 | `AI無効時は無効メッセージを返しPythonを呼ばない` | `aiEnabled=false`・モード=NORMAL | `AiChatCommand`（メッセージ付き） | Python サービス未呼出し・「AI 機能が無効」旨のメッセージが返る |
| BE-ACHS-002 | `AI無効時はモードがそのまま返される` | `aiEnabled=false`・モード=CAREER | `AiChatCommand`（CAREER モード） | モード=CAREER のままレスポンスが返る |

---

## 2. AiChatControllerTest

**ファイル**: `apps/backend/src/test/java/com/skilize/ai/presentation/AiChatControllerTest.java`  
**テスト対象**: `com.skilize.ai.presentation.AiChatController`  
**テスト種別**: Web レイヤーテスト（`@WebMvcTest`）  
**モック対象**: `AiChatService`, `AiChatApplicationMapper`, `JwtAuthenticationFilter`（素通り設定）, `InitialPasswordFilter`（素通り設定）

### POST /api/ai/chat

| テスト ID | テスト名 | 前提条件 | 入力 | 期待結果 |
|---|---|---|---|---|
| BE-ACHC-001 | `正常系_通常モードでAI応答を返す` | 認証済みユーザー・`AiChatService` 正常返却 | `{"message":"テスト","mode":"NORMAL","history":[]}` | 200 OK・AI レスポンス JSON |
| BE-ACHC-002 | `正常系_会話履歴付きで送信できる` | 認証済みユーザー・過去の会話履歴あり | `{"message":"...","mode":"NORMAL","history":[{"role":"user","content":"..."},{"role":"assistant","content":"..."}]}` | 200 OK・AI レスポンス JSON |
| BE-ACHC-003 | `異常系_未認証はアクセス不可` | 未認証状態 | `Authorization` ヘッダーなし | 401 Unauthorized |
| BE-ACHC-004 | `異常系_メッセージ空はバリデーションエラー` | 認証済みユーザー | `{"message":"","mode":"NORMAL"}` | 400 Bad Request |
| BE-ACHC-005 | `異常系_不正なモードはバリデーションエラー` | 認証済みユーザー | `{"message":"test","mode":"INVALID_MODE"}` | 400 Bad Request |
| BE-ACHC-006 | `異常系_AIサービス障害時は503を返す` | 認証済みユーザー・`AiChatService` が例外スロー | 正常なリクエスト | 503 Service Unavailable |

# テスト仕様書 — Backend / AIキャリア分析

**コンテナ**: バックエンド（Spring Boot / JUnit 5）  
**機能**: AIキャリア分析

> テストの実行方法 → [`running-tests.md`](../running-tests.md)

---

## 1. AiAnalysisServiceTest

**ファイル**: `apps/backend/src/test/java/com/skilize/ai/application/AiAnalysisServiceTest.java`  
**テスト対象**: `com.skilize.ai.application.AiAnalysisService`  
**テスト種別**: 単体テスト（`@ExtendWith(MockitoExtension.class)`）  
**モック対象**: `AiCareerAnalysisRepository`

### FindByUserId

| テストID | テスト名 |
|---|---|
| BE-AAS-001 | `正常系_COMPLETED_analysisResultをパースして返す` |
| BE-AAS-002 | `正常系_FAILED_errorMessageを返しanalysisResultはnull` |
| BE-AAS-003 | `異常系_不正なJSON_パース失敗時はanalysisResultがnullになる` |
| BE-AAS-004 | `正常系_分析結果なし_空リストを返す` |

### UpsertPendingAndTrigger

| テストID | テスト名 |
|---|---|
| BE-AAS-005 | `正常系_既存レコードなし_新規PENDINGレコードを作成する` |
| BE-AAS-006 | `正常系_既存レコードあり_PENDINGへリセットする` |
---

## 2. AiAnalysisControllerTest

**ファイル**: `apps/backend/src/test/java/com/skilize/ai/presentation/AiAnalysisControllerTest.java`  
**テスト対象**: `com.skilize.ai.presentation.AiAnalysisController`  
**テスト種別**: Web レイヤーテスト（`MockMvcBuilders.standaloneSetup`）  
**モック対象**: `AiAnalysisService, UserService`

| テストID | テスト名 |
|---|---|
| BE-AAC-001 | `getMyAnalyses_正常系_200と自分の分析結果一覧を返す` |
| BE-AAC-002 | `getMemberAnalyses_正常系_担当TL_200を返す` |
| BE-AAC-003 | `getMemberAnalyses_異常系_担当外TL_403を返す` |
| BE-AAC-004 | `getMemberAnalyses_異常系_対象ユーザー不在_404を返す` |

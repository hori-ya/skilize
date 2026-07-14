# テスト仕様書 — Backend / 期待コメント

**コンテナ**: バックエンド（Spring Boot / JUnit 5）  
**機能**: 期待コメント

> テストの実行方法 → [`running-tests.md`](../running-tests.md)

---

## 1. ExpectationServiceTest

**ファイル**: `apps/backend/src/test/java/com/skilize/expectation/application/ExpectationServiceTest.java`  
**テスト対象**: `com.skilize.expectation.application.ExpectationService`  
**テスト種別**: 単体テスト（`@ExtendWith(MockitoExtension.class)`）  
**モック対象**: `UserExpectationRepository, UserRepository`

### GetForUser

| テストID | テスト名 |
|---|---|
| BE-ES-001 | `正常系_ADMIN_全ユーザーの期待情報を取得できる` |
| BE-ES-002 | `正常系_担当TL_チームメンバーの期待情報を取得できる` |
| BE-ES-003 | `異常系_担当外TL_403をスロー` |
| BE-ES-004 | `異常系_GENERAL_403をスロー` |
| BE-ES-005 | `異常系_対象ユーザー不在_404をスロー` |

### SaveTlExpectation

| テストID | テスト名 |
|---|---|
| BE-ES-006 | `正常系_担当TL_新規レコードを作成して保存する` |
| BE-ES-007 | `正常系_既存レコードあり_更新して保存する` |
| BE-ES-008 | `異常系_TL以外のロール_403をスロー` |
| BE-ES-009 | `異常系_担当外TL_403をスロー` |

### SaveCompanyExpectation

| テストID | テスト名 |
|---|---|
| BE-ES-010 | `正常系_ADMIN_新規レコードを作成して保存する` |
| BE-ES-011 | `異常系_ADMIN以外のロール_403をスロー` |
---

## 2. ExpectationControllerTest

**ファイル**: `apps/backend/src/test/java/com/skilize/expectation/presentation/ExpectationControllerTest.java`  
**テスト対象**: `com.skilize.expectation.presentation.ExpectationController`  
**テスト種別**: Web レイヤーテスト（`MockMvcBuilders.standaloneSetup`）  
**モック対象**: `ExpectationService`

| テストID | テスト名 |
|---|---|
| BE-EC-001 | `get_正常系_200と期待情報を返す` |
| BE-EC-002 | `get_異常系_担当外TL_403を返す` |
| BE-EC-003 | `saveTl_正常系_200で保存結果を返す` |
| BE-EC-004 | `saveTl_異常系_担当外TL_403を返す` |
| BE-EC-005 | `saveCompany_正常系_200で保存結果を返す` |
| BE-EC-006 | `saveCompany_異常系_ADMIN以外_403を返す` |

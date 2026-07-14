# テスト仕様書 — Backend / 年度管理

**コンテナ**: バックエンド（Spring Boot / JUnit 5）  
**機能**: 年度管理

> テストの実行方法 → [`running-tests.md`](../running-tests.md)

---

## 1. FiscalYearServiceTest

**ファイル**: `apps/backend/src/test/java/com/skilize/fiscalyear/application/FiscalYearServiceTest.java`  
**テスト対象**: `com.skilize.fiscalyear.application.FiscalYearService`  
**テスト種別**: 単体テスト（`@ExtendWith(MockitoExtension.class)`）  
**モック対象**: `FiscalYearRepository, FiscalYearSettingsRepository`

### FindAllOrderByStartDateDesc

| テストID | テスト名 |
|---|---|
| BE-FYS-001 | `正常系_開始日降順にソートして返す` |

### FindCurrent

| テストID | テスト名 |
|---|---|
| BE-FYS-002 | `正常系_現在有効な年度を返す` |

### GetSettings

| テストID | テスト名 |
|---|---|
| BE-FYS-003 | `正常系_設定を返す` |
| BE-FYS-004 | `異常系_設定未初期化_404をスロー` |

### CreateFiscalYear

| テストID | テスト名 |
|---|---|
| BE-FYS-005 | `正常系_年度を新規作成する` |

### UpdateFiscalYear

| テストID | テスト名 |
|---|---|
| BE-FYS-006 | `正常系_activeを指定_指定した値で更新される` |
| BE-FYS-007 | `正常系_activeがnull_現在の値を維持する` |
| BE-FYS-008 | `異常系_対象年度不在_404をスロー` |

### UpdateSettings

| テストID | テスト名 |
|---|---|
| BE-FYS-009 | `正常系_年度開始月を更新する` |
| BE-FYS-010 | `異常系_設定未初期化_404をスロー` |
---

## 2. FiscalYearControllerTest

**ファイル**: `apps/backend/src/test/java/com/skilize/fiscalyear/presentation/FiscalYearControllerTest.java`  
**テスト対象**: `com.skilize.fiscalyear.presentation.FiscalYearController`  
**テスト種別**: Web レイヤーテスト（`MockMvcBuilders.standaloneSetup`）  
**モック対象**: `FiscalYearService`

### List_

| テストID | テスト名 |
|---|---|
| BE-FYC-001 | `正常系_200と年度一覧を返す` |

### Current

| テストID | テスト名 |
|---|---|
| BE-FYC-002 | `正常系_現在年度あり_200を返す` |
| BE-FYC-003 | `異常系_現在年度なし_404を返す` |

### Create

| テストID | テスト名 |
|---|---|
| BE-FYC-004 | `正常系_201で作成した年度を返す` |
| BE-FYC-005 | `異常系_name空_400バリデーションエラー` |

### Update

| テストID | テスト名 |
|---|---|
| BE-FYC-006 | `正常系_200で更新後の年度を返す` |

### Settings

| テストID | テスト名 |
|---|---|
| BE-FYC-007 | `getSettings_正常系_200と年度開始月を返す` |
| BE-FYC-008 | `updateSettings_正常系_200で更新後の月を返す` |
| BE-FYC-009 | `updateSettings_異常系_範囲外の月_400バリデーションエラー` |

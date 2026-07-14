# テスト仕様書 — Backend / ダッシュボード

**コンテナ**: バックエンド（Spring Boot / JUnit 5）  
**機能**: ダッシュボード

> テストの実行方法 → [`running-tests.md`](../running-tests.md)

---

## 1. DashboardServiceTest

**ファイル**: `apps/backend/src/test/java/com/skilize/dashboard/application/DashboardServiceTest.java`  
**テスト対象**: `com.skilize.dashboard.application.DashboardService`  
**テスト種別**: 単体テスト（`@ExtendWith(MockitoExtension.class)`）  
**モック対象**: `FiscalYearRepository, InventoryRepository, ItSkillDetailRepository, QualificationDetailRepository, SeminarDetailRepository`

### GetDashboard

| テストID | テスト名 |
|---|---|
| BE-DS-001 | `正常系_有効年度なし_全項目デフォルト値を返す` |
| BE-DS-002 | `正常系_有効年度ありだが今年度棚卸未作成_棚卸nullで返す` |
| BE-DS-003 | `正常系_今年度棚卸あり_各明細件数を返す` |
| BE-DS-004 | `正常系_他年度の棚卸は今年度棚卸として扱わない` |
---

## 2. DashboardControllerTest

**ファイル**: `apps/backend/src/test/java/com/skilize/dashboard/presentation/DashboardControllerTest.java`  
**テスト対象**: `com.skilize.dashboard.presentation.DashboardController`  
**テスト種別**: Web レイヤーテスト（`MockMvcBuilders.standaloneSetup`）  
**モック対象**: `DashboardService`

| テストID | テスト名 |
|---|---|
| BE-DC-001 | `getDashboard_正常系_年度なし_年度と棚卸がnullで返る` |
| BE-DC-002 | `getDashboard_正常系_年度ありだが棚卸未作成_棚卸がnullで返る` |
| BE-DC-003 | `getDashboard_正常系_棚卸あり_ステータスと明細件数を返す` |

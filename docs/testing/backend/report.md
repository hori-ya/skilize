# テスト仕様書 — Backend / 帳票・レポート

**コンテナ**: バックエンド（Spring Boot / JUnit 5）  
**機能**: 帳票・レポート

> テストの実行方法 → [`running-tests.md`](../running-tests.md)

---

## 1. ReportServiceTest

**ファイル**: `apps/backend/src/test/java/com/skilize/report/application/ReportServiceTest.java`  
**テスト対象**: `com.skilize.report.application.ReportService`  
**テスト種別**: 単体テスト（`@ExtendWith(MockitoExtension.class)`）  
**モック対象**: `InventoryRepository, ItSkillDetailRepository, SeminarDetailRepository, InventoryGoalRepository`

### GenerateInventoryReport

| テストID | テスト名 |
|---|---|
| BE-RS-001 | `異常系_棚卸不在_404をスロー` |
| BE-RS-002 | `異常系_他人の棚卸へGENERALアクセス_FORBIDDENをスロー` |
| BE-RS-003 | `正常系_本人アクセス_PDFバイナリを返す` |
| BE-RS-004 | `正常系_TLアクセス_他人の棚卸でもPDFを返す` |
| BE-RS-005 | `正常系_明細データありでもPDF生成される` |
---

## 2. ReportControllerTest

**ファイル**: `apps/backend/src/test/java/com/skilize/report/presentation/ReportControllerTest.java`  
**テスト対象**: `com.skilize.report.presentation.ReportController`  
**テスト種別**: Web レイヤーテスト（`MockMvcBuilders.standaloneSetup`）  
**モック対象**: `ReportService`

| テストID | テスト名 |
|---|---|
| BE-RC-001 | `downloadInventoryReport_正常系_200とPDFを返す` |

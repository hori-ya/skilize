# テスト仕様書 — Backend / 棚卸

**コンテナ**: バックエンド（Spring Boot / JUnit 5）  
**機能**: 棚卸（前年度比較・差分計算・アクセス制御）

> テストの実行方法 → [`running-tests.md`](../running-tests.md)

---

## 1. InventoryServiceComparisonTest

**ファイル**: `apps/backend/src/test/java/com/skilize/inventory/application/InventoryServiceComparisonTest.java`  
**テスト対象**: `com.skilize.inventory.application.InventoryService#getComparison`  
**テスト種別**: 単体テスト（Mockito）  
**モック対象**: `InventoryRepository`, `ItSkillDetailRepository`, `SkillLevelRepository`, `ItSkillRepository`, `FiscalYearRepository` 他

### GetComparison

| テスト ID | テスト名 | 前提条件 | 入力 | 期待結果 |
|---|---|---|---|---|
| BE-ISC-001 | `正常系_前年度なし_hasPrevYearがfalseで空リストを返す` | 前年度の棚卸が存在しない | `inventoryId` | `hasPrevYear=false`・スキルリスト空 |
| BE-ISC-002 | `正常系_マスタスキルのみ_レベル差分が正しく計算される` | 今年度・前年度ともにマスタスキルあり | 今年度レベル=3、前年度レベル=2 | `currentLevelValue=3, prevLevelValue=2, diff=+1` |
| BE-ISC-003 | `正常系_前年度にないスキル_prevLevelValueとdiffがnull` | 今年度スキルが前年度に存在しない | 前年度にないスキル ID | `prevLevelValue=null, diff=null` |
| BE-ISC-004 | `正常系_カスタムスキルを含む_currentLevelValueとdiffがnull` | 今年度にカスタムスキル（マスタ未登録）あり | カスタムスキルの `detailId` | `currentLevelValue=null, diff=null`（差分計算対象外） |
| BE-ISC-005 | `異常系_他ユーザーの棚卸へGENERALアクセス_FORBIDDENをスロー` | GENERAL ロールのユーザーが他ユーザーの棚卸へアクセス | 他ユーザーの `inventoryId` | `AccessDeniedException`（FORBIDDEN）がスローされる |

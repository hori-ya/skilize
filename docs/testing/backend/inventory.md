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

---

## 2. InventoryServiceTest

**ファイル**: `apps/backend/src/test/java/com/skilize/inventory/application/InventoryServiceTest.java`  
**テスト対象**: `com.skilize.inventory.application.InventoryService`（`getComparison` 以外の全メソッド。`getComparison` は InventoryServiceComparisonTest で検証済み）  
**テスト種別**: 単体テスト（`@ExtendWith(MockitoExtension.class)`）  
**モック対象**: `InventoryRepository, ItSkillDetailRepository, QualificationDetailRepository, SeminarDetailRepository, InventoryGoalRepository, FiscalYearRepository, SkillLevelRepository, ItSkillRepository, QualificationRepository, AdSeminarRepository, SeminarCategoryRepository, ApplicationEventPublisher`

### FindById

| テストID | テスト名 |
|---|---|
| BE-IS-001 | `正常系_本人アクセス_取得できる` |
| BE-IS-002 | `正常系_TLアクセス_取得できる` |
| BE-IS-003 | `異常系_他人GENERALアクセス_FORBIDDENをスロー` |
| BE-IS-004 | `異常系_棚卸不在_404をスロー` |

### Create

| テストID | テスト名 |
|---|---|
| BE-IS-005 | `正常系_新規棚卸を作成する` |
| BE-IS-006 | `異常系_年度不在_404をスロー` |
| BE-IS-007 | `異常系_同年度に既存棚卸あり_409をスロー` |

### ItSkillDetails

| テストID | テスト名 |
|---|---|
| BE-IS-008 | `saveItSkillDetails_正常系_全件洗い替えで保存する` |
| BE-IS-009 | `saveItSkillDetails_異常系_ITスキル不在_404をスロー` |
| BE-IS-010 | `saveItSkillDetails_異常系_スキルレベル不在_404をスロー` |
| BE-IS-011 | `updateItSkillDetailRemarks_正常系_備考を更新する` |
| BE-IS-012 | `updateItSkillDetailRemarks_異常系_別棚卸の明細_403をスロー` |
| BE-IS-013 | `updateItSkillDetailRemarks_異常系_明細不在_404をスロー` |

### QualificationDetails

| テストID | テスト名 |
|---|---|
| BE-IS-014 | `saveQualificationDetails_正常系_日付文字列をLocalDateへ変換して保存する` |
| BE-IS-015 | `saveQualificationDetails_異常系_資格不在_404をスロー` |
| BE-IS-016 | `findQualificationDetails_正常系_一覧を返す` |

### SeminarDetails

| テストID | テスト名 |
|---|---|
| BE-IS-017 | `saveSeminarDetails_正常系_ADセミナー指定時はカテゴリを無視する` |
| BE-IS-018 | `saveSeminarDetails_正常系_自由入力セミナー_カテゴリを解決する` |
| BE-IS-019 | `saveSeminarDetails_異常系_ADセミナー不在_404をスロー` |

### Submit

| テストID | テスト名 |
|---|---|
| BE-IS-020 | `正常系_ステータスがPENDING_GOALに遷移する` |

### GoalReview

| テストID | テスト名 |
|---|---|
| BE-IS-021 | `getGoalReview_正常系_前年度なし_空レスポンスを返す` |
| BE-IS-022 | `getGoalReview_正常系_前年度目標あり_名称と達成状況を返す` |
| BE-IS-023 | `saveGoalReview_正常系_達成状況を更新する` |
| BE-IS-024 | `saveGoalReview_異常系_目標不在_404をスロー` |
| BE-IS-025 | `completeGoalReview_正常系_完了日時が設定される` |

### Goals

| テストID | テスト名 |
|---|---|
| BE-IS-026 | `saveGoals_正常系_目標期間の文字列をLocalDateへ変換して保存する` |
| BE-IS-027 | `completeGoal_正常系_件数条件を満たす_ステータスがCOMPLETEDに遷移しイベント発行される` |
| BE-IS-028 | `completeGoal_異常系_件数不足_GoalIncompleteExceptionをスロー` |
| BE-IS-029 | `findGoals_正常系_一覧を返す` |

### FindMine

| テストID | テスト名 |
|---|---|
| BE-IS-030 | `正常系_ユーザーの棚卸一覧を返す` |

---

## 3. InventoryControllerTest

**ファイル**: `apps/backend/src/test/java/com/skilize/inventory/presentation/InventoryControllerTest.java`  
**テスト対象**: `com.skilize.inventory.presentation.InventoryController`  
**テスト種別**: Web レイヤーテスト（`MockMvcBuilders.standaloneSetup`）  
**モック対象**: `InventoryService, InventoryApplicationMapper`

### mine / getById

| テストID | テスト名 |
|---|---|
| BE-IC-001 | `mine_正常系_200と棚卸一覧を返す` |
| BE-IC-004 | `getById_正常系_200と棚卸詳細を返す` |
| BE-IC-005 | `getById_異常系_他人の棚卸_403を返す` |

### Create

| テストID | テスト名 |
|---|---|
| BE-IC-002 | `正常系_201で作成した棚卸を返す` |
| BE-IC-003 | `異常系_同年度既存棚卸あり_409を返す` |

### ItSkillDetails

| テストID | テスト名 |
|---|---|
| BE-IC-006 | `get_正常系_200と明細一覧を返す` |
| BE-IC-007 | `save_正常系_200で保存結果を返す` |
| BE-IC-008 | `patchRemarks_正常系_200で更新結果を返す` |

### QualificationDetails

| テストID | テスト名 |
|---|---|
| BE-IC-009 | `get_正常系_200と明細一覧を返す` |
| BE-IC-010 | `save_正常系_200で保存結果を返す` |

### SeminarDetails / Submit / Comparison

| テストID | テスト名 |
|---|---|
| BE-IC-011 | `get_正常系_200と明細一覧を返す` |
| BE-IC-012 | `save_正常系_200で保存結果を返す` |
| BE-IC-013 | `submit_正常系_200で提出後のステータスを返す` |
| BE-IC-014 | `getComparison_正常系_200と比較結果を返す` |

### GoalReview

| テストID | テスト名 |
|---|---|
| BE-IC-015 | `get_正常系_200と振り返りデータを返す` |
| BE-IC-016 | `save_正常系_200で更新結果を返す` |
| BE-IC-017 | `complete_正常系_200で完了日時を返す` |

### Goals

| テストID | テスト名 |
|---|---|
| BE-IC-018 | `get_正常系_200と目標一覧を返す` |
| BE-IC-019 | `save_正常系_200で保存結果を返す` |
| BE-IC-020 | `complete_正常系_200で完了ステータスを返す` |
| BE-IC-021 | `complete_異常系_件数不足_422を返す` |

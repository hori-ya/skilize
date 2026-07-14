# テスト仕様書 — Backend / グラフ・チャート

**コンテナ**: バックエンド（Spring Boot / JUnit 5）  
**機能**: グラフ・チャート

> テストの実行方法 → [`running-tests.md`](../running-tests.md)

---

## 1. ChartServiceTest

**ファイル**: `apps/backend/src/test/java/com/skilize/charts/application/ChartServiceTest.java`  
**テスト対象**: `com.skilize.charts.application.ChartService`  
**テスト種別**: 単体テスト（`@ExtendWith(MockitoExtension.class)`）  
**モック対象**: `FiscalYearRepository, InventoryRepository, ItSkillDetailRepository, QualificationDetailRepository, SeminarDetailRepository, InventoryGoalRepository, ItSkillCategoryRepository, ItSkillRepository, SkillLevelRepository`

### GetRadar

| テストID | テスト名 |
|---|---|
| BE-CHS-001 | `正常系_当年度なし_全軸スコア0で返す` |
| BE-CHS-002 | `正常系_当年度棚卸なし_平均0で前年度情報なし` |
| BE-CHS-003 | `正常系_当年度と前年度データあり_平均スコアと前年度名を返す` |
| BE-CHS-004 | `正常系_カスタムスキルは集計対象外` |

### GetGrowth

| テストID | テスト名 |
|---|---|
| BE-CHS-005 | `正常系_提出済み棚卸なし_空の系列を返す` |
| BE-CHS-006 | `正常系_DRAFTの棚卸は集計対象外` |
| BE-CHS-007 | `正常系_マスタスキルのみ_年度順にスコア合計を集計` |
| BE-CHS-008 | `正常系_カスタムスキルが1件以上_カスタムスキル系列が末尾に追加される` |
| BE-CHS-009 | `正常系_カスタムスキルなし_カスタム系列は追加されない` |

### GetHeatmap

| テストID | テスト名 |
|---|---|
| BE-CHS-010 | `正常系_当年度なし_スコアなしヒートマップを返す` |
| BE-CHS-011 | `正常系_当年度の棚卸なし_年度名ありでスコアなし` |
| BE-CHS-012 | `正常系_採点済みスキルあり_平均レベルを算出する` |
| BE-CHS-013 | `正常系_無効化スキルでも採点済みなら表示継続する` |

### GetTimeline

| テストID | テスト名 |
|---|---|
| BE-CHS-014 | `正常系_棚卸なし_空イベントを返す` |
| BE-CHS-015 | `正常系_資格とセミナー実績_ゴールを時系列イベントとして返す` |
| BE-CHS-016 | `正常系_複数棚卸_最新年度のみ実績ソースとして扱う` |
| BE-CHS-017 | `異常系_取得年月nullの資格明細は無視される` |
---

## 2. ChartControllerTest

**ファイル**: `apps/backend/src/test/java/com/skilize/charts/presentation/ChartControllerTest.java`  
**テスト対象**: `com.skilize.charts.presentation.ChartController`  
**テスト種別**: Web レイヤーテスト（`MockMvcBuilders.standaloneSetup`）  
**モック対象**: `ChartService`

| テストID | テスト名 |
|---|---|
| BE-CHC-001 | `getRadar_正常系_200とレーダーデータを返す` |
| BE-CHC-002 | `getGrowth_正常系_200と成長推移データを返す` |
| BE-CHC-003 | `getHeatmap_正常系_200とヒートマップデータを返す` |
| BE-CHC-004 | `getTimeline_正常系_200とタイムラインデータを返す` |

# テスト仕様書 — Backend / 面談メモ

**コンテナ**: バックエンド（Spring Boot / JUnit 5）  
**機能**: 面談メモ

> テストの実行方法 → [`running-tests.md`](../running-tests.md)

---

## 1. InterviewServiceTest

**ファイル**: `apps/backend/src/test/java/com/skilize/interview/application/InterviewServiceTest.java`  
**テスト対象**: `com.skilize.interview.application.InterviewService`  
**テスト種別**: 単体テスト（`@ExtendWith(MockitoExtension.class)`）  
**モック対象**: `InventoryInterviewRepository, InterviewDetailNoteRepository, InventoryRepository`

### FindMine

| テストID | テスト名 |
|---|---|
| BE-IVS-001 | `正常系_TL_自身の面談メモを取得できる` |
| BE-IVS-002 | `異常系_GENERAL_403をスロー` |

### Save

| テストID | テスト名 |
|---|---|
| BE-IVS-003 | `正常系_新規面談メモ_明細ノートを保存する` |
| BE-IVS-004 | `正常系_既存面談メモ_更新して保存する` |
| BE-IVS-005 | `異常系_棚卸不在_404をスロー` |
| BE-IVS-006 | `異常系_GENERAL_403をスロー` |

### FindPrevYear

| テストID | テスト名 |
|---|---|
| BE-IVS-007 | `正常系_前年度棚卸あり_前年度の面談メモを返す` |
| BE-IVS-008 | `正常系_前年度棚卸なし_空を返す` |
| BE-IVS-009 | `異常系_棚卸不在_404をスロー` |

### FindDetailNotes

| テストID | テスト名 |
|---|---|
| BE-IVS-010 | `正常系_明細ノート一覧を返す` |
---

## 2. InterviewControllerTest

**ファイル**: `apps/backend/src/test/java/com/skilize/interview/presentation/InterviewControllerTest.java`  
**テスト対象**: `com.skilize.interview.presentation.InterviewController`  
**テスト種別**: Web レイヤーテスト（`MockMvcBuilders.standaloneSetup`）  
**モック対象**: `InterviewService`

| テストID | テスト名 |
|---|---|
| BE-IVC-001 | `getMine_正常系_200と面談メモを返す` |
| BE-IVC-002 | `getMine_異常系_未作成_404を返す` |
| BE-IVC-003 | `save_正常系_200で保存した面談メモを返す` |
| BE-IVC-004 | `save_異常系_detailNotesがnull_400バリデーションエラー` |
| BE-IVC-005 | `getPrevYear_正常系_200と前年度面談メモを返す` |
| BE-IVC-006 | `getPrevYear_異常系_前年度なし_404を返す` |

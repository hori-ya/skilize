# テスト仕様書 — Backend / マスタ管理

**コンテナ**: バックエンド（Spring Boot / JUnit 5）  
**機能**: マスタ管理

> テストの実行方法 → [`running-tests.md`](../running-tests.md)

---

## 1. MasterServiceTest

**ファイル**: `apps/backend/src/test/java/com/skilize/master/application/MasterServiceTest.java`  
**テスト対象**: `com.skilize.master.application.MasterService`  
**テスト種別**: 単体テスト（`@ExtendWith(MockitoExtension.class)`）  
**モック対象**: `SkillLevelRepository, ItSkillRepository, ItSkillCategoryRepository, QualificationRepository, QualificationCategoryRepository, AdSeminarRepository, AdSeminarCategoryRepository, SeminarCategoryRepository, ItSkillDetailRepository, QualificationDetailRepository`

### SkillLevels

| テストID | テスト名 |
|---|---|
| BE-MS-001 | `getSkillLevels_isActiveなし_全件を返す` |
| BE-MS-002 | `getSkillLevels_isActive指定_絞り込みメソッドを呼ぶ` |
| BE-MS-003 | `createSkillLevel_正常系_新規作成される` |
| BE-MS-004 | `updateSkillLevel_正常系_activeがnull_現在の値を維持する` |
| BE-MS-005 | `updateSkillLevel_異常系_不在_404をスロー` |

### ItSkills

| テストID | テスト名 |
|---|---|
| BE-MS-006 | `getItSkills_isActiveなし_階層順を返す` |
| BE-MS-007 | `getItSkills_isActiveTrue_有効のみを返す` |
| BE-MS-008 | `getItSkills_isActiveFalse_無効のみを返す` |
| BE-MS-009 | `createItSkill_正常系_分類解決して作成する` |
| BE-MS-010 | `createItSkill_異常系_分類不在_400をスロー` |
| BE-MS-011 | `updateItSkill_異常系_スキル不在_404をスロー` |

### ItSkillCategories

| テストID | テスト名 |
|---|---|
| BE-MS-012 | `createItSkillCategory_正常系_親なし_レベル1で作成される` |
| BE-MS-013 | `createItSkillCategory_正常系_親あり_親レベル1の子はレベル2になる` |
| BE-MS-014 | `createItSkillCategory_異常系_親不在_400をスロー` |
| BE-MS-015 | `createItSkillCategory_異常系_最大深度超過_400をスロー` |
| BE-MS-016 | `updateItSkillCategory_異常系_不在_404をスロー` |

### Qualifications

| テストID | テスト名 |
|---|---|
| BE-MS-017 | `getQualifications_isActiveなし_全件を返す` |
| BE-MS-018 | `createQualification_正常系_カテゴリなしで作成できる` |
| BE-MS-019 | `createQualification_異常系_カテゴリ不在_400をスロー` |
| BE-MS-020 | `updateQualification_異常系_不在_404をスロー` |

### QualificationCategories

| テストID | テスト名 |
|---|---|
| BE-MS-021 | `createQualificationCategory_正常系_作成される` |
| BE-MS-022 | `updateQualificationCategory_異常系_不在_404をスロー` |

### AdSeminars

| テストID | テスト名 |
|---|---|
| BE-MS-023 | `getAdSeminars_isActiveFalse_無効のみを返す` |
| BE-MS-024 | `createAdSeminar_異常系_カテゴリ不在_400をスロー` |
| BE-MS-025 | `updateAdSeminar_異常系_不在_404をスロー` |

### AdSeminarCategories

| テストID | テスト名 |
|---|---|
| BE-MS-026 | `createAdSeminarCategory_正常系_作成される` |
| BE-MS-027 | `updateAdSeminarCategory_異常系_不在_404をスロー` |

### SeminarCategoriesAndItSkillCategoryList

| テストID | テスト名 |
|---|---|
| BE-MS-028 | `getSeminarCategories_正常系_有効な分類一覧を返す` |
| BE-MS-029 | `getItSkillCategories_isActiveFalse_無効のみを返す` |
| BE-MS-030 | `findItSkillCategoryById_正常系_取得できる` |

### PromoteCustom

| テストID | テスト名 |
|---|---|
| BE-MS-031 | `getCustomUnregisteredItSkills_正常系_一覧を返す` |
| BE-MS-032 | `promoteItSkill_正常系_新規マスタ登録し明細を紐付ける` |
| BE-MS-033 | `getCustomUnregisteredQualifications_正常系_一覧を返す` |
| BE-MS-034 | `promoteQualification_正常系_新規マスタ登録し明細を紐付ける` |
---

## 2. MasterControllerTest

**ファイル**: `apps/backend/src/test/java/com/skilize/master/presentation/MasterControllerTest.java`  
**テスト対象**: `com.skilize.master.presentation.MasterController`  
**テスト種別**: Web レイヤーテスト（`MockMvcBuilders.standaloneSetup`）  
**モック対象**: `MasterService`

### SkillLevels

| テストID | テスト名 |
|---|---|
| BE-MC-001 | `getSkillLevels_正常系_200と一覧を返す` |
| BE-MC-002 | `createSkillLevel_異常系_levelValue未指定_400バリデーションエラー` |
| BE-MC-003 | `createSkillLevel_正常系_201で作成結果を返す` |

### ItSkills

| テストID | テスト名 |
|---|---|
| BE-MC-004 | `getItSkills_正常系_レベル3スキル_大分類と中分類を解決する` |
| BE-MC-005 | `getItSkills_正常系_レベル1スキル_大分類はそのまま返す` |
| BE-MC-006 | `createItSkill_異常系_categoryId未指定_400バリデーションエラー` |
| BE-MC-007 | `getCustomUnregisteredItSkills_正常系_使用件数付きで返す` |
| BE-MC-008 | `promoteItSkill_正常系_201で昇格結果を返す` |

### Qualifications

| テストID | テスト名 |
|---|---|
| BE-MC-009 | `getQualifications_正常系_200と一覧を返す` |
| BE-MC-010 | `updateQualification_正常系_200で更新結果を返す` |
| BE-MC-011 | `getCustomUnregisteredQualifications_正常系_使用件数付きで返す` |

### QualificationCategories

| テストID | テスト名 |
|---|---|
| BE-MC-012 | `createQualificationCategory_正常系_201で作成結果を返す` |

### AdSeminars

| テストID | テスト名 |
|---|---|
| BE-MC-013 | `getAdSeminars_正常系_200と一覧を返す` |
| BE-MC-014 | `createAdSeminar_正常系_201で作成結果を返す` |

### SeminarCategories

| テストID | テスト名 |
|---|---|
| BE-MC-015 | `getSeminarCategories_正常系_200と一覧を返す` |

### ItSkillCategories

| テストID | テスト名 |
|---|---|
| BE-MC-016 | `getItSkillCategories_正常系_200と一覧を返す` |
| BE-MC-017 | `createItSkillCategory_異常系_name空_400バリデーションエラー` |
| BE-MC-018 | `updateItSkillCategory_正常系_200で更新結果を返す` |

### AdSeminarCategories

| テストID | テスト名 |
|---|---|
| BE-MC-019 | `getAdSeminarCategories_正常系_200と一覧を返す` |

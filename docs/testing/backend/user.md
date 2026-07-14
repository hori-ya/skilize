# テスト仕様書 — Backend / ユーザー管理

**コンテナ**: バックエンド（Spring Boot / JUnit 5）  
**機能**: ユーザー管理

> テストの実行方法 → [`running-tests.md`](../running-tests.md)

---

## 1. UserServiceTest

**ファイル**: `apps/backend/src/test/java/com/skilize/user/application/UserServiceTest.java`  
**テスト対象**: `com.skilize.user.application.UserService`  
**テスト種別**: 単体テスト（`@ExtendWith(MockitoExtension.class)`）  
**モック対象**: `UserRepository, PasswordEncoder, FiscalYearRepository, InventoryRepository`

### Create

| テストID | テスト名 |
|---|---|
| BE-US-001 | `正常系_新規ユーザーを作成する` |
| BE-US-002 | `異常系_ユーザーID重複_409をスロー` |

### Update

| テストID | テスト名 |
|---|---|
| BE-US-003 | `正常系_ユーザー情報を更新する` |
| BE-US-004 | `異常系_対象ユーザー不在_404をスロー` |

### ResetPassword

| テストID | テスト名 |
|---|---|
| BE-US-005 | `正常系_ユーザーID同一の仮パスワードを返す` |
| BE-US-006 | `異常系_対象ユーザー不在_404をスロー` |

### FindActiveMembersFor

| テストID | テスト名 |
|---|---|
| BE-US-007 | `正常系_ADMIN_全有効ユーザーを返す` |
| BE-US-008 | `正常系_TL_自分の担当ユーザーのみ返す` |

### FindCurrentInventory

| テストID | テスト名 |
|---|---|
| BE-US-009 | `正常系_指定年度の棚卸を返す` |
---

## 2. UserControllerTest

**ファイル**: `apps/backend/src/test/java/com/skilize/user/presentation/UserControllerTest.java`  
**テスト対象**: `com.skilize.user.presentation.UserController`  
**テスト種別**: Web レイヤーテスト（`MockMvcBuilders.standaloneSetup`）  
**モック対象**: `UserService`

### List_

| テストID | テスト名 |
|---|---|
| BE-UC-001 | `正常系_200とユーザー一覧を返す_TL名を解決する` |

### Create

| テストID | テスト名 |
|---|---|
| BE-UC-002 | `正常系_201で作成したユーザーを返す` |
| BE-UC-003 | `異常系_不正なロール文字列_400を返す` |
| BE-UC-004 | `異常系_userIdが空_400バリデーションエラー` |

### Update

| テストID | テスト名 |
|---|---|
| BE-UC-005 | `正常系_200で更新後のユーザーを返す` |
| BE-UC-006 | `異常系_不正なロール文字列パターン_400バリデーションエラー` |

| テストID | テスト名 |
|---|---|
| BE-UC-007 | `resetPassword_正常系_200で仮パスワードを返す` |

### GetTeamMembers

| テストID | テスト名 |
|---|---|
| BE-UC-008 | `正常系_今年度棚卸ありのメンバーを返す` |
| BE-UC-009 | `正常系_今年度なし_棚卸情報がnullで返る` |

### GetUserInventories

| テストID | テスト名 |
|---|---|
| BE-UC-010 | `正常系_TL_担当ユーザーの棚卸一覧を返す` |
| BE-UC-011 | `異常系_担当外TL_403を返す` |
| BE-UC-012 | `異常系_対象ユーザー不在_404を返す` |

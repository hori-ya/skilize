# コーディング規約・命名規則

---

# クラス・メソッド・変数の命名

| 対象 | 規則 | 例 |
|---|---|---|
| Java クラス | PascalCase | `AuthService`, `LoginRequest` |
| Java メソッド・フィールド | camelCase | `findByUserId`, `passwordHash` |
| DB テーブル・カラム | snake_case | `users`, `password_hash`, `is_active` |
| REST パス | kebab-case（小文字） | `/api/auth/change-password` |
| React コンポーネント | PascalCase | `LoginPage`, `NavBar` |
| CSS クラス | BEM ライク | `.navbar__link`, `.btn-primary` |
| TypeScript 型・interface | PascalCase | `InventoryDetail`, `TeamMember` |
| TypeScript 変数・関数 | camelCase | `getMyInventories`, `userId` |

---

# Backend DTO 命名規則

`XxxDto` という命名は廃止。責務に応じて以下のパターンで命名する。

| 種別 | 命名パターン | 例 |
|---|---|---|
| HTTP リクエスト | `XxxRequest` | `CreateUserRequest`, `ItSkillDetailsRequest` |
| リクエスト内リスト要素 | `XxxItem` | `ItSkillDetailItem`, `GoalItem` |
| HTTP レスポンス | `XxxResponse` | `UserResponse`, `InventorySummaryResponse` |
| レスポンス内参照オブジェクト | `XxxRef` | `FiscalYearRef` |
| Service 入力 | `XxxCommand` | `LoginCommand`, `ItSkillDetailCommand` |
| Service 出力 | `XxxQueryResult` | `LoginQueryResult`, `ComparisonQueryResult` |
| Mapper | `XxxApplicationMapper` | `AuthApplicationMapper`, `InventoryApplicationMapper` |

> 配置先（`presentation/request/` 等）・依存ルールの詳細は `.claude/context/backend-architecture.md` の「DTO Rules」を参照。

---

# ファイル・フォルダ命名規則

| 対象 | 規則 | 例 |
|---|---|---|
| Java ファイル | PascalCase | `AuthService.java`, `LoginRequest.java` |
| TypeScript コンポーネントファイル | PascalCase | `LoginPage.tsx`, `NavBar.tsx` |
| TypeScript API ファイル | camelCase + `Api` suffix | `inventoryApi.ts`, `chartApi.ts` |
| TypeScript 型ファイル | camelCase | `index.ts` |
| i18n JSON ファイル | feature 名に合わせる（kebab-case） | `inventory.json`, `ai.json` |
| DB マイグレーションファイル | `V{n}__{description}.sql` | `V1__create_users.sql` |

---

# i18n キー命名規則

- ネスト階層 2〜3 段（`section.element` または `section.subsection.element`）
- camelCase のみ（`btn1` などの略称・スネークケース禁止）
- 意味が自明なキー名にする

```json
{
  "loginForm": {
    "title": "ログイン",
    "userIdLabel": "ユーザーID",
    "submitButton": "ログイン"
  }
}
```

- 動的キー（例: `` t(`status.${code}`) ``）は許容するが過剰な動的化は禁止

> ファイル構成・namespace 割り当て・使用方法の詳細は `.claude/context/frontend-architecture.md` の「i18n Rules」を参照。

---

# エラーコードの命名規則

バックエンドの例外メッセージには日本語を書かず、`SCREAMING_SNAKE_CASE` のエラーコード文字列を使用する。フロントエンドの `errors.json` がこれを翻訳する。

```java
// OK
throw new ResponseStatusException(HttpStatus.NOT_FOUND, "INVENTORY_NOT_FOUND");

// NG
throw new ResponseStatusException(HttpStatus.NOT_FOUND, "棚卸が見つかりません");
```

---

# 禁止パターン（命名）

| 禁止 | 代替 |
|---|---|
| `XxxDto` | `XxxRequest` / `XxxResponse` / `XxxCommand` / `XxxQueryResult` |
| `feature/dto/` パッケージ | `presentation/request/` / `presentation/response/` / `application/command/` / `application/query/` |
| `common/util/BusinessUtil` | 3回以上重複した場合のみ `shared` 化（業務ロジックを `shared` に置くことは禁止） |
| `useUserManagement` | feature 固有ロジックは hook に抽出しない（ページに直接実装する） |
| `shared/utils/common.ts` | 目的が明確な名前にする（例: `apiError.ts`） |

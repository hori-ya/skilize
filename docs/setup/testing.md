# テスト実行ガイド

このドキュメントでは、バックエンド（Spring Boot）とフロントエンド（React）のテストコードの実行方法を説明します。

---

## バックエンドテスト（Spring Boot / JUnit 5）

### テストファイル一覧

| ファイル | テスト対象 | 種別 |
|---|---|---|
| `AuthServiceTest.java` | AuthService（login / changePassword / getMe） | 単体テスト |
| `JwtUtilTest.java` | JwtUtil（トークン生成・検証） | 単体テスト |
| `AuthControllerTest.java` | AuthController（Web レイヤー） | Web レイヤーテスト |
| `InventoryServiceComparisonTest.java` | InventoryService#getComparison（前年度比較） | 単体テスト |

### 実行方法

**全テスト実行**（プロジェクトルートから）
```bash
cd apps/backend
./gradlew test
```

**Windows の場合**
```cmd
cd apps\backend
gradlew.bat test
```

**特定クラスのみ実行**
```bash
./gradlew test --tests "com.skilize.auth.application.AuthServiceTest"
./gradlew test --tests "com.skilize.auth.presentation.AuthControllerTest"
./gradlew test --tests "com.skilize.shared.infrastructure.JwtUtilTest"
./gradlew test --tests "com.skilize.inventory.application.InventoryServiceComparisonTest"
```

**テスト結果の確認**
```
apps/backend/build/reports/tests/test/index.html
```

### テスト設計方針

- **単体テスト** — `@ExtendWith(MockitoExtension.class)` で Spring コンテキストなし。Repository・外部サービスはすべて `@Mock` でモック化
- **Web レイヤーテスト** — `@WebMvcTest` で Controller のみロード。フィルター（`JwtAuthenticationFilter`, `InitialPasswordFilter`）は `@MockitoBean` + `doAnswer` で素通り設定
- **DB 接続不要** — H2 不使用。テストプロパティは `src/test/resources/application-test.properties`
- **認証付きリクエスト** — `SecurityMockMvcRequestPostProcessors.user(ourUser)` を使用（`@WithMockUser` は使わない。型が異なるため）

---

## フロントエンドテスト（Vitest + React Testing Library）

### テストファイル一覧

| ファイル | テスト対象 | テスト内容 |
|---|---|---|
| `LoginPage.test.tsx` | ログイン画面 | 正常系ログイン・リダイレクト・エラーメッセージ・ローディング状態 |
| `InventoryHistoryPage.test.tsx` | 棚卸照会画面 | カスタムスキル表示・上昇/下降フィルター時の非表示・新規フィルター時の表示 |

### セットアップ（初回のみ）

```bash
cd apps/frontend
npm install
```

### 実行方法

**全テスト実行（CI 向け）**
```bash
cd apps/frontend
npm test
```

**ウォッチモード（開発時）**
```bash
cd apps/frontend
npm run test:watch
```

### テスト設計方針

- **Vitest** をテストランナーとして使用（Vite との統合、ESM ネイティブ）
- **`@testing-library/react`** でコンポーネントをレンダリングし、ユーザー操作を模倣
- **API モック** — `vi.mock('../api/inventoryApi')` 等でモジュールごと差し替え
- **認証コンテキスト** — `useAuth` フックをモック化して任意の認証状態を再現
- **i18next** — `useTranslation` をモック化し `t(key)` がキーをそのまま返す形で検証

---

## 注意事項

### バックエンド

- `BackendApplicationTests.java` は `@SpringBootTest` で DB 接続が必要です。ローカル実行時は `docker compose up db` で DB を起動してから実行するか、環境変数を設定してください
- 新しいサービスメソッドを追加した場合は、対応する単体テストを同一パッケージに追加してください

### フロントエンド

- テストはブラウザ環境（jsdom）をシミュレートします。ブラウザ固有 API（`localStorage` 等）は利用可能です
- `@testing-library/user-event` を使用した入力シミュレーションは非同期のため、`await` と `waitFor` を適切に使用してください

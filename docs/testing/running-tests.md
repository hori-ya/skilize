# テスト実行ガイド

このドキュメントでは、バックエンド（Spring Boot）、フロントエンド（React）、Python AI サービスのテストコードの実行方法を説明します。

> **テスト仕様書**（各テストケースの前提条件・期待結果）は [`test-spec.md`](test-spec.md) を参照。

---

## バックエンドテスト（Spring Boot / JUnit 5）

### テストファイル一覧

| ファイル | テスト対象 | 種別 |
|---|---|---|
| `AuthServiceTest.java` | AuthService（login / changePassword / getMe） | 単体テスト |
| `JwtUtilTest.java` | JwtUtil（トークン生成・検証） | 単体テスト |
| `AuthControllerTest.java` | AuthController（Web レイヤー） | Web レイヤーテスト |
| `InventoryServiceComparisonTest.java` | InventoryService#getComparison（前年度比較） | 単体テスト |
| `AiChatServiceTest.java` | AiChatService（AI 無効時の制御） | 単体テスト |
| `AiChatControllerTest.java` | AiChatController（POST /api/ai/chat） | Web レイヤーテスト |
| `MasterExcelControllerTest.java` | MasterExcelController（マスタ Excel 出力・取込） | Web レイヤーテスト |
| `ItSkillExcelImporterTest.java` | ItSkillExcelImporter（ITスキル Excel 取込） | 単体テスト |

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
./gradlew test --tests "com.skilize.ai.application.AiChatServiceTest"
./gradlew test --tests "com.skilize.ai.presentation.AiChatControllerTest"
./gradlew test --tests "com.skilize.master.presentation.MasterExcelControllerTest"
./gradlew test --tests "com.skilize.master.infrastructure.excel.ItSkillExcelImporterTest"
```

**テスト結果の確認**
```
apps/backend/build/reports/tests/test/index.html
```

### カバレッジレポート（JaCoCo）

`./gradlew test` を実行すると、テスト完了後に JaCoCo によるカバレッジレポートが自動生成される（`build.gradle` の `finalizedBy jacocoTestReport` 設定による）。

```
apps/backend/build/reports/jacoco/test/html/index.html   ← ブラウザ表示用（行単位で色分け）
apps/backend/build/reports/jacoco/test/jacocoTestReport.xml ← 機械可読用
```

`index.html` を開くと、パッケージ・クラス単位のカバレッジ率に加え、ソースコードを行単位で緑（実行済み）・赤（未実行）・黄（分岐の一部のみ実行）に色分け表示できる。

CI では以下の方法でも確認できる。

- **PR / 任意のブランチ**: GitHub Actions の実行結果から `backend-coverage-report` アーティファクトをダウンロードして確認する
- **main ブランチ（マージ後）**: GitHub Pages に自動デプロイされたレポートを、ダウンロードせずブラウザから直接確認できる

> 設計の詳細は [`docs/architecture/testing-coverage.md`](../architecture/testing-coverage.md) を参照。

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
| `AiSupportWidget.test.tsx` | AI サポートウィジェット | パネル開閉・モード切替・メッセージ送受信・会話履歴管理 |

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

## Python AI テスト（pytest）

### テストファイル一覧

| ファイル | テスト対象 | テスト内容 |
|---|---|---|
| `test_chat_service.py` | chat_service（チャットロジック）| システムプロンプト選択・キャリア文脈フォーマット・LLM 呼び出し・会話履歴制限 |

### 実行方法

```bash
cd apps/ai
pytest tests/
```

**特定ファイルのみ実行**
```bash
pytest tests/test_chat_service.py
```

**詳細ログを表示**
```bash
pytest tests/ -v
```

### テスト設計方針

- **pytest** をテストランナーとして使用
- `unittest.mock.patch` で外部依存（LLM・DB）をモック化
- LLM 呼び出しはすべてモック化されるため、API キーなしで実行可能

---

## 注意事項

### バックエンド

- `BackendApplicationTests.java` は `@SpringBootTest` で DB 接続が必要です。ローカル実行時は `docker compose up db` で DB を起動してから実行するか、環境変数を設定してください
- 新しいサービスメソッドを追加した場合は、対応する単体テストを同一パッケージに追加してください

### フロントエンド

- テストはブラウザ環境（jsdom）をシミュレートします。ブラウザ固有 API（`localStorage` 等）は利用可能です
- `@testing-library/user-event` を使用した入力シミュレーションは非同期のため、`await` と `waitFor` を適切に使用してください

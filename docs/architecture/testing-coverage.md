# テストカバレッジ可視化（JaCoCo）

**バージョン**: 1.0.0
**作成日**: 2026-07-07
**更新日**: 2026-07-07

---

## 1. 目的

CI（GitHub Actions）でバックエンドの自動テストを実行した後、ソースコードのどの行が実行されたかをコード単位で可視化する。JaCoCo（Java Code Coverage）を導入し、テストによって実行されたコード範囲を継続的に把握できるようにする。

対象はバックエンド（Spring Boot / Java）のみ。JaCoCo は JVM 向けのカバレッジツールであり、フロントエンド（Vitest）・AI モジュール（pytest）は対象外（それぞれ別のカバレッジ機構が必要になった場合は別途検討する）。

---

## 2. 前提として解消する課題

CI (`.github/workflows/ci.yml`) のバックエンドテストは `--tests` オプションで以下の4クラスのみを指定して実行しており、実装済みの残り4クラスは CI で実行されていなかった。

**実行されていた**: `AuthServiceTest` / `JwtUtilTest` / `AuthControllerTest` / `InventoryServiceComparisonTest`
**実行されていなかった**: `AiChatServiceTest` / `AiChatControllerTest` / `MasterExcelControllerTest` / `ItSkillExcelImporterTest`

この状態でカバレッジ計測を導入すると、実際のテスト実装より過少なカバレッジ率が計測され誤解を招くため、本対応と合わせて CI を全テストクラス実行に修正する。

---

## 3. 変更内容

### 3.1 CI: 全テストの実行

`.github/workflows/ci.yml` のバックエンドテストステップを、`--tests` による個別クラス指定から `./gradlew test`（全テスト実行）に変更する。

### 3.2 JaCoCo Gradle プラグイン導入

`apps/backend/build.gradle` に `jacoco` プラグインを追加し、`test` タスク完了後に自動で `jacocoTestReport` が実行されるようにする。

```groovy
plugins {
    id 'java'
    id 'jacoco'
    id 'org.springframework.boot' version '4.0.6'
    id 'io.spring.dependency-management' version '1.1.7'
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.named('test') {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
}
```

- HTML レポート: `apps/backend/build/reports/jacoco/test/html/index.html`（ブラウザ表示用。行単位で緑=実行済み・赤=未実行・黄=分岐一部実行の色分け表示）
- XML レポート: `apps/backend/build/reports/jacoco/test/jacocoTestReport.xml`（機械可読。将来的な PR コメント連携等に利用可能）

### 3.3 CI: カバレッジレポートの公開

- 全 PR / push で、HTML レポートをビルドアーティファクトとして保存する（既存の `Upload test report` ステップと同様のパターン）。作業中ブランチの一時確認用
- `main` ブランチへの push（マージ後）にのみ、レポートを **GitHub Pages** へ自動デプロイする。固定 URL からダウンロード不要でアクセスでき、行単位の色付きソース表示を継続的に確認できる
- GitHub Pages へのデプロイには公式 Actions（`actions/configure-pages` / `actions/upload-pages-artifact` / `actions/deploy-pages`）を使用する
- 事前準備としてリポジトリの **Settings → Pages → Source** を `GitHub Actions` に設定する必要がある（リポジトリ設定の変更のため、担当者が別途実施する）

### 3.4 閾値ゲート

今回は導入しない。可視化のみを目的とし、カバレッジ率による CI 失敗（`jacocoTestCoverageVerification`）は行わない。将来的に必要になった場合は別途設計する。

---

## 4. 影響範囲

| ファイル | 変更内容 |
|---|---|
| `apps/backend/build.gradle` | `jacoco` プラグイン追加・`jacocoTestReport` 設定 |
| `.github/workflows/ci.yml` | 全テスト実行への変更、JaCoCo レポートのアーティファクト保存、GitHub Pages へのデプロイジョブ追加 |
| `docs/testing/running-tests.md` | 未掲載だった2テストファイルの追記、カバレッジレポートの確認方法の追記 |

テストケース自体の追加・変更は行わないため、`docs/testing/test-spec.md` の更新は不要。

---

## 5. 関連ドキュメント

| ドキュメント | パス |
|---|---|
| テスト仕様書インデックス | [docs/testing/test-spec.md](../testing/test-spec.md) |
| テスト実行ガイド | [docs/testing/running-tests.md](../testing/running-tests.md) |

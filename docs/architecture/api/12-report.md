# 帳票出力 API

**バージョン**: 1.0.0  
**作成日**: 2026-06-01

---

## 概要

棚卸表を PDF 形式でダウンロードする API。  
帳票エンジンに JasperReports を使用し、Jaspersoft Studio で作成した `.jrxml` レイアウトをバックエンドで読み込んで PDF を生成する。

---

## エンドポイント一覧

| メソッド | パス | 権限 | 説明 |
|---------|------|------|------|
| GET | `/api/inventories/{id}/report` | GENERAL / TL / ADMIN | 棚卸表 PDF ダウンロード |

---

## GET /api/inventories/{id}/report

### 概要

指定した棚卸 ID の棚卸表を PDF で返す。

アクセス制御:
- `GENERAL` ロール: 自分の棚卸のみダウンロード可能（他ユーザーは 403）
- `TL` ロール: 自チームメンバーの棚卸をダウンロード可能
- `ADMIN` ロール: すべての棚卸をダウンロード可能

### リクエスト

**パスパラメータ**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `id` | Long | 棚卸 ID |

**ヘッダー**

```
Authorization: Bearer <JWT>
```

### レスポンス

**成功時（200 OK）**

```
Content-Type: application/pdf
Content-Disposition: attachment; filename="inventory_report_{id}.pdf"
```

レスポンスボディは PDF バイナリ。

**エラー時**

| コード | HTTP | 説明 |
|--------|:----:|------|
| `NOT_FOUND` | 404 | 指定の棚卸が存在しない |
| `FORBIDDEN` | 403 | 他ユーザーの棚卸へのアクセス権なし |

---

## 実装方針

### 帳票エンジン

| 項目 | 内容 |
|------|------|
| ライブラリ | JasperReports（`net.sf.jasperreports:jasperreports`） |
| レイアウト定義 | Jaspersoft Studio で作成した `.jrxml` ファイル |
| レイアウト格納場所 | `apps/backend/src/main/resources/reports/inventoryReport.jrxml` |
| データ受け渡し方式 | JasperReports Parameters + JRBeanCollectionDataSource |
| 出力形式 | PDF のみ |

### データソース方式

JasperReports のデータ渡し方式として JDBC データソースは使用しない。  
`ReportService` がビジネスデータを収集し、以下の形式で JasperReports に渡す。

```
Map<String, Object> parameters  ← ヘッダー情報（ユーザー名・年度・提出日など）
JRBeanCollectionDataSource      ← 明細行（ITスキル・資格・セミナー一覧）
```

### フォント

JasperReports 標準フォントは日本語非対応のため、以下のいずれかで対応する。

- **推奨**: IPA フォントまたは Noto Sans CJK を Font Extension として JAR 化し `build.gradle` に追加
- **代替**: `apps/backend/src/main/resources/fonts/` にフォントファイルを配置し、`.jrxml` で `fontFamily` を指定

> フォント対応はレイアウト（`.jrxml`）作成後に実施する。

### ⚠️ 既知の課題：フォント未配置による本番環境での帳票出力失敗（2026-07-15 判明・未対応）

現在の実装は上記「代替」案を採用し、`apps/backend/src/main/resources/fonts/fonts.xml` で `MSGothic`（`msgothic.ttc`, Windows 同梱フォント）を参照している。しかし `msgothic.ttc` 本体は **ライセンス上の理由で `.gitignore` に登録されており、Git リポジトリに含まれていない**（`.gitignore:47`、コメントに "コピー元: C:\Windows\Fonts\msgothic.ttc" と明記）。

**影響**:
- リポジトリを新規 clone した環境（CI・本番デプロイ含む）には `msgothic.ttc` が存在しないため、PDF エクスポート時に `net.sf.jasperreports.engine.JRRuntimeException: Could not load the following font` が発生する
- **本番環境で実際に帳票出力（GET /api/inventories/{id}/report）が異常終了することを確認済み**
- テストコード（`ReportServiceTest`）ではフォント未配置環境で該当テストをスキップする対応のみ実施済み（`assumeFontAvailable()`）。プロダクションコードの修正は未対応

**今後の対応方針（要検討）**:
- 上記「推奨」案（IPA フォントまたは Noto Sans CJK 等、再配布可能なライセンスのフォントに切り替え、通常の依存関係として `build.gradle` に追加する）への移行を推奨
- 移行後は `.gitignore` の除外設定・`fonts/fonts.xml` のフォントファミリー定義・`.jrxml` の `fontName` 指定を新フォントに合わせて更新する

### レイアウトファイル

```
apps/backend/src/main/resources/reports/
└── inventoryReport.jrxml    ← Jaspersoft Studio で作成・保存するレイアウトファイル
```

`.jrxml` は XML 形式のため Git 管理対象とする。  
バックエンドは起動時に `.jrxml` をコンパイルし、リクエスト毎にデータを流し込んで PDF を生成する。

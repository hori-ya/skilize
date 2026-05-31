# マスタ Excel 出力・取込 API

**対象画面**: SCR-012（ITスキルマスタ）、SCR-016（資格分類マスタ）、SCR-011（ADマスタ）  
**作成日**: 2026-05-31

---

## 1. 機能概要

ADMIN ユーザーが以下 3 種のマスタデータを Excel ファイルで一括出力・取込できる機能を追加する。

| 対象マスタ | 含むシート |
|-----------|-----------|
| ITスキルマスタ | IT分類 / ITスキル |
| 参考資格マスタ | 資格カテゴリ / 参考資格 |
| ADマスタ | ADカテゴリ / ADセミナー |

**基本方針**
- 出力と取込は同一の Excel レイアウトを使用する（出力したファイルをそのまま取込に使える）
- 取込は **all-or-nothing**（1 行でもエラーがあれば全件ロールバック）
- ライブラリ: Apache POI (poi-ooxml)
- ファイル形式: `.xlsx` (OOXML)

---

## 2. Excel ファイルレイアウト

### 2.1 共通ルール

| 項目 | 仕様 |
|------|------|
| ヘッダー行 | 1 行目（太字・背景色あり）|
| データ開始行 | 2 行目以降 |
| ID 列 | 空白 = 新規登録 / 数値 = 既存レコード更新 |
| 有効列 | `有効` = is_active true / `無効` = is_active false |
| 並び順 | Excel の行の並び順（上から何番目か）を sort_order として設定。**並び順列は存在しない** |
| 参考列 | セルに薄いグレー背景を付け、取込時は無視する |
| 非存在データ | Excel に存在しない DB レコードは論理削除（is_active = false）とする |

### 2.2 出力時のソート順

| シート | ソート条件 |
|--------|-----------|
| IT分類 | 階層レベル（L1→L2→L3）昇順、同レベル内は親カテゴリID昇順、さらに sort_order 昇順 |
| ITスキル | L1 カテゴリの sort_order → L2 → L3 → スキルの sort_order（階層パス順）|
| 資格カテゴリ | sort_order 昇順 |
| 参考資格 | カテゴリの sort_order 昇順、同カテゴリ内は sort_order 昇順 |
| ADカテゴリ | sort_order 昇順 |
| ADセミナー | カテゴリの sort_order 昇順、同カテゴリ内は sort_order 昇順 |

### 2.3 ITスキルマスタ（ItSkillMaster.xlsx）

#### Sheet 1: "IT分類"

| 列 | ヘッダー | 型 | 取込時の扱い | 必須 |
|----|---------|-----|------------|------|
| A | ID | 数値 | 新規/更新判定 | - |
| B | 親カテゴリID | 数値 | 親紐付け（空白 = L1 ルート）| - |
| C | 階層レベル | 数値（1/2/3）| **参考列**（自動計算）| - |
| D | カテゴリ名 | 文字列 | 登録値 | ✓ |
| E | 有効 | 文字列（有効/無効）| 登録値（省略時: 新規=有効、更新=現在値維持）| - |

> **取込時の並び順**: 同じ親カテゴリを持つ行の相対的な出現順（上から 1, 2, 3…）を sort_order として設定する。

#### Sheet 2: "ITスキル"

| 列 | ヘッダー | 型 | 取込時の扱い | 必須 |
|----|---------|-----|------------|------|
| A | カテゴリID | 数値 | カテゴリ紐付け | ✓ |
| B | 大分類 | 文字列 | **参考列** | - |
| C | 中分類 | 文字列 | **参考列** | - |
| D | 小分類 | 文字列 | **参考列** | - |
| E | ID | 数値 | 新規/更新判定 | - |
| F | スキル名 | 文字列 | 登録値 | ✓ |
| G | 説明 | 文字列 | 登録値 | - |
| H | 有効 | 文字列（有効/無効）| 登録値（省略時: 新規=有効、更新=現在値維持）| - |

> **取込時の並び順**: 同じカテゴリID を持つ行の相対的な出現順（上から 1, 2, 3…）を sort_order として設定する。

### 2.4 参考資格マスタ（QualificationMaster.xlsx）

#### Sheet 1: "資格カテゴリ"

| 列 | ヘッダー | 型 | 取込時の扱い | 必須 |
|----|---------|-----|------------|------|
| A | ID | 数値 | 新規/更新判定 | - |
| B | カテゴリ名 | 文字列 | 登録値 | ✓ |
| C | 有効 | 文字列（有効/無効）| 登録値（省略時: 新規=有効、更新=現在値維持）| - |

> **取込時の並び順**: 行の出現順（上から 1, 2, 3…）を sort_order として設定する。

#### Sheet 2: "参考資格"

| 列 | ヘッダー | 型 | 取込時の扱い | 必須 |
|----|---------|-----|------------|------|
| A | カテゴリID | 数値 | カテゴリ紐付け（空白 = 未分類）| - |
| B | ID | 数値 | 新規/更新判定 | - |
| C | カテゴリ名 | 文字列 | **参考列** | - |
| D | 資格名 | 文字列 | 登録値 | ✓ |
| E | 説明 | 文字列 | 登録値 | - |
| F | 有効 | 文字列（有効/無効）| 登録値（省略時: 新規=有効、更新=現在値維持）| - |

> **取込時の並び順**: 同じカテゴリID を持つ行の相対的な出現順（上から 1, 2, 3…）を sort_order として設定する。未分類（カテゴリID 空白）は互いにグループとして扱う。

### 2.5 ADマスタ（AdSeminarMaster.xlsx）

#### Sheet 1: "ADカテゴリ"

| 列 | ヘッダー | 型 | 取込時の扱い | 必須 |
|----|---------|-----|------------|------|
| A | ID | 数値 | 新規/更新判定 | - |
| B | カテゴリ名 | 文字列 | 登録値 | ✓ |
| C | 有効 | 文字列（有効/無効）| 登録値（省略時: 新規=有効、更新=現在値維持）| - |

> **取込時の並び順**: 行の出現順（上から 1, 2, 3…）を sort_order として設定する。

#### Sheet 2: "ADセミナー"

| 列 | ヘッダー | 型 | 取込時の扱い | 必須 |
|----|---------|-----|------------|------|
| A | カテゴリID | 数値 | カテゴリ紐付け（空白 = 未分類）| - |
| B | カテゴリ名 | 文字列 | **参考列** | - |
| C | ID | 数値 | 新規/更新判定 | - |
| D | AD名 | 文字列 | 登録値 | ✓ |
| E | 説明 | 文字列 | 登録値 | - |
| F | 有効 | 文字列（有効/無効）| 登録値（省略時: 新規=有効、更新=現在値維持）| - |

> **取込時の並び順**: 同じカテゴリID を持つ行の相対的な出現順（上から 1, 2, 3…）を sort_order として設定する。未分類（カテゴリID 空白）は互いにグループとして扱う。

---

## 3. API 設計

### 3.1 エンドポイント一覧

| メソッド | パス | 概要 | 権限 |
|---------|------|------|------|
| GET | /api/master-excel/it-skills/download | ITスキルマスタ Excel 出力 | ADMIN |
| POST | /api/master-excel/it-skills/upload | ITスキルマスタ Excel 取込 | ADMIN |
| GET | /api/master-excel/qualifications/download | 参考資格マスタ Excel 出力 | ADMIN |
| POST | /api/master-excel/qualifications/upload | 参考資格マスタ Excel 取込 | ADMIN |
| GET | /api/master-excel/ad-seminars/download | ADマスタ Excel 出力 | ADMIN |
| POST | /api/master-excel/ad-seminars/upload | ADマスタ Excel 取込 | ADMIN |

---

### 3.2 出力 API

#### GET /api/master-excel/{target}/download

**リクエスト**
- 認証: `Authorization: Bearer <token>`
- クエリパラメータ: なし
- ボディ: なし

**レスポンス（200 OK）**
```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="ItSkillMaster.xlsx"
```
バイナリ（xlsx ファイル）

**出力内容**
- 有効・無効問わず全件を出力する
- ソート順は「2.2 出力時のソート順」に従う
- ヘッダー行: 太字・背景色（薄い水色 `#D0E4F7`）
- 参考列: 背景色（薄いグレー `#F0F0F0`）

---

### 3.3 取込 API

#### POST /api/master-excel/{target}/upload

**リクエスト**
- 認証: `Authorization: Bearer <token>`
- Content-Type: `multipart/form-data`
- フォームフィールド: `file` (.xlsx ファイル)

**レスポンス（200 OK） - 成功時**
```json
{
  "created": 3,
  "updated": 12,
  "deleted": 2
}
```

**レスポンス（400 Bad Request） - バリデーションエラー時**
```json
{
  "code": "EXCEL_IMPORT_ERROR",
  "message": "取込データに誤りがあります",
  "errors": [
    {
      "sheet": "ITスキル",
      "row": 5,
      "column": "F",
      "message": "スキル名は必須です"
    },
    {
      "sheet": "IT分類",
      "row": 12,
      "column": "B",
      "message": "親カテゴリID=999 が存在しません"
    }
  ]
}
```

**エラーコード**

| コード | HTTP | 説明 |
|--------|:----:|------|
| `EXCEL_IMPORT_ERROR` | 400 | バリデーションエラー（詳細は errors 配列） |
| `EXCEL_FORMAT_ERROR` | 400 | ファイル形式不正（xlsx でない、シートが見つからない等）|

---

## 4. バックエンド実装設計

### 4.1 依存ライブラリ追加（build.gradle）

```groovy
implementation 'org.apache.poi:poi-ooxml:5.3.0'
```

### 4.2 パッケージ構成

```
com.skilize.master/
  presentation/
    MasterExcelController.java        ← NEW
    response/
      MasterImportResponse.java       ← NEW（record: created, updated, deleted）
  application/
    MasterExcelService.java           ← NEW（@Transactional）
    query/
      MasterImportQueryResult.java    ← NEW（record: created, updated, deleted, errors）
      MasterImportErrorDetail.java    ← NEW（record: sheet, row, column, message）
  infrastructure/
    excel/
      ExcelStyleHelper.java           ← NEW（ヘッダー・参考列スタイル生成）
      ItSkillExcelExporter.java       ← NEW
      ItSkillExcelImporter.java       ← NEW
      QualificationExcelExporter.java ← NEW
      QualificationExcelImporter.java ← NEW
      AdSeminarExcelExporter.java     ← NEW
      AdSeminarExcelImporter.java     ← NEW
```

### 4.3 クラス責務

#### MasterExcelController

```java
@RestController
@RequestMapping("/api/master-excel")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class MasterExcelController {

    // GET /api/master-excel/it-skills/download
    // → byte[] → ResponseEntity<byte[]>
    //   Content-Type: application/vnd.openxmlformats-...
    //   Content-Disposition: attachment; filename="ItSkillMaster.xlsx"

    // POST /api/master-excel/it-skills/upload
    // → @RequestParam("file") MultipartFile
    // → ResponseEntity<MasterImportResponse>

    // 同様に qualifications / ad-seminars
}
```

#### MasterExcelService（@Transactional）

```java
// 出力系（readOnly = true）
public byte[] exportItSkillExcel();
public byte[] exportQualificationExcel();
public byte[] exportAdSeminarExcel();

// 取込系（書き込みトランザクション）
public MasterImportQueryResult importItSkillExcel(MultipartFile file);
public MasterImportQueryResult importQualificationExcel(MultipartFile file);
public MasterImportQueryResult importAdSeminarExcel(MultipartFile file);
```

#### ItSkillExcelExporter（infrastructure）

```java
// XSSFWorkbook を生成して byte[] を返す
// Sheet1: "IT分類" - 全カテゴリを2.2のソート順で出力
// Sheet2: "ITスキル" - 全スキルを2.2のソート順で出力
public byte[] export(List<ItSkillCategory> categories, List<ItSkill> skills);
```

#### ItSkillExcelImporter（infrastructure）

```java
// MultipartFile を受け取り Row ごとにパースして中間データを返す
// パース失敗（型不正・シート欠如）は ExcelFormatException をスロー
public ItSkillImportData parse(MultipartFile file);

record ItSkillImportData(List<CategoryRow> categoryRows, List<SkillRow> skillRows) {}

// rowNum: Excel 上の行番号（2始まり）
// siblingOrder: 同じ parentId 内での相対出現順（1始まり）→ sort_order に使用
record CategoryRow(Integer id, Integer parentId, String name, Boolean active,
                   int rowNum, int siblingOrder) {}

// siblingOrder: 同じ categoryId 内での相対出現順（1始まり）→ sort_order に使用
record SkillRow(Integer id, Integer categoryId, String name, String description,
                Boolean active, int rowNum, int siblingOrder) {}
```

> Qualification・AdSeminar の Exporter/Importer も同様の構造。

### 4.4 取込処理フロー（MasterExcelService）

```
1. parse(file) → ItSkillImportData
   ↓ ExcelFormatException → 400 EXCEL_FORMAT_ERROR

2. validate(importData) ← エラーを全量収集
   カテゴリシート:
   a. カテゴリ名が空でないか
   b. ID 指定時: DB に存在するか
   c. parentId 指定時: DB または同ファイル内の新規行に存在するか
   d. 親が L3 でないか（最大3階層）
   スキルシート:
   a. スキル名が空でないか
   b. categoryId が指定されているか
   c. categoryId が DB または同ファイル内の新規カテゴリに存在するか
   d. ID 指定時: DB に存在するか
   ↓ errors 非空 → MasterImportQueryResult(errors) を返却（ロールバック）

3. save categories（カテゴリ先行）
   - ID 空白 → 新規作成（sort_order = siblingOrder）
   - ID あり → 既存取得 → update（sort_order = siblingOrder）

4. 論理削除（カテゴリ）
   - DB の全カテゴリ ID のうち、Excel に存在しない ID を is_active = false に設定

5. save skills（カテゴリ保存後）
   - ID 空白 → 新規作成（sort_order = siblingOrder）
   - ID あり → 既存取得 → update（sort_order = siblingOrder）

6. 論理削除（スキル）
   - DB の全スキル ID のうち、Excel に存在しない ID を is_active = false に設定

7. return MasterImportQueryResult(created=N, updated=M, deleted=D)
```

**論理削除の判定**
- 「Excel に存在しない」= Excel の ID 列に数値として記載されていない DB 上のレコード
- ID 空白行（新規登録）は論理削除の対象外
- 既に `is_active = false` のレコードが Excel に存在しない場合: 変更なし（削除済みのため `deleted` カウント対象外）
- 既に `is_active = false` のレコードが Excel に ID 付きで存在する場合: 通常の更新として処理する

**同ファイル内参照**
- 新規カテゴリ（ID 空白）を同ファイルのスキル行が参照する場合、カテゴリ保存後に確定した ID でスキル行のカテゴリを解決する

### 4.5 バリデーションエラーの収集方針

- すべての行を走査し、エラーを全量収集してから返す
- エラー上限: 100 件（超過時は末尾に「100 件を超えるエラーがあります」を追加）

---

## 5. フロントエンド実装設計

### 5.1 API クライアント（masterApi.ts への追加）

```typescript
// 出力: Blob でダウンロード
export const downloadItSkillExcel = () =>
  apiClient.get<Blob>('/master-excel/it-skills/download', { responseType: 'blob' });
export const downloadQualificationExcel = () =>
  apiClient.get<Blob>('/master-excel/qualifications/download', { responseType: 'blob' });
export const downloadAdSeminarExcel = () =>
  apiClient.get<Blob>('/master-excel/ad-seminars/download', { responseType: 'blob' });

// 取込: FormData で送信
export const uploadItSkillExcel = (file: File) =>
  apiClient.post<MasterImportResult>('/master-excel/it-skills/upload',
    Object.assign(new FormData(), { file }), // FormData
    { headers: { 'Content-Type': 'multipart/form-data' } });
// qualifications / ad-seminars も同様
```

### 5.2 型定義（master.ts への追加）

```typescript
export interface MasterImportResult {
  created: number;
  updated: number;
  deleted: number;
}

export interface MasterImportError {
  sheet: string;
  row: number;
  column: string;
  message: string;
}

export interface MasterImportErrorResponse {
  code: string;
  message: string;
  errors: MasterImportError[];
}
```

### 5.3 UI 変更

対象ページ: `ItSkillMasterPage.tsx`、`QualificationMasterPage.tsx`、`AdSeminarMasterPage.tsx`

各ページの右下に固定表示（`position: fixed`）の Excel ボタン群（`.excel-fab`）を配置する。  
「先頭に戻るボタン」（`right: 28px`）と重複しないよう `right: 88px` に配置し、縦並びとする。

```
画面右下（固定表示）
  [Excel 出力]  ← .excel-fab__btn
  [Excel 取込]  ← .excel-fab__btn
                      ↑ right: 88px
  [  ↑  ]             ← scroll-top-btn (right: 28px)
```

**Excel 出力ボタン**
- クリック → ダウンロード API 呼び出し → `<a download>` でファイル保存

**Excel 取込ボタン**
- クリック → `<input type="file" accept=".xlsx">` をトリガー
- ファイル選択後 → アップロード API 呼び出し
- 成功: 取込完了モーダルを表示（登録・更新・削除の件数を表形式で表示、OK ボタンで閉じる）
- エラー: エラーモーダルでエラー一覧を表示（シート・行番号・列・メッセージ）

### 5.4 i18n（`src/i18n/locales/ja/master.json` への追加）

```json
{
  "excel": {
    "download": "Excel 出力",
    "upload": "Excel 取込",
    "importing": "取込中...",
    "importResultTitle": "取込完了",
    "importResultCreated": "登録",
    "importResultUpdated": "更新",
    "importResultDeleted": "削除",
    "importResultUnit": "件",
    "importResultOk": "OK",
    "importError": "取込エラー",
    "importErrorClose": "閉じる",
    "importErrorDetail": "{{sheet}}シート {{row}}行目 {{column}}列: {{message}}",
    "importErrorOverflow": "エラーが多すぎます。ファイルを修正してから再度取込してください。"
  }
}
```

---

## 6. 実装時の制約・考慮事項

### 6.1 ファイルサイズ上限
- 実用上の想定件数: ITスキル最大 500 件、資格最大 200 件、AD 最大 300 件

### 6.2 文字コード
- xlsx 内部は UTF-8 のため文字コード問題は発生しない

### 6.3 同一名称の重複チェック
- カテゴリ名の一意性は DB UNIQUE 制約が保証する
- 取込時に事前チェックは行わず、DB 制約違反は 409 として GlobalExceptionHandler でハンドリング

### 6.4 セキュリティ
- アップロードファイルは一時ファイルへの書き出しは行わず、`MultipartFile.getInputStream()` からストリームで処理する
- ADMIN ロールのみ許可（`@PreAuthorize("hasRole('ADMIN')")`）

### 6.5 既存エンドポイントとの整合性
- 取込は既存の `MasterService` の create/update メソッドは使わず、`MasterExcelService` が直接 Repository を操作し 1 トランザクションで完結させる
- `MasterExcelService` は `MasterService` と独立した Application Service として配置する

### 6.6 論理削除と参照整合性
- 棚卸明細が参照しているスキル/資格/ADが削除対象になった場合でも論理削除（is_active = false）のみ行い、参照先レコードは保持する
- 削除対象カテゴリ配下のスキル/資格/ADが Excel に存在する場合は、スキル側の処理で正常に更新される（カテゴリが削除されてもスキルの category_id は変わらない）

---

## 7. テスト方針

### バックエンド（`apps/backend/src/test/`）

| テスト対象 | テストクラス | 検証内容 |
|-----------|------------|---------|
| `ItSkillExcelImporter` | `ItSkillExcelImporterTest` | 正常パース・空行スキップ・有効列=無効・有効列省略・シート欠如 |
| `MasterExcelController` | `MasterExcelControllerTest` | 権限（ADMIN のみ）・ダウンロード Content-Type・取込成功・バリデーションエラー・ファイル形式不正 |

### フロントエンド（`apps/frontend/src/features/master/`）

- 出力ボタンのクリックで API が呼ばれることの確認（モック）
- 取込成功時の結果モーダル表示（件数確認）・データ再フェッチ
- 取込エラー時のエラーモーダル表示

---

## 8. 関連ドキュメント

- [05-master-admin.md](./05-master-admin.md) — マスタ管理 API（既存）
- [00-conventions.md](./00-conventions.md) — API 設計規約
- `docs/testing/test-spec.md` — テスト仕様書（取込機能のテストケース追加要）

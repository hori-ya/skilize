# マスタ Excel 出力・取込 テスト仕様

## テスト対象

- `ItSkillExcelImporterTest` — Excel パース処理の単体テスト
- `MasterExcelControllerTest` — HTTP レイヤーテスト（権限・レスポンス形式）

---

## ItSkillExcelImporter テスト

### BE-MEI-001: 正常パース（カテゴリ・スキル）

| 項目 | 内容 |
|------|------|
| テスト ID | BE-MEI-001 |
| テスト名 | カテゴリとスキルを正常にパースできる |
| 前提条件 | IT分類・ITスキルシートを含む正常な xlsx |
| 操作 | `importer.parse(file)` |
| 期待結果 | categoryRows=2件・skillRows=2件、siblingOrder が同一グループ内で連番 |

### BE-MEI-002: 空行スキップ

| 項目 | 内容 |
|------|------|
| テスト ID | BE-MEI-002 |
| テスト名 | 空行はスキップされる |
| 前提条件 | データ行の間に空行を含む xlsx |
| 操作 | `importer.parse(file)` |
| 期待結果 | 空行はカウントされない |

### BE-MEI-003: 有効列=無効

| 項目 | 内容 |
|------|------|
| テスト ID | BE-MEI-003 |
| テスト名 | 有効列が「無効」の場合は false になる |
| 前提条件 | 有効列に「無効」を設定した xlsx |
| 操作 | `importer.parse(file)` |
| 期待結果 | `active = false` |

### BE-MEI-004: 有効列省略

| 項目 | 内容 |
|------|------|
| テスト ID | BE-MEI-004 |
| テスト名 | 有効列が省略の場合は null になる |
| 前提条件 | 有効列が空白の xlsx |
| 操作 | `importer.parse(file)` |
| 期待結果 | `active = null` |

### BE-MEI-005: IT分類シート欠如

| 項目 | 内容 |
|------|------|
| テスト ID | BE-MEI-005 |
| テスト名 | IT分類シートが存在しない場合は ExcelFormatException がスローされる |
| 前提条件 | IT分類シートが存在しない xlsx |
| 操作 | `importer.parse(file)` |
| 期待結果 | `ExcelFormatException`（"IT分類" 含むメッセージ） |

### BE-MEI-006: ITスキルシート欠如

| 項目 | 内容 |
|------|------|
| テスト ID | BE-MEI-006 |
| テスト名 | ITスキルシートが存在しない場合は ExcelFormatException がスローされる |
| 前提条件 | ITスキルシートが存在しない xlsx |
| 操作 | `importer.parse(file)` |
| 期待結果 | `ExcelFormatException`（"ITスキル" 含むメッセージ） |

---

## MasterExcelController テスト

### BE-MEC-001: ITスキルダウンロード

| 項目 | 内容 |
|------|------|
| テスト ID | BE-MEC-001 |
| テスト名 | ITスキルマスタをダウンロードできる |
| 前提条件 | ADMIN ロール |
| 操作 | `GET /api/master-excel/it-skills/download` |
| 期待結果 | 200, Content-Type = xlsx, Content-Disposition = attachment |

### BE-MEC-002: 権限不足

| 項目 | 内容 |
|------|------|
| テスト ID | BE-MEC-002 |
| テスト名 | ADMIN 以外は 403 が返る |
| 前提条件 | GENERAL ロール |
| 操作 | `GET /api/master-excel/it-skills/download` |
| 期待結果 | 403 |

### BE-MEC-003: ITスキル取込成功

| 項目 | 内容 |
|------|------|
| テスト ID | BE-MEC-003 |
| テスト名 | ITスキルマスタを取込できる |
| 前提条件 | ADMIN ロール, Service が created=3・updated=5・deleted=1 を返す |
| 操作 | `POST /api/master-excel/it-skills/upload` (multipart) |
| 期待結果 | 200, `{ created:3, updated:5, deleted:1 }` |

### BE-MEC-004: 取込バリデーションエラー

| 項目 | 内容 |
|------|------|
| テスト ID | BE-MEC-004 |
| テスト名 | バリデーションエラーがある場合は 400 が返る |
| 前提条件 | ADMIN ロール, Service がエラーリストを返す |
| 操作 | `POST /api/master-excel/it-skills/upload` |
| 期待結果 | 400, `{ code:"EXCEL_IMPORT_ERROR", errors:[...] }` |

### BE-MEC-005: ファイル形式不正

| 項目 | 内容 |
|------|------|
| テスト ID | BE-MEC-005 |
| テスト名 | ファイル形式が不正な場合は 400 が返る |
| 前提条件 | ADMIN ロール, Service が ExcelFormatException をスロー |
| 操作 | `POST /api/master-excel/it-skills/upload` |
| 期待結果 | 400, `{ code:"EXCEL_FORMAT_ERROR" }` |

### BE-MEC-006: 参考資格ダウンロード

| 項目 | 内容 |
|------|------|
| テスト ID | BE-MEC-006 |
| テスト名 | 参考資格マスタをダウンロードできる |
| 前提条件 | ADMIN ロール |
| 操作 | `GET /api/master-excel/qualifications/download` |
| 期待結果 | 200, xlsx |

### BE-MEC-007: 参考資格取込

| 項目 | 内容 |
|------|------|
| テスト ID | BE-MEC-007 |
| テスト名 | 参考資格マスタを取込できる |
| 前提条件 | ADMIN ロール |
| 操作 | `POST /api/master-excel/qualifications/upload` |
| 期待結果 | 200, `{ created:2 }` |

### BE-MEC-008: ADダウンロード

| 項目 | 内容 |
|------|------|
| テスト ID | BE-MEC-008 |
| テスト名 | ADマスタをダウンロードできる |
| 前提条件 | ADMIN ロール |
| 操作 | `GET /api/master-excel/ad-seminars/download` |
| 期待結果 | 200, xlsx |

### BE-MEC-009: AD取込

| 項目 | 内容 |
|------|------|
| テスト ID | BE-MEC-009 |
| テスト名 | ADマスタを取込できる |
| 前提条件 | ADMIN ロール |
| 操作 | `POST /api/master-excel/ad-seminars/upload` |
| 期待結果 | 200, `{ deleted:2 }` |

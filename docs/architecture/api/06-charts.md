# グラフ・可視化 API

**対象画面**: SCR-002（ダッシュボード — グラフセクション）  
**関連要件**: functional-requirements.md § 3.18

---

## エンドポイント一覧

| メソッド | パス | 概要 | 権限 |
|---------|------|------|------|
| GET | /api/charts/radar | レーダーチャート用データ | 全員 |
| GET | /api/charts/growth | 成長グラフ（時系列推移）用データ | 全員 |
| GET | /api/charts/heatmap | ヒートマップ用データ | 全員 |
| GET | /api/charts/timeline | ラーニング・タイムライン用データ | 全員 |

> すべてのエンドポイントは JWT 認証必須。**ログインユーザー自身のデータのみ**返す（Phase 1）。

---

## GET /api/charts/radar

SCR-002 レーダーチャート（スキルバランス）の描画に使用する。  
有効な分類1（`it_skill_categories.level = 1`）を軸として、今年度・前年度それぞれのスキル平均採点を返す。

**権限**: 全員

**Response 200**

```json
{
  "currentFiscalYear": "FY2025",
  "prevFiscalYear": "FY2024",
  "hasCurrentYearData": true,
  "maxLevelValue": 5,
  "axes": [
    {
      "category1Id": 1,
      "category1Name": "インフラ",
      "currentAvgScore": 3.2,
      "prevAvgScore": 2.8
    },
    {
      "category1Id": 2,
      "category1Name": "開発言語・フレームワーク",
      "currentAvgScore": 4.0,
      "prevAvgScore": 3.5
    },
    {
      "category1Id": 3,
      "category1Name": "セキュリティ",
      "currentAvgScore": 0.0,
      "prevAvgScore": null
    }
  ]
}
```

**フィールド定義**

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `currentFiscalYear` | string \| null | 現在の有効年度名。有効年度が存在しない場合 `null` |
| `prevFiscalYear` | string \| null | 前年度名。前年度棚卸が存在しない場合 `null` |
| `hasCurrentYearData` | boolean | 今年度のITスキル採点が1件以上存在するか。フロントのグラフ表示制御に使用 |
| `maxLevelValue` | int | レベルマスタの `MAX(level_value)`（有効なもの）。チャートのスケール基準 |
| `axes` | array | 有効な分類1を全件含む（採点なしの分類1も `currentAvgScore: 0.0` で返す） |
| `axes[].category1Id` | int | 分類1 ID |
| `axes[].category1Name` | string | 分類1名 |
| `axes[].currentAvgScore` | number | 今年度の採点済みスキルの `level_value` 平均（小数第1位四捨五入）。採点なしは `0.0` |
| `axes[].prevAvgScore` | number \| null | 前年度の平均。前年度棚卸なし、または採点なしは `null` |

**集計ルール**

- 対象スキル：`it_skill_id IS NOT NULL`（カスタムスキル除外）かつ当該分類1の配下スキル
  - 分類1への所属は `it_skill_categories` の親子関係を辿って判定（スキルの leaf category から level=1 まで遡る）
- 平均値：`SUM(level_value) / COUNT(採点済みスキル)` — 未採点スキルは分母・分子ともに含めない
- 有効年度の棚卸が未作成の場合：`hasCurrentYearData: false`、全 `currentAvgScore: 0.0`
- 削除済みスキルマスタ（`is_active = false`）の棚卸明細は集計に含める

---

## GET /api/charts/growth

SCR-002 成長グラフ（時系列スコア推移）の描画に使用する。  
提出済みの棚卸が存在する全年度を横軸とし、分類1別のスコア合計を積み上げ棒グラフ形式で返す。

**権限**: 全員

**Response 200**

```json
{
  "fiscalYears": ["FY2023", "FY2024", "FY2025"],
  "series": [
    {
      "category1Id": 1,
      "category1Name": "インフラ",
      "yearlyTotalScores": [12, 15, 18]
    },
    {
      "category1Id": 2,
      "category1Name": "開発言語・フレームワーク",
      "yearlyTotalScores": [20, 22, 25]
    },
    {
      "category1Id": 3,
      "category1Name": "セキュリティ",
      "yearlyTotalScores": [0, 6, 8]
    }
  ]
}
```

**フィールド定義**

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `fiscalYears` | string[] | 提出済み棚卸のある年度名一覧（`fiscal_years.name` 昇順）。提出済み棚卸が存在しない場合は空配列 |
| `series` | array | 有効な分類1を全件含む |
| `series[].category1Id` | int | 分類1 ID |
| `series[].category1Name` | string | 分類1名 |
| `series[].yearlyTotalScores` | int[] | `fiscalYears` の各年度に対応するスコア合計（インデックス対応）。その年度の棚卸なし or 採点なしは `0` |

> `yearlyTotalScores[i]` は `fiscalYears[i]` に対応する。配列長は `fiscalYears` と一致する。

**集計ルール**

- 対象年度：`inventories.status IN ('PENDING_GOAL', 'COMPLETED')`（提出済みのみ）
- スコア合計：`SUM(skill_levels.level_value)`。カスタムスキル（`it_skill_id = NULL`）は除外
- 削除済みスキルマスタの棚卸明細はスコアに含める
- 削除済み分類1（`is_active = false`）のスキルは「(削除済み分類)」の系列に集約せず、`series` には有効な分類1のみ含める

---

## GET /api/charts/heatmap

SCR-002 ヒートマップ（スキル詳細分布）の描画に使用する。  
今年度の棚卸における分類1 × 分類2 別の平均採点と、ツールチップ表示用の個別スキル情報を返す。

**権限**: 全員

**Response 200**

```json
{
  "currentFiscalYear": "FY2025",
  "hasCurrentYearData": true,
  "maxLevelValue": 5,
  "rows": [
    {
      "category1Id": 1,
      "category1Name": "インフラ",
      "cells": [
        {
          "category2Id": 10,
          "category2Name": "ネットワーク",
          "avgLevelValue": 3.3,
          "scoredSkillCount": 3,
          "skills": [
            { "skillName": "TCP/IP",       "levelValue": 4 },
            { "skillName": "DNS",          "levelValue": 3 },
            { "skillName": "HTTP/HTTPS",   "levelValue": 3 },
            { "skillName": "BGP",          "levelValue": null }
          ]
        },
        {
          "category2Id": 11,
          "category2Name": "サーバー",
          "avgLevelValue": null,
          "scoredSkillCount": 0,
          "skills": [
            { "skillName": "Linux", "levelValue": null },
            { "skillName": "Windows Server", "levelValue": null }
          ]
        },
        {
          "category2Id": null,
          "category2Name": "(分類なし)",
          "avgLevelValue": 2.0,
          "scoredSkillCount": 1,
          "skills": [
            { "skillName": "VMware", "levelValue": 2 }
          ]
        }
      ]
    }
  ]
}
```

**フィールド定義**

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `currentFiscalYear` | string \| null | 有効年度名。有効年度が存在しない場合 `null` |
| `hasCurrentYearData` | boolean | 今年度のITスキル採点が1件以上存在するか |
| `maxLevelValue` | int | レベルマスタの `MAX(level_value)`（有効なもの）。色スケールの基準 |
| `rows` | array | 有効な分類1を全件含む |
| `rows[].cells` | array | 当該分類1配下の分類2一覧。分類2を持たないスキルは `category2Id: null`、`category2Name: "(分類なし)"` のセルに集約 |
| `cells[].avgLevelValue` | number \| null | 採点済みスキルの `level_value` 平均（小数第1位四捨五入）。採点なし（`scoredSkillCount = 0`）は `null` |
| `cells[].scoredSkillCount` | int | 採点済みスキル件数（ツールチップの補足表示用） |
| `skills[]` | array | ツールチップ表示用。ユーザーが採点したスキル＋有効な未採点スキルを含む |
| `skills[].levelValue` | int \| null | 採点値。未採点は `null` |

**集計ルール**

- 分類2の所属判定：スキルの leaf category から level=2 の祖先を辿る。level=1 が直接 leaf の場合は `category2Id: null`
- 有効なスキルマスタ（`is_active = true`）は採点の有無にかかわらず `skills` に含める
- 削除済みスキルマスタ（`is_active = false`）でユーザーが採点したものは `skills` に含め `avgLevelValue` の集計にも算入する
- カスタムスキルは除外

---

## GET /api/charts/timeline

SCR-002 ラーニング・タイムライン（学習活動）の描画に使用する。  
全年度の資格取得・セミナー受講実績と、最新年度の学習目標を時系列順に返す。

**権限**: 全員

**Response 200**

```json
{
  "events": [
    {
      "type": "QUALIFICATION",
      "lane": "ACHIEVEMENT",
      "name": "基本情報技術者試験",
      "yearMonth": "2023-06-01",
      "isPast": true
    },
    {
      "type": "AD_SEMINAR",
      "lane": "ACTIVITY",
      "name": "マネジメント基礎",
      "yearMonth": "2025-02-01",
      "isPast": true
    },
    {
      "type": "FREE_SEMINAR",
      "lane": "ACTIVITY",
      "name": "AWS re:Invent",
      "yearMonth": "2025-11-01",
      "isPast": true
    },
    {
      "type": "GOAL_QUALIFICATION",
      "lane": "ACHIEVEMENT",
      "name": "応用情報技術者試験",
      "yearMonth": "2025-10-01",
      "isPast": false
    },
    {
      "type": "GOAL_IT_SKILL",
      "lane": "ACTIVITY",
      "name": "Java",
      "yearMonth": "2026-03-01",
      "isPast": false
    },
    {
      "type": "GOAL_AD",
      "lane": "ACTIVITY",
      "name": "マネジメント応用",
      "yearMonth": "2025-09-01",
      "isPast": true
    }
  ]
}
```

**フィールド定義**

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `events` | array | 全イベント一覧（`yearMonth` 昇順）。イベントが存在しない場合は空配列 |
| `events[].type` | string | イベント種別（下記参照） |
| `events[].lane` | string | タイムラインのレーン。`ACHIEVEMENT`（上段）または `ACTIVITY`（下段） |
| `events[].name` | string | 表示名（資格名・セミナー名・スキル名・AD名） |
| `events[].yearMonth` | string | 年月（`YYYY-MM-DD` 形式。月初日） |
| `events[].isPast` | boolean | `yearMonth` がサーバー処理日時点で過去かどうか（`yearMonth < 当月1日` なら `true`） |

**イベント種別（type）一覧**

| type | 発生源 | lane |
|------|--------|------|
| `QUALIFICATION` | `qualification_details.acquired_year_month`（NULL以外） | `ACHIEVEMENT` |
| `AD_SEMINAR` | `seminar_details.attended_year_month`（`ad_seminar_id IS NOT NULL`） | `ACTIVITY` |
| `FREE_SEMINAR` | `seminar_details.attended_year_month`（`seminar_name IS NOT NULL`） | `ACTIVITY` |
| `GOAL_QUALIFICATION` | `inventory_goals.target_period`（`goal_category = 'QUALIFICATION'`） | `ACHIEVEMENT` |
| `GOAL_IT_SKILL` | `inventory_goals.target_period`（`goal_category = 'IT_SKILL'`） | `ACTIVITY` |
| `GOAL_AD` | `inventory_goals.target_period`（`goal_category = 'AD'`） | `ACTIVITY` |

**データ取得ルール**

- **実績（QUALIFICATION・AD_SEMINAR・FREE_SEMINAR）**：ログインユーザーの全年度棚卸から `yearMonth IS NOT NULL` のものを全件取得
- **目標（GOAL_*）**：ログインユーザーの**最新年度**の棚卸（`inventories` の `fiscal_year_id` が最大のもの）の `inventory_goals` を取得。棚卸のステータスは問わない
- `GOAL_QUALIFICATION` の `name`：`qualification_id` がある場合は `qualifications.name`、`custom_name` がある場合はその値
- `GOAL_IT_SKILL` の `name`：`it_skill_id` がある場合は `it_skills.name`、`custom_name` がある場合はその値
- `GOAL_AD` の `name`：`ad_seminars.name`

---

## 共通仕様

### 分類の親子関係の解決

バックエンドは `it_skill_categories` の親子関係（自己参照）を辿り、各スキルの分類1・分類2を解決してから集計する。

| レベル | 取得方法 |
|--------|---------|
| 分類1（level=1） | leaf category から `parent_id` を `level=1` になるまで遡った祖先 |
| 分類2（level=2） | 同上で `level=2` の祖先。存在しない場合（leaf が level=1 直下）は `null` |

### 削除済みマスタの扱い

| データ | 集計への含め方 |
|--------|-------------|
| 削除済みスキルマスタ（`it_skills.is_active = false`）の棚卸明細 | **含める**（過去の棚卸実績を正確に反映するため） |
| 削除済み分類（`it_skill_categories.is_active = false`）配下のスキル | **含める**（スコアは集計するが、分類名は `(削除済み分類)` で表示） |
| カスタムスキル（`it_skill_id = NULL`） | **除外**（分類1が特定できないため、全グラフから除外） |

### エラーレスポンス

| ケース | HTTP | code |
|--------|:----:|------|
| JWT なし / 期限切れ | 401 | `UNAUTHORIZED` |
| 初回パスワード未変更 | 403 | `FORBIDDEN` |

> データが存在しない場合（棚卸ゼロ・採点ゼロ等）は **200** を返し、空配列または `false` フラグで表現する。404 は返さない。

### 将来拡張（Phase 2）

現在は全エンドポイントでログインユーザー自身のデータのみ返す。  
チーム集計・全社集計への対応（要件定義 TBD #3）が確定した際は、クエリパラメータ `scope` を追加して対応する想定。

```
GET /api/charts/radar?scope=team     # TL / ADMIN: チーム平均
GET /api/charts/radar?scope=company  # ADMIN: 全社平均
```

パラメータ省略時は `scope=self`（デフォルト）として現行仕様と互換性を保つ。

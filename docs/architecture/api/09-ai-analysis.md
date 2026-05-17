# AIキャリア分析 API

**対象画面**: SCR-002（ダッシュボード）、SCR-006（棚卸・目標照会）、MemberDetailPage（TL/ADMINのメンバー詳細）

---

## エンドポイント一覧

| メソッド | パス | 概要 | 権限 |
|---------|------|------|------|
| GET | /api/users/me/ai-analyses | 自分の AI 分析結果一覧（全年度） | 全員 |
| GET | /api/users/{userId}/ai-analyses | 指定ユーザーの AI 分析結果一覧 | TL（担当のみ） / ADMIN |

> 分析は非同期バックグラウンド処理で実行される（ユーザー操作による直接起動は不可）。  
> LLM はデータ取得 API では呼び出さない。DB に保存済みの結果のみを返す。

---

## 認可ルール

```
GET /api/users/me/ai-analyses:
  → JWT 認証済みであれば全ロールアクセス可

GET /api/users/{userId}/ai-analyses:
  1. ADMIN → 常にアクセス可
  2. TL → 対象ユーザーの tl_user_id = 自分の ID の場合のみアクセス可（それ以外 403）
  3. GENERAL → 403
```

---

## GET /api/users/me/ai-analyses

自分の AI 分析結果を全年度分返す。ダッシュボードと照会画面（自分）で使用。

**権限**: 全員

**Response 200**

```json
[
  {
    "id": 1,
    "fiscalYear": { "id": 1, "name": "FY2025" },
    "status": "COMPLETED",
    "analysisResult": {
      "summary": "今年度はJavaとAWSの両面でバランス良く成長されており...",
      "strengths": [
        "実務経験に裏打ちされたJava/Spring Bootの高い習熟度",
        "資格取得への継続的な取り組み"
      ],
      "growth_areas": [
        "クラウドアーキテクチャ設計のさらなる深化",
        "チームへの技術共有・メンタリングの機会を増やすこと"
      ],
      "expectation_fit": "TLから期待されているリードエンジニアとしての役割に対し...",
      "recommended_actions": [
        "もし既に取り組んでいれば、AWSの設計判断をドキュメントに残す習慣を少し加えてみる",
        "面談でTLに、チーム内でのコードレビュー改善について相談してみる"
      ]
    },
    "createdAt": "2026-04-25T10:00:00Z",
    "updatedAt": "2026-04-25T10:05:00Z"
  },
  {
    "id": 2,
    "fiscalYear": { "id": 2, "name": "FY2024" },
    "status": "COMPLETED",
    "analysisResult": { "summary": "...", "strengths": [], "growth_areas": [], "expectation_fit": "...", "recommended_actions": [] },
    "createdAt": "2025-04-20T09:00:00Z",
    "updatedAt": "2025-04-20T09:10:00Z"
  }
]
```

**status が COMPLETED 以外の場合（analysisResult は null）**

```json
[
  {
    "id": 3,
    "fiscalYear": { "id": 1, "name": "FY2025" },
    "status": "PENDING",
    "analysisResult": null,
    "createdAt": "2026-04-25T10:00:00Z",
    "updatedAt": "2026-04-25T10:00:00Z"
  }
]
```

> 年度の降順（最新年度が先頭）で返す。  
> `status` の値: `PENDING`（分析待ち）/ `PROCESSING`（分析中）/ `COMPLETED`（完了）/ `FAILED`（エラー）  
> 棚卸が未提出（COMPLETED 遷移前）の年度はレコード自体が存在しないため、一覧に含まれない。

---

## GET /api/users/{userId}/ai-analyses

TL / ADMIN が担当メンバーの AI 分析結果を参照する。MemberDetailPage の「AI分析」タブで使用。

**権限**: TL（担当チームのみ）/ ADMIN

**Path Parameters**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `userId` | int | ユーザーの内部 ID（`users.id`） |

**Response 200**: `GET /api/users/me/ai-analyses` と同形式

**Response 403**

```json
{ "code": "FORBIDDEN", "message": "このユーザーへのアクセス権限がありません" }
```

---

## 分析結果 JSON 構造（analysisResult）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `summary` | string | 全体の総括とモチベーションを高める応援メッセージ |
| `strengths` | array of string | スキルや目標から見える客観的な強み・成長点 |
| `growth_areas` | array of string | 期待値や今後のキャリアに向けたさらなる伸び代・注力領域 |
| `expectation_fit` | string | 会社・TLからの期待と現在のスキル・目標のマッチング状況・アドバイス |
| `recommended_actions` | array of string | 明日から活用できる具体的なネクストステップ（行動の工夫・相談のきっかけ） |

---

## バックエンド実装メモ

### パッケージ構成

```
com.skilize.ai/
├── presentation/
│   ├── AiAnalysisController.java      ← GET /me/ai-analyses, GET /{userId}/ai-analyses
│   └── AiAnalysisResponse.java        ← record
├── application/
│   ├── AiAnalysisService.java         ← 取得ロジック / Python 非同期呼び出し
│   └── InventoryCompletedEventListener.java  ← @EventListener（COMPLETED 遷移を検知）
└── domain/
    ├── AiCareerAnalysis.java           ← Entity（@Column(columnDefinition = "jsonb")）
    ├── AiAnalysisStatus.java           ← Enum
    └── AiCareerAnalysisRepository.java
```

### フロントエンド構成

```
features/inventory/
├── api/inventoryApi.ts    ← getMyAiAnalyses() を追加
└── types/index.ts         ← AiAnalysis / AiAnalysisResult 型を追加

features/team/
└── api/userApi.ts         ← getMemberAiAnalyses(userId) を追加
```

> AI 分析の表示 UI はダッシュボード（`DashboardPage.tsx`）・棚卸照会（`InventoryHistoryPage.tsx`）・メンバー詳細（`MemberDetailPage.tsx`）にそれぞれ直接実装する。

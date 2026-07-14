# テスト仕様書 インデックス

## 構成

テスト仕様書はコンテナ単位のフォルダに分割し、その中に機能単位のファイルを配置しています。

```
docs/testing/
├── backend/          ← Spring Boot / JUnit 5
│   ├── auth.md            ← 認証（JWT・ログイン・パスワード管理）
│   ├── inventory.md       ← 棚卸（CRUD・明細・提出・目標・前年度比較・振り返り・アクセス制御）
│   ├── ai-chat.md         ← AI チャット（有効/無効制御・チャット API）
│   ├── ai-analysis.md     ← AI キャリア分析（分析結果参照・upsert トリガー）
│   ├── master.md          ← マスタ管理（スキルレベル・ITスキル・資格・ADセミナーとその分類・カスタム昇格）
│   ├── master-excel.md    ← マスタ Excel 出力・取込
│   ├── chart.md           ← グラフ・チャート（レーダー・成長推移・ヒートマップ・タイムライン集計）
│   ├── dashboard.md       ← ダッシュボード（今年度棚卸サマリー）
│   ├── expectation.md     ← 期待コメント（TL期待・会社期待のアクセス制御・upsert）
│   ├── fiscal-year.md     ← 年度管理（年度・年度設定のCRUD）
│   ├── interview.md       ← 面談メモ（TL/ADMIN限定・全件洗い替え・前年度参照）
│   ├── user.md            ← ユーザー管理（CRUD・パスワードリセット・チーム照会）
│   └── report.md          ← 帳票・レポート（棚卸PDF帳票生成・アクセス制御）
├── frontend/         ← Vitest + React Testing Library
│   ├── auth.md       ← 認証（ログイン画面）
│   ├── inventory.md  ← 棚卸（棚卸照会・フィルタリング）
│   └── ai-support.md ← AI サポート（チャットウィジェット）
└── ai/               ← Python FastAPI / pytest
    └── chat-service.md ← チャットサービス（プロンプト・LLM 呼び出し）
```

## ファイル一覧

| ファイル | テスト対象 | テストケース数 |
|---|---|---|
| [backend/auth.md](backend/auth.md) | JwtUtil / AuthService / AuthController | 24 件 |
| [backend/inventory.md](backend/inventory.md) | InventoryService（getComparison / 全体）/ InventoryController | 56 件 |
| [backend/ai-chat.md](backend/ai-chat.md) | AiChatService / AiChatController | 8 件 |
| [backend/ai-analysis.md](backend/ai-analysis.md) | AiAnalysisService / AiAnalysisController | 10 件 |
| [backend/master.md](backend/master.md) | MasterService / MasterController | 53 件 |
| [backend/master-excel.md](backend/master-excel.md) | ItSkillExcelImporter / MasterExcelController | 15 件 |
| [backend/chart.md](backend/chart.md) | ChartService / ChartController | 21 件 |
| [backend/dashboard.md](backend/dashboard.md) | DashboardService / DashboardController | 7 件 |
| [backend/expectation.md](backend/expectation.md) | ExpectationService / ExpectationController | 17 件 |
| [backend/fiscal-year.md](backend/fiscal-year.md) | FiscalYearService / FiscalYearController | 19 件 |
| [backend/interview.md](backend/interview.md) | InterviewService / InterviewController | 16 件 |
| [backend/user.md](backend/user.md) | UserService / UserController | 21 件 |
| [backend/report.md](backend/report.md) | ReportService / ReportController | 6 件 |
| [frontend/auth.md](frontend/auth.md) | LoginPage | 10 件 |
| [frontend/inventory.md](frontend/inventory.md) | InventoryHistoryPage | 9 件 |
| [frontend/ai-support.md](frontend/ai-support.md) | AiSupportWidget | 12 件 |
| [ai/chat-service.md](ai/chat-service.md) | chat_service.py | 14 件 |

**合計**: 21 テストファイル・約 318 テストケース

## テスト ID 体系

| プレフィックス | 対象 |
|---|---|
| `BE-JWT-` | バックエンド / JwtUtil |
| `BE-AS-` | バックエンド / AuthService |
| `BE-AC-` | バックエンド / AuthController |
| `BE-ISC-` | バックエンド / InventoryService（Comparison）|
| `BE-IS-` | バックエンド / InventoryService（Comparison以外の全体） |
| `BE-IC-` | バックエンド / InventoryController |
| `BE-ACHS-` | バックエンド / AiChatService |
| `BE-ACHC-` | バックエンド / AiChatController |
| `BE-AAS-` | バックエンド / AiAnalysisService |
| `BE-AAC-` | バックエンド / AiAnalysisController |
| `BE-MEI-` | バックエンド / ItSkillExcelImporter |
| `BE-MEC-` | バックエンド / MasterExcelController |
| `BE-MS-` | バックエンド / MasterService |
| `BE-MC-` | バックエンド / MasterController |
| `BE-CHS-` | バックエンド / ChartService |
| `BE-CHC-` | バックエンド / ChartController |
| `BE-DS-` | バックエンド / DashboardService |
| `BE-DC-` | バックエンド / DashboardController |
| `BE-ES-` | バックエンド / ExpectationService |
| `BE-EC-` | バックエンド / ExpectationController |
| `BE-FYS-` | バックエンド / FiscalYearService |
| `BE-FYC-` | バックエンド / FiscalYearController |
| `BE-IVS-` | バックエンド / InterviewService |
| `BE-IVC-` | バックエンド / InterviewController |
| `BE-US-` | バックエンド / UserService |
| `BE-UC-` | バックエンド / UserController |
| `BE-RS-` | バックエンド / ReportService |
| `BE-RC-` | バックエンド / ReportController |
| `FE-LP-` | フロントエンド / LoginPage |
| `FE-IHP-` | フロントエンド / InventoryHistoryPage |
| `FE-ASW-` | フロントエンド / AiSupportWidget |
| `AI-CS-` | Python AI / chat_service |

> **テストの実行方法** → [`running-tests.md`](running-tests.md)  
> **更新タイミング** → テストコードを追加・変更・削除した際は各仕様書ファイルを同時に更新すること（`CLAUDE.md` 開発ルール参照）

# テスト仕様書 インデックス

## 構成

テスト仕様書はコンテナ単位のフォルダに分割し、その中に機能単位のファイルを配置しています。

```
docs/testing/
├── backend/          ← Spring Boot / JUnit 5
│   ├── auth.md       ← 認証（JWT・ログイン・パスワード管理）
│   ├── inventory.md  ← 棚卸（前年度比較・差分計算）
│   ├── ai-chat.md    ← AI チャット（有効/無効制御・チャット API）
│   └── master-excel.md ← マスタ Excel 出力・取込
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
| [backend/inventory.md](backend/inventory.md) | InventoryService#getComparison | 5 件 |
| [backend/ai-chat.md](backend/ai-chat.md) | AiChatService / AiChatController | 8 件 |
| [backend/master-excel.md](backend/master-excel.md) | ItSkillExcelImporter / MasterExcelController | 13 件 |
| [frontend/auth.md](frontend/auth.md) | LoginPage | 10 件 |
| [frontend/inventory.md](frontend/inventory.md) | InventoryHistoryPage | 9 件 |
| [frontend/ai-support.md](frontend/ai-support.md) | AiSupportWidget | 12 件 |
| [ai/chat-service.md](ai/chat-service.md) | chat_service.py | 14 件 |

**合計**: 11 テストファイル・約 95 テストケース

## テスト ID 体系

| プレフィックス | 対象 |
|---|---|
| `BE-JWT-` | バックエンド / JwtUtil |
| `BE-AS-` | バックエンド / AuthService |
| `BE-AC-` | バックエンド / AuthController |
| `BE-ISC-` | バックエンド / InventoryService（Comparison）|
| `BE-ACHS-` | バックエンド / AiChatService |
| `BE-ACHC-` | バックエンド / AiChatController |
| `BE-MEI-` | バックエンド / ItSkillExcelImporter |
| `BE-MEC-` | バックエンド / MasterExcelController |
| `FE-LP-` | フロントエンド / LoginPage |
| `FE-IHP-` | フロントエンド / InventoryHistoryPage |
| `FE-ASW-` | フロントエンド / AiSupportWidget |
| `AI-CS-` | Python AI / chat_service |

> **テストの実行方法** → [`running-tests.md`](running-tests.md)  
> **更新タイミング** → テストコードを追加・変更・削除した際は各仕様書ファイルを同時に更新すること（`CLAUDE.md` 開発ルール参照）

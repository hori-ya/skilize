# Skilize

> *Visualizing skills to empower employee growth and potential.*

エンジニアの学習履歴と成長の軌跡を可視化し、次なるキャリア形成へと導く社内スキル管理プラットフォームです。

ITスキル・資格・セミナー受講実績を年次で蓄積し、レーダーチャートやタイムラインなどの多彩なグラフを通じて、自身のスキルバランスや成長推移を直感的に把握できます。また、前年度との差分分析やチームリーダーとの目標設定、面談記録までを一気通貫でサポート。さらに、入力されたデータをもとにLLM（大規模言語モデル）が心理的安全性に配慮したキャリア分析を自動実行し、個人のスキル傾向や強み、次の具体的なアクションを盛り込んだパーソナルレポートを提示します。

**主な特徴**

- **スキルの一元管理** — ITスキル・資格・セミナーをまとめて記録。カスタムスキルの登録にも対応
- **ビジュアルダッシュボード** — レーダーチャート・成長推移グラフ・ヒートマップで自分の強みを直感的に把握
- **チームレビュー連携** — TL による棚卸レビュー・面談メモ・目標振り返りをワンプラットフォームで完結
- **AI キャリア分析** — 棚卸完了をトリガーに AI が自動分析。スキルの傾向・成長余地・具体的なネクストステップを提案

---

## Tech Stack

| レイヤー | 技術 |
|---|---|
| Frontend | React 19 + TypeScript（Vite） |
| Backend | Spring Boot 4.0.6 / Java 21 |
| AI | Python 3.12 / FastAPI / LangChain |
| DB | PostgreSQL 16.4 |
| Infra | Docker Compose + nginx |

---

## Development

### 起動

```bash
# 初回・コード変更後（ビルドあり）
docker compose up --build

# 2回目以降
docker compose up

# バックグラウンド起動
docker compose up -d
```

### 停止

```bash
# 停止
docker compose down

# 停止 + DB ボリューム削除（DB を初期化したい場合）
docker compose down -v
```

### 状態・ログ確認

```bash
# コンテナ一覧と状態確認
docker compose ps

# ログ（リアルタイム）
docker compose logs -f
docker compose logs -f backend   # バックエンドのみ
docker compose logs -f frontend  # フロントエンドのみ
```

### 特定サービスだけ再ビルド

```bash
docker compose up --build backend
docker compose up --build frontend
```

### アクセス先

| URL | 説明 |
|---|---|
| http://localhost:8081 | アプリ（nginx 経由） |
| http://localhost:8080/api/health | バックエンド ヘルスチェック |

### テストユーザー

| ユーザーID | パスワード | ロール |
|---|---|---|
| `admin` | `admin` | ADMIN |
| `tl01` | `tl01` | TL |
| `user01` | `user01` | GENERAL |
| `user02` | `user02` | GENERAL |

---

## Docs

| ドキュメント | パス |
|---|---|
| 開発環境構築手順書 | [docs/setup/development-setup.md](docs/setup/development-setup.md) |
| 本番環境構築手順書 | [docs/setup/production-setup.md](docs/setup/production-setup.md) |
| システム構成・アーキテクチャ | [docs/architecture/overview.md](docs/architecture/overview.md) |
| 機能要件 | [docs/requirements/functional/functional-requirements.md](docs/requirements/functional/functional-requirements.md) |

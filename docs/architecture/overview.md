# システム全体構成

**バージョン**: 1.0.0  
**作成日**: 2026-05-09

---

## 1. システム概要

社員が年次でスキルの棚卸を行うWebアプリケーション。  
フロントエンド（React）とバックエンド（Spring Boot）をDockerコンテナで構成し、AWS EC2上で稼働する。  
データベースはAWS RDS（PostgreSQL）を使用する。

---

## 2. 構成図

```
[ ユーザー（ブラウザ）]
        │ HTTPS
        ▼
[ EC2インスタンス ]
  ┌─────────────────────────────────┐
  │  Docker Compose                 │
  │                                 │
  │  ┌───────────┐  ┌────────────┐  │
  │  │  Nginx    │  │  Frontend  │  │
  │  │ (reverse  │─▶│  (React)   │  │
  │  │  proxy)   │  │  :3000     │  │
  │  └─────┬─────┘  └────────────┘  │
  │        │ /api/*                 │
  │        ▼                        │
  │  ┌─────────────┐                │
  │  │  Backend    │                │
  │  │ (Spring     │                │
  │  │  Boot)      │                │
  │  │  :8080      │                │
  │  └──────┬──────┘                │
  └─────────┼───────────────────────┘
            │
            ▼
  [ AWS RDS ]
  PostgreSQL 16.4
```

---

## 3. コンポーネント説明

| コンポーネント | 技術 | 役割 |
|--------------|------|------|
| Nginx | nginx:alpine | リバースプロキシ。`/api/*` をバックエンド、それ以外をフロントエンドへルーティング |
| Frontend | React | SPA。画面描画・ユーザー操作の受付 |
| Backend | Spring Boot 3 / Java 21 | REST API提供。ビジネスロジック・認証・認可 |
| Database | PostgreSQL 16.4（AWS RDS） | データ永続化 |

---

## 4. 認証フロー

```
Browser ──POST /api/auth/login──▶ Backend
                                    │ 認証成功
                                    ▼
Browser ◀──JWT（アクセストークン）── Backend
    │
    │ 以降のAPIリクエストに Authorization: Bearer <token> を付与
    ▼
Backend（Spring Security で検証・認可）
```

---

## 5. 関連ドキュメント

| ドキュメント | パス |
|------------|------|
| 機能要件 | [docs/requirements/functional/functional-requirements.md](../requirements/functional/functional-requirements.md) |
| 非機能要件 | [docs/requirements/non-functional/non-functional-requirements.md](../requirements/non-functional/non-functional-requirements.md) |
| データモデル（概念） | [docs/architecture/database/data-model.md](./database/data-model.md) |
| ER図 | [docs/architecture/database/er-diagram.md](./database/er-diagram.md) |
| 技術スタック詳細 | [.claude/context/tech-stack.md](../../.claude/context/tech-stack.md) |

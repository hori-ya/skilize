# フォルダ構成

myapp/
├── .claude/
│   └── context/
│       ├── project.md          # プロジェクト概要・目的・スコープ
│       ├── tech-stack.md       # 技術スタック定義（今回の内容をそのまま記載）
│       ├── conventions.md      # 命名規則・コーディング規約
│       └── commands.md         # よく使うコマンドリスト
│
├── .cursor/
│   └── rules/
│       ├── general.mdc         # 汎用ルール
│       ├── java.mdc            # Java / Spring Boot 固有ルール
│       ├── react.mdc           # React / Frontend 固有ルール
│       └── test.mdc            # テストルール（JUnit / Vitest 等）
│
├── docs/
│   ├── requirements/
│   │   ├── functional/         # 機能要件
│   │   └── non-functional/     # 非機能要件（性能・セキュリティ等）
│   ├── architecture/
│   │   ├── overview.md         # 全体構成図（EC2 / Docker / RDS の関係等）
│   │   ├── database/           # DB設計（ER図・テーブル定義）
│   │   ├── api/                # API設計（エンドポイント・リクエスト/レスポンス定義）
│   │   └── security/           # 認証・認可設計（Spring Security の方針等）
│   └── decisions/              # ADR（アーキテクチャ決定記録）
│
├── prompts/                    # 定型プロンプトテンプレート
│
├── docker/                     # Docker関連設定ファイルをまとめる場所
│   ├── backend/
│   │   └── Dockerfile
│   ├── frontend/
│   │   └── Dockerfile
│   └── nginx/                  # リバースプロキシ設定（任意）
│       └── nginx.conf
│
├── scripts/                    # 運用・開発補助スクリプト
│   ├── db/
│   │   └── init.sql            # DB初期化SQL（ローカル開発用）
│   └── deploy/                 # デプロイスクリプト（EC2向け）
│
├── docker-compose.yml          # ローカル開発環境定義
├── docker-compose.prod.yml     # 本番環境定義（EC2用）
├── .env.example                # 環境変数テンプレート（.envは.gitignoreへ）
├── .gitignore
└── CLAUDE.md

# 技術スタック

Frontend
- React

Backend
- Spring Boot

Auth
- Spring Security

DB
- PostgreSQL(RDS)

Hosting
- EC2

Infra
- Docker Compose

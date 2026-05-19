# 開発環境構築手順書

## 目次

1. [必要なツールのインストール](#1-必要なツールのインストール)
   - [Git](#11-git)
   - [Docker Desktop](#12-docker-desktop)
   - [JDK 21（IntelliJ デバッグ実行時のみ）](#13-jdk-21intellij-デバッグ実行時のみ)
   - [Node.js（IntelliJ デバッグ実行時のみ）](#14-nodejsintelliJ-デバッグ実行時のみ)
2. [リポジトリのクローン](#2-リポジトリのクローン)
3. [環境変数の設定](#3-環境変数の設定)
4. [起動方法](#4-起動方法)
   - [A. Docker 一括起動（推奨）](#a-docker-一括起動推奨)
   - [B. IntelliJ + ローカル起動（デバッグ向け）](#b-intellij--ローカル起動デバッグ向け)
5. [動作確認](#5-動作確認)
6. [テストユーザー](#6-テストユーザー)
7. [よく使うコマンド](#7-よく使うコマンド)
8. [トラブルシューティング](#8-トラブルシューティング)

---

## 1. 必要なツールのインストール

### 1.1 Git

ソースコードを管理するツールです。

#### インストール

**Windows**

1. [git-scm.com](https://git-scm.com/download/win) からインストーラーをダウンロードして実行します
2. インストール中の設定は基本的にデフォルトのままで問題ありません
3. 「Git Bash Here」を含む設定はそのまま有効にしておくと便利です

**Mac**

```bash
# Homebrew が入っている場合（推奨）
brew install git

# Homebrew がない場合は https://brew.sh/ からインストールできます
```

**確認**

```bash
git --version
# git version 2.xx.x と表示されれば OK
```

#### 初期設定

Git を初めて使う場合は、コミット時に使う名前とメールアドレスを設定します。  
（コミット履歴に記録されるため、氏名または GitHub アカウント名を入れるのが一般的です）

```bash
git config --global user.name "あなたの名前"
git config --global user.email "あなたのメールアドレス"
```

---

### 1.2 Docker Desktop

アプリケーション全体をコンテナで動かすためのツールです。  
DB・バックエンド・フロントエンドをまとめて一コマンドで起動できます。

#### インストール

**Windows**

1. [docs.docker.com](https://docs.docker.com/desktop/install/windows-install/) からインストーラー（`Docker Desktop Installer.exe`）をダウンロードして実行します
2. インストール完了後、PC を再起動します
3. Docker Desktop を起動し、初回セットアップ画面を完了させます
4. タスクトレイに Docker のクジラアイコンが表示されれば起動中です

> **Windows の注意**: WSL 2（Windows Subsystem for Linux）が必要です。インストーラーの指示に従ってください。  
> WSL 2 が有効になっていない場合、PowerShell を管理者権限で開き `wsl --install` を実行してから再起動してください。

**Mac**

```bash
# Apple Silicon（M1/M2/M3）の場合
brew install --cask docker

# または Docker Desktop のサイトから .dmg ファイルをダウンロードしてインストールできます
```

インストール後、アプリケーションフォルダから Docker Desktop を起動します。  
メニューバーにクジラアイコンが表示されれば起動中です。

**確認**

```bash
docker --version
# Docker version 27.x.x と表示されれば OK

docker compose version
# Docker Compose version v2.x.x と表示されれば OK
```

---

### 1.3 JDK 21（IntelliJ デバッグ実行時のみ）

バックエンド（Spring Boot）をローカルで直接実行する場合に必要です。  
Docker 一括起動（方法 A）のみ使う場合はインストール不要です。

**Windows / Mac 共通**

1. [Adoptium](https://adoptium.net/) にアクセスします
2. 「Other platforms and versions」から **Version: 21**、JVM: Temurin を選択してダウンロードします
3. インストーラーに従ってインストールします

**Mac（Homebrew）**

```bash
brew install --cask temurin@21
```

**確認**

```bash
java --version
# openjdk 21.x.x と表示されれば OK
```

---

### 1.4 Node.js（IntelliJ デバッグ実行時のみ）

フロントエンド（React）をローカルで直接実行する場合に必要です。  
Docker 一括起動（方法 A）のみ使う場合はインストール不要です。

**Windows**

1. [nodejs.org](https://nodejs.org/en) にアクセスし、**LTS 版**（推奨版）をダウンロードしてインストールします

**Mac**

```bash
brew install node
```

**確認**

```bash
node --version
# v20.x.x 以上と表示されれば OK

npm --version
# 10.x.x 以上と表示されれば OK
```

---

## 2. リポジトリのクローン

リポジトリとは、プロジェクトのソースコード一式が保存されている場所です。  
以下のコマンドで自分の PC にコピーします。

```bash
git clone <リポジトリURL>
cd skilize
```

> `<リポジトリURL>` は GitHub/GitLab のリポジトリページにある「Code」ボタンから確認できます。  
> HTTPS URL（`https://github.com/...`）を使う場合はそのままコピーしてください。  
> SSH URL（`git@github.com:...`）を使う場合は、事前に SSH キーの設定が必要です（GitHub 等のドキュメントを参照）。

---

## 3. 環境変数の設定

環境変数とは、アプリケーションが動作するために必要な設定値（パスワードや API キーなど）を外部から渡す仕組みです。  
これらをソースコードに直接書くとセキュリティ上の問題になるため、`.env` ファイルで管理します。

### .env ファイルの作成

`.env.example` は設定のひな形です。これをコピーして自分用の `.env` を作ります。

```bash
cp .env.example .env
```

> **注意**: `.env` は Git 管理外（`.gitignore` 登録済み）です。誤ってコミット・プッシュしないでください。

### 編集が必要な項目

テキストエディタで `.env` を開き、以下を確認・修正します。

```env
# ─────────────────────────────────────────────────────
# Docker Compose の設定
# ─────────────────────────────────────────────────────

# 使用する Compose ファイルのパス（ローカル開発用）
COMPOSE_FILE=infra/compose/docker-compose.yml

# ─────────────────────────────────────────────────────
# データベース接続情報（ローカルは変更不要）
# ─────────────────────────────────────────────────────
DB_NAME=skilize
DB_USER=skilize
DB_PASSWORD=password

# ─────────────────────────────────────────────────────
# JWT（認証トークン）の設定
# ─────────────────────────────────────────────────────
# JWT_SECRET: ログイン情報の暗号化に使う鍵。推測されにくい長い文字列に変更する
JWT_SECRET=change-this-to-a-random-256-bit-secret-key-before-use
JWT_EXPIRATION_MS=86400000   # トークンの有効期限（ミリ秒）。86400000 = 24時間

# ─────────────────────────────────────────────────────
# CORS（フロントエンドのアクセス許可設定）
# ─────────────────────────────────────────────────────
# ローカルでアクセスする URL を指定する（変更不要）
FRONTEND_ORIGIN=http://localhost:8081,http://localhost:5173

# ─────────────────────────────────────────────────────
# Spring Boot のプロファイル（変更不要）
# ─────────────────────────────────────────────────────
SPRING_PROFILES_ACTIVE=local

# ─────────────────────────────────────────────────────
# AI 機能の設定
# ─────────────────────────────────────────────────────
# AI_ENABLED: false にすると LLM への API 通信が行われなくなる（コスト不要）
AI_ENABLED=true

# OpenAI を使う場合（デフォルト）
LLM_PROVIDER=openai
LLM_MODEL=gpt-4o
OPENAI_API_KEY=sk-...           # OpenAI の API キーを入力する

# Anthropic（Claude）に切り替える場合は以下を使用
# LLM_PROVIDER=anthropic
# LLM_MODEL=claude-opus-4-7
# ANTHROPIC_API_KEY=sk-ant-...

# AI サービスとの内部通信に使う認証キー（ランダムな文字列に変更する）
AI_SECRET_KEY=change-this-to-a-random-secret

# AI サービスの URL（変更不要）
AI_SERVICE_URL=http://ai:8000
```

> **AI 機能を使わない場合**: `AI_ENABLED=false` に設定すれば API キーがなくてもアプリは動作します。

---

## 4. 起動方法

### A. Docker 一括起動（推奨）

DB・バックエンド・フロントエンド・AI モジュール・nginx の全サービスを一度に起動します。  
JDK や Node.js のインストールが不要で、コマンド 1 つで完結するためこちらを推奨します。

#### 初回起動

```bash
docker compose up --build
```

初回はイメージのビルド（必要なファイルをダウンロード・構築する処理）があるため、5〜10 分程度かかります。  
`Starting skilize-nginx-1` のような表示が出始めれば起動完了に近づいています。

> **うまく起動しない場合**: Docker Desktop が起動していることを確認してください（タスクトレイまたはメニューバーのクジラアイコン）。

#### 2 回目以降

```bash
docker compose up
```

ビルドが不要なため数十秒で起動します。

#### バックグラウンド起動（ターミナルを占有したくない場合）

```bash
docker compose up -d
```

ログを確認する場合は別途以下を実行します:

```bash
docker compose logs -f           # 全サービス
docker compose logs -f backend   # バックエンドのみ
docker compose logs -f frontend  # フロントエンドのみ
```

#### 停止

```bash
docker compose down
```

---

### B. IntelliJ + ローカル起動（デバッグ向け）

バックエンドを IntelliJ でブレークポイントを使ってデバッグしたい場合に使います。  
JDK 21・Node.js・IntelliJ IDEA が必要です（[1. 必要なツールのインストール](#1-必要なツールのインストール) 参照）。

#### ステップ 1: DB コンテナのみ起動

```bash
docker compose up db
```

PostgreSQL が `localhost:5433` で起動します。  
（ポート 5433 を使うのは、ローカルに別の PostgreSQL が入っている場合の競合を避けるためです）

#### ステップ 2: IntelliJ の実行構成を設定

1. IntelliJ IDEA を開き、`apps/backend/build.gradle` を選択してプロジェクトとしてインポートします
2. 上部メニューの「Run」→「Edit Configurations...」を開きます
3. 「+」ボタンから「Application」を追加します
4. 以下を設定します:
   - **Name**: `SkilizeBackend`（任意）
   - **Main class**: `com.skilize.BackendApplication`
   - **Module**: `backend.main`
5. 「Modify options」→「Environment variables」を追加し、以下を入力します:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/skilize
SPRING_DATASOURCE_USERNAME=skilize
SPRING_DATASOURCE_PASSWORD=password
JWT_SECRET=change-this-to-a-random-256-bit-secret-key-before-use
AI_ENABLED=false
AI_SECRET_KEY=local-secret
SPRING_PROFILES_ACTIVE=local
```

> `SPRING_PROFILES_ACTIVE=local` にすることで `application-local.yml` が有効になり、Flyway（DB マイグレーションツール）が無効化されます。  
> ローカル環境では `scripts/db/init.sql` でスキーマを初期化しているため Flyway は不要です。

6. 「OK」で保存し、「▶ Run」または「🐛 Debug」で起動します

#### ステップ 3: フロントエンドをローカル起動

```bash
cd apps/frontend
npm install      # 初回のみ。依存パッケージをダウンロードする
npm run dev      # 開発サーバーを起動する
```

`http://localhost:5173` でアクセスできます。  
Vite の設定により、`/api` へのリクエストは自動的に `localhost:8080`（バックエンド）へ転送されます。

---

## 5. 動作確認

### アクセス先

| 方法 | URL | 説明 |
|---|---|---|
| Docker 一括起動（A） | http://localhost:8081 | nginx 経由（本番に近い構成） |
| IntelliJ + ローカル（B） | http://localhost:5173 | Vite 直接（コード変更が即反映される） |
| バックエンド API 直接 | http://localhost:8080/api/health | `{"status":"ok"}` が返れば正常 |

### 確認手順

1. ブラウザで上記 URL を開く
2. ログイン画面が表示されることを確認する
3. 下記のテストユーザーでログインして各機能を確認する

---

## 6. テストユーザー

`scripts/db/init.sql` 適用後（Docker 起動時に自動実行）に以下のユーザーが使えます。

| ユーザーID | パスワード | ロール | 確認できる機能 |
|---|---|---|---|
| `admin` | `admin` | ADMIN | マスタ管理・ユーザー管理・年度設定など全機能 |
| `tl01` | `tl01` | TL | チーム照会・面談記録・マスタ参照 |
| `user01` | `user01` | GENERAL | 自分の棚卸入力・ダッシュボード・グラフ閲覧 |
| `user02` | `user02` | GENERAL | 自分の棚卸入力・ダッシュボード・グラフ閲覧 |

---

## 7. よく使うコマンド

### Docker

```bash
# 全サービス起動（ビルドあり）
docker compose up --build

# バックグラウンド起動
docker compose up -d

# 停止
docker compose down

# 停止 + DB ボリューム削除（DB を完全初期化したい場合）
docker compose down -v

# 特定サービスだけ再ビルド・起動
docker compose up --build backend

# コンテナの起動状態を確認
docker compose ps

# ログ確認（Ctrl+C で抜ける）
docker compose logs -f backend
```

### DB の再初期化（ローカル）

DB スキーマやデータを最初からやり直したい場合:

```bash
docker compose down -v   # ボリューム（DB のデータ）ごと削除
docker compose up db     # DB コンテナを起動（init.sql が自動実行される）
```

### フロントエンド（B 方法・ローカル直接起動時）

```bash
cd apps/frontend
npm install        # 依存パッケージのインストール（初回または package.json 変更後）
npm run dev        # 開発サーバー起動
npm run build      # 本番用ビルド（成果物が dist/ に生成される）
npm run lint       # ESLint によるコードチェック
```

---

## 8. トラブルシューティング

### Docker Desktop が起動していない

**症状**: `docker compose up` を実行すると `Cannot connect to the Docker daemon` というエラーが出る

**対処**: Docker Desktop を起動してください（Windows: スタートメニュー / Mac: アプリケーションフォルダ）。  
タスクトレイまたはメニューバーにクジラアイコンが出てから再度実行してください。

---

### バックエンドが起動しない（DB 接続エラー）

**症状**: `Connection refused` や `FATAL: database "skilize" does not exist` というエラーが出る

**対処**:
1. DB コンテナが起動・ヘルスチェック完了しているか確認します

```bash
docker compose ps
# skilize-db-1 が "running (healthy)" になっているか確認
```

2. `healthy` でない場合はさらに 30 秒ほど待ってから再確認します
3. `.env` の `DB_NAME`・`DB_USER`・`DB_PASSWORD` が `.env.example` の内容と一致しているか確認します

---

### フロントエンドの `/api` が 502 になる

**症状**: API コールが `502 Bad Gateway` エラーになる

**対処**:
- **Docker 一括起動の場合**: バックエンドコンテナの起動を待ちます（バックエンドは DB の起動完了後に起動するため、少し時間がかかります）

```bash
docker compose logs -f backend   # "Started BackendApplication" が出るまで待つ
```

- **ローカル起動（B 方法）の場合**: IntelliJ でバックエンドが起動しているか確認します（ポート 8080）

---

### AI 機能が動かない

**症状**: AI キャリア分析が実行されない、またはエラーになる

**対処**:
1. `.env` の `AI_ENABLED=true` になっているか確認します
2. `OPENAI_API_KEY`（または `ANTHROPIC_API_KEY`）が正しく設定されているか確認します
3. `LLM_PROVIDER` と `LLM_MODEL` の組み合わせが正しいか確認します（例: `openai` なら `gpt-4o`）
4. AI コンテナのログでエラー内容を確認します

```bash
docker compose logs -f ai
```

---

### ポートが競合する

**症状**: `bind: address already in use` というエラーが出る

このアプリが使うポートは以下のとおりです。他のアプリが同じポートを使っていると起動できません。

| ポート | サービス |
|---|---|
| 8081 | nginx（メインアクセス口） |
| 8080 | バックエンド（Spring Boot） |
| 5173 | フロントエンド（Vite） |
| 5433 | PostgreSQL（ホスト側） |

競合しているプロセスを停止するか、`docker-compose.yml` のポート設定を変更してください。

---

### `npm install` でエラーが出る

**症状**: Node.js のバージョンが古くてエラーになる

```bash
node --version   # v20 以上であることを確認
```

バージョンが古い場合は [nvm](https://github.com/nvm-sh/nvm)（Mac/Linux）または [nvm-windows](https://github.com/coreybutler/nvm-windows) でバージョンを切り替えてください。

---

### IntelliJ で Gradle のインポートに失敗する

以下を確認してください:

1. `apps/backend/build.gradle` をプロジェクトとしてインポートしているか
2. 「File」→「Project Structure」→「SDKs」で JDK 21 が登録されているか
3. 登録されていない場合は「+」→「JDK の追加」でインストール先フォルダを指定します  
   （例: Mac は `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`）

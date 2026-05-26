# 開発環境構築手順書

## 目次

1. [必要なツールのインストール](#1-必要なツールのインストール)
   - [Git](#11-git)
   - [Docker Desktop](#12-docker-desktop)
   - [JDK 21（IDE デバッグ実行時のみ）](#13-jdk-21ide-デバッグ実行時のみ)
   - [Node.js（IDE デバッグ実行時のみ）](#14-nodejside-デバッグ実行時のみ)
   - [IDE のインストール](#15-ide-のインストール)
2. [リポジトリのクローン](#2-リポジトリのクローン)
3. [環境変数の設定](#3-環境変数の設定)
4. [社内プロキシ環境での設定](#4-社内プロキシ環境での設定)（プロキシ・CA 証明書・npm）
5. [起動方法](#5-起動方法)
   - [A. Docker 一括起動（推奨）](#a-docker-一括起動推奨)
   - [B. IntelliJ + ローカル起動（デバッグ向け）](#b-intellij--ローカル起動デバッグ向け)
   - [C. VSCode + ローカル起動（デバッグ向け）](#c-vscode--ローカル起動デバッグ向け)
   - [D. Cursor + ローカル起動（デバッグ向け）](#d-cursor--ローカル起動デバッグ向け)
   - [E. Eclipse + ローカル起動（デバッグ向け）](#e-eclipse--ローカル起動デバッグ向け)
6. [動作確認](#6-動作確認)
7. [テストユーザー](#7-テストユーザー)
8. [よく使うコマンド](#8-よく使うコマンド)
9. [トラブルシューティング](#9-トラブルシューティング)

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

### 1.3 JDK 21（IDE デバッグ実行時のみ）

バックエンド（Spring Boot）を IDE でローカル実行する場合に必要です。  
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

### 1.4 Node.js（IDE デバッグ実行時のみ）

フロントエンド（React）をローカルで直接実行する場合に必要です。  
Docker 一括起動（方法 A）のみ使う場合はインストール不要です。

このプロジェクトは **Node.js 20 LTS** が必要です。  
Vite 8・Vitest 3・jsdom 26 が Node.js 20 以上を要求するため、v18 以下では動作しません。  
複数バージョンを管理しやすい **nvm**（バージョンマネージャー）の使用を推奨します。

#### nvm を使う場合（推奨）

**Mac / Linux**

```bash
# nvm のインストール（https://github.com/nvm-sh/nvm）
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.0/install.sh | bash

# シェルを再起動後、Node.js のインストール
nvm install 20   # v20 LTS をインストール

# プロジェクトルートで実行すると .nvmrc のバージョン（20）が自動適用される
nvm use
```

**Windows**

1. [github.com/coreybutler/nvm-windows](https://github.com/coreybutler/nvm-windows) からインストーラーをダウンロードして実行します
2. インストール後、以下を実行します:

```powershell
nvm install 20
nvm use 20
```

#### nvm を使わない場合

**Windows**

1. [nodejs.org](https://nodejs.org/en) にアクセスし、**LTS 版**（推奨版）をダウンロードしてインストールします

**Mac**

```bash
brew install node@20
```

**確認**

```bash
node --version
# v20.x.x 以上と表示されれば OK

npm --version
# 10.x.x 以上と表示されれば OK
```

---

### 1.5 IDE のインストール

IDE（統合開発環境）はいずれか 1 つをインストールしてください。

#### IntelliJ IDEA

1. [jetbrains.com/idea/download](https://www.jetbrains.com/idea/download/) からダウンロードします
2. **Community Edition（無料）** または **Ultimate Edition（有料）** を選択してインストールします

> Spring Boot プロジェクトのデバッグは Community Edition でも可能です。

#### VSCode

1. [code.visualstudio.com](https://code.visualstudio.com/) からダウンロードしてインストールします
2. 起動後、以下の拡張機能をインストールします（拡張機能パネル `Ctrl+Shift+X` / `Cmd+Shift+X` から検索）

   | 拡張機能名 | 提供元 | 用途 |
   |---|---|---|
   | Extension Pack for Java | Microsoft | Java 実行・デバッグ・補完 |
   | Spring Boot Extension Pack | VMware | Spring Boot デバッグ補助・Dashboard |
   | Gradle for Java | Microsoft | Gradle プロジェクトサポート |
   | ESLint | Microsoft | フロントエンドコード品質チェック |

#### Cursor

1. [cursor.com](https://www.cursor.com/) からダウンロードしてインストールします
2. Cursor は VSCode をベースとしているため **VSCode と同じ拡張機能**が使えます
3. 上記 VSCode と同じ拡張機能をインストールします

> **Cursor の設定は VSCode と共通**です。以降の「C. VSCode + ローカル起動」の手順をそのまま適用できます。

#### Eclipse

1. [eclipse.org/downloads](https://www.eclipse.org/downloads/) から **Eclipse IDE for Enterprise Java and Web Developers** をダウンロードしてインストールします
2. 起動後、Spring Tools プラグインをインストールします
   - 「Help」→「Eclipse Marketplace...」を開きます
   - 「Spring Tools 4」で検索し、**Spring Tools 4 (aka Spring Tool Suite 4)** をインストールします
   - インストール後、Eclipse を再起動します

> Spring Tools 4 を入れることで、Spring Boot アプリを Eclipse から直接起動・デバッグできるようになります。

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

## 4. 社内プロキシ環境での設定

社内ネットワークにプロキシサーバーがある場合、設定なしでは Docker イメージのビルドや依存パッケージのダウンロードが失敗します。  
以下の設定を行うことでプロキシ環境でも動作させられます。

> **プロキシが不要な環境（自宅・プロキシなし）では設定不要**です。`.env` の該当行はコメントのままにして次のセクションへ進んでください。

---

### 4.1 各ツールとプロキシの関係

| ツール・処理 | プロキシ設定方法 | 備考 |
|---|---|---|
| Docker Desktop（イメージ pull） | Docker Desktop の GUI 設定 | Docker Hub からのダウンロードに必要 |
| Docker ビルド（RUN コマンド） | `.env` の `HTTP_PROXY` / `HTTPS_PROXY` | apk・wget・pip・npm のビルド時ダウンロード |
| Gradle（依存関係ダウンロード） | `.env` の `JAVA_TOOL_OPTIONS` | **Java は `HTTP_PROXY` を読まない**ため専用設定が必要 |
| npm（ビルド時 npm install） | `.env` の `HTTP_PROXY` / `HTTPS_PROXY` | Dockerfile 内で自動的に npm config に変換される |
| npm（ローカル直接起動時） | `npm config set proxy` または `~/.npmrc` | Node.js バージョンに依存しないため、v18→v20 移行後もそのまま有効 |
| Python pip（依存関係ダウンロード） | `.env` の `HTTP_PROXY` / `HTTPS_PROXY` | pip は標準プロキシ env var を参照する |
| AI コンテナ（OpenAI / Anthropic API 呼び出し） | `.env` の `HTTP_PROXY` / `HTTPS_PROXY` | 外部 API への通信に必要 |
| Git（クローン・プッシュ） | `git config --global` | 下記参照 |
| IDE（IntelliJ / VSCode / Eclipse） | 各 IDE の設定画面 | 下記参照 |

---

### 4.2 Docker Desktop のプロキシ設定

Docker Desktop 自身のプロキシを設定しないと、`docker pull` でイメージの取得に失敗します。

1. Docker Desktop を起動します
2. 右上の歯車アイコン（Settings）を開きます
3. 「Resources」→「Proxies」を開きます
4. 「Manual proxy configuration」を有効にします
5. 以下を入力します:

   | 項目 | 入力値の例 |
   |---|---|
   | Web Server (HTTP) | `http://proxy.corp.example.com:8080` |
   | Secure Web Server (HTTPS) | `http://proxy.corp.example.com:8080` |
   | Bypass proxy settings for these hosts and domains | `localhost,127.0.0.1,db,backend,frontend,ai` |

6. 「Apply & restart」をクリックします

---

### 4.3 .env へのプロキシ設定

`.env` ファイルを開き、末尾のプロキシ設定のコメントを外して編集します。

```env
# ─── 社内プロキシ設定 ────────────────────────────────────────────
HTTP_PROXY=http://proxy.corp.example.com:8080
HTTPS_PROXY=http://proxy.corp.example.com:8080
NO_PROXY=localhost,127.0.0.1,db,backend,frontend,ai

# Gradle / Spring Boot 用（Java は HTTP_PROXY を読まないため個別指定が必要）
JAVA_TOOL_OPTIONS=-Dhttp.proxyHost=proxy.corp.example.com -Dhttp.proxyPort=8080 -Dhttps.proxyHost=proxy.corp.example.com -Dhttps.proxyPort=8080 -Dhttp.nonProxyHosts="localhost|127.0.0.1|db|backend|frontend|ai"
```

**設定値の確認ポイント**:

| 項目 | 説明 |
|---|---|
| `proxy.corp.example.com` | 社内プロキシのホスト名または IP アドレスに変更する |
| `8080` | 社内プロキシのポート番号に変更する |
| `NO_PROXY` | **必ず Docker サービス名（`db,backend,frontend,ai`）を含める**こと。含めないとコンテナ間通信がプロキシ経由になり接続失敗する |
| `nonProxyHosts` | Java 用。区切り文字は `,` でなく `\|`（パイプ）を使う |

> **認証プロキシの場合**（ユーザー名・パスワードが必要な場合）:
> ```env
> HTTP_PROXY=http://ユーザー名:パスワード@proxy.corp.example.com:8080
> HTTPS_PROXY=http://ユーザー名:パスワード@proxy.corp.example.com:8080
> ```
> パスワードに特殊文字が含まれる場合は URL エンコード（例: `@` → `%40`）が必要です。

---

### 4.4 設定後の起動手順

`.env` を保存したら、イメージを再ビルドします。  
プロキシ設定はビルド時にも渡す必要があるため、**必ず `--build` オプションを付けて**起動します。

```bash
docker compose up --build
```

---

### 4.5 Git のプロキシ設定

リポジトリのクローンやプッシュにプロキシが必要な場合は以下を実行します。

```bash
git config --global http.proxy http://proxy.corp.example.com:8080
git config --global https.proxy http://proxy.corp.example.com:8080
```

不要になったときは以下で削除できます。

```bash
git config --global --unset http.proxy
git config --global --unset https.proxy
```

---

### 4.6 CA 証明書の設定（社内ルートCA証明書が必要な場合）

社内プロキシが SSL インスペクションを行っている環境では、プロキシが独自のルートCA証明書で HTTPS 通信を再署名します。  
この場合、Docker コンテナ内の各ランタイム（Node.js / Java / Python）でその CA 証明書を信頼させる必要があります。

このプロジェクトは `CA_CERT_ENABLED=true` の設定で、社内ルートCA証明書をすべてのコンテナに自動的に組み込む仕組みを提供しています。

> **プロキシが不要な環境（自宅・CA 証明書なし）では設定不要**です。`CA_CERT_ENABLED` はデフォルト `false` のため何も変更しなくて構いません。

---

#### ステップ 0: 社内ルートCA証明書ファイルの取得

社内ルートCA証明書は、通常 IT 部門によって PC に配布済みです。  
以下の手順で OS の証明書ストアからエクスポートします。

**Windows（certmgr.msc を使う方法）**

1. `Win + R` で「ファイル名を指定して実行」を開き、`certmgr.msc` と入力して「OK」をクリックします
2. 左ペインで「信頼されたルート証明機関」→「証明書」を開きます
3. 一覧から社内のルートCA証明書を探します（「発行先」や「発行者」に会社名が含まれているものを選択します）
4. 対象の証明書を右クリック → 「すべてのタスク」→「エクスポート」を選択します
5. 「証明書のエクスポートウィザード」が起動します:
   - 「次へ」をクリックします
   - エクスポートファイルの形式で **「Base 64 encoded X.509 (.CER)」** を選択して「次へ」をクリックします
   - ファイル名に `company-root-ca` を入力し、保存先を指定します
   - 「完了」をクリックします
6. エクスポートしたファイルを `infra/certs/company-root-ca.cer` にコピーします

**Windows（PowerShell を使う方法）**

```powershell
# 信頼されたルート証明機関の一覧を確認（Subject に会社名が含まれるものを探す）
Get-ChildItem -Path Cert:\LocalMachine\Root | Select-Object Subject, Thumbprint

# 対象の証明書を PEM 形式でエクスポート（Thumbprint を書き換える）
$cert = Get-ChildItem -Path Cert:\LocalMachine\Root | Where-Object { $_.Thumbprint -eq "ここにThumbprintを入力" }
$bytes = $cert.Export([System.Security.Cryptography.X509Certificates.X509ContentType]::Cert)
$b64   = [System.Convert]::ToBase64String($bytes, [System.Base64FormattingOptions]::InsertLineBreaks)
$pem   = "-----BEGIN CERTIFICATE-----`n$b64`n-----END CERTIFICATE-----"
[System.IO.File]::WriteAllText("$PWD\infra\certs\company-root-ca.cer", $pem)
```

> コマンドはプロジェクトルート（`skilize/`）で実行してください。

**Mac**

1. Spotlight（`Cmd + Space`）で「キーチェーンアクセス」を開きます
2. 左ペインで「システム」または「システムのルート」を選択します
3. 一覧から社内のルートCA証明書を探します（名前に会社名が含まれているものを選択します）
4. 対象の証明書を右クリック（または `Control + クリック`）→「書き出す」を選択します
5. フォーマットを **「証明書（.cer）」** のままにして保存します
6. エクスポートしたファイルを `infra/certs/company-root-ca.cer` にコピーします

> **証明書が見つからない場合**: IT 部門にルートCA証明書ファイルの入手方法を確認してください。

---

#### ステップ 1: 証明書ファイルの配置

社内ルートCA証明書を以下のパスに配置します。  
ファイル名は `company-root-ca.cer` にしてください。

```
infra/certs/company-root-ca.cer
```

> `infra/certs/` ディレクトリは既にリポジトリに存在します。  
> 証明書ファイルは `.gitignore` 登録済みで **Git 管理対象外**です。コミット・プッシュは行われません。

---

#### ステップ 2: .env の設定変更

`.env` ファイルを開き、`CA_CERT_ENABLED` を `true` に変更します。

```env
CA_CERT_ENABLED=true
```

---

#### ステップ 3: コンテナの再ビルド・起動

設定後、コンテナを再ビルドして起動します。

```bash
docker compose up --build
```

---

#### 有効化される内容

| コンテナ | 信頼設定 | 説明 |
|---|---|---|
| frontend | OS CA バンドル + `NODE_EXTRA_CA_CERTS` | Node.js が npm registry・外部 API に HTTPS 接続できるようになる |
| backend | OS CA バンドル + Java キーストア（`keytool`） | Gradle の依存解決・Spring Boot の外部通信が正常に行われる |
| ai | OS CA バンドル + `REQUESTS_CA_BUNDLE` | pip・Python requests ライブラリが HTTPS 通信できるようになる |

---

#### 無効化する場合

`.env` を元に戻してコンテナを再ビルドします。

```env
CA_CERT_ENABLED=false
```

```bash
docker compose up --build
```

---

### 4.7 IDE のプロキシ設定

#### IntelliJ IDEA

1. 「File」→「Settings」（Mac: 「IntelliJ IDEA」→「Settings」）を開きます
2. 「Appearance & Behavior」→「System Settings」→「HTTP Proxy」を開きます
3. 「Manual proxy configuration」を選択して入力します

また、Gradle がダウンロードする際にもプロキシが必要です。  
プロジェクトの `apps/backend/gradle.properties` に以下を追記します（`.gitignore` 対象外のため注意）:

```properties
systemProp.http.proxyHost=proxy.corp.example.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.corp.example.com
systemProp.https.proxyPort=8080
systemProp.http.nonProxyHosts=localhost|127.0.0.1
```

> **注意**: `gradle.properties` に認証情報を書く場合は Git にコミットしないよう `.gitignore` への追加を検討してください。  
> または、ユーザーホームの `~/.gradle/gradle.properties` に書く方法（プロジェクト全体に適用されます）も有効です。

#### VSCode / Cursor

1. `Ctrl+Shift+P` / `Cmd+Shift+P` でコマンドパレットを開きます
2. 「Preferences: Open User Settings (JSON)」を検索して開きます
3. 以下を追加します:

```json
{
  "http.proxy": "http://proxy.corp.example.com:8080",
  "http.proxyStrictSSL": false
}
```

Java 拡張機能（Gradle）のプロキシは IntelliJ と同様に `gradle.properties` で設定します。

#### Eclipse

1. 「Window」→「Preferences」（Mac: 「Eclipse」→「Preferences」）を開きます
2. 「General」→「Network Connections」を開きます
3. 「Active Provider」を「Manual」に変更して入力します

---

### 4.8 npm のプロキシ設定（ローカル直接起動時）

Docker 一括起動（方法 A）では `.env` の設定が自動で反映されますが、ローカル直接起動（方法 B〜E）で `npm install` や `npm run dev` を実行する際は npm に対してプロキシを手動で設定する必要があります。

> **Node.js 18 → 20 に移行する場合**: npm のプロキシ設定は `~/.npmrc`（ユーザーホームディレクトリ）に保存されます。これは Node.js のバージョンに依存しないため、`nvm use 20` を実行するだけで既存の設定はそのまま有効です。追加の変更は不要です。

---

#### npm プロキシの設定

以下のコマンドを一度実行すると `~/.npmrc` に永続保存されます。

```bash
npm config set proxy http://proxy.corp.example.com:8080
npm config set https-proxy http://proxy.corp.example.com:8080
npm config set noproxy localhost,127.0.0.1
```

`~/.npmrc` を直接編集しても構いません:

```ini
proxy=http://proxy.corp.example.com:8080
https-proxy=http://proxy.corp.example.com:8080
noproxy=localhost,127.0.0.1
```

> **認証プロキシの場合**: `http://ユーザー名:パスワード@proxy.corp.example.com:8080` の形式を使用します。  
> パスワードに特殊文字が含まれる場合は URL エンコード（例: `@` → `%40`）が必要です。

不要になった場合は以下で削除できます:

```bash
npm config delete proxy
npm config delete https-proxy
```

---

#### CA 証明書の設定（ローカル直接起動時）

Docker 環境では `CA_CERT_ENABLED=true` で自動適用されますが、ローカルで npm を実行する際は `NODE_EXTRA_CA_CERTS` 環境変数で証明書を指定します。

**Windows（PowerShell）**

```powershell
# 現在のセッションのみ有効
$env:NODE_EXTRA_CA_CERTS = "C:\git\skilize\infra\certs\company-root-ca.cer"
```

永続化する場合は「システムのプロパティ」→「環境変数」→「ユーザー環境変数」に `NODE_EXTRA_CA_CERTS` を追加します。

**Mac / Linux**

```bash
# 現在のセッションのみ有効
export NODE_EXTRA_CA_CERTS=/path/to/company-root-ca.cer

# 永続化（~/.zshrc または ~/.bashrc に追記）
echo 'export NODE_EXTRA_CA_CERTS=/path/to/company-root-ca.cer' >> ~/.zshrc
```

---

## 5. 起動方法

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

6. 「OK」で保存します

#### ステップ 3: デバッグ実行

「🐛 Debug」ボタン（または `Shift+F9`）でバックエンドを起動します。

#### ステップ 4: ブレークポイントの使い方

1. デバッグしたい Java ファイルを開きます
2. 行番号の左側の余白をクリックすると **赤い丸（ブレークポイント）** が表示されます
3. ブラウザや API クライアントから対象の処理を呼び出すと、ブレークポイントで実行が一時停止します
4. 停止中は以下の操作ができます:

   | 操作 | ショートカット | 説明 |
   |---|---|---|
   | 再開 | `F9` | 次のブレークポイントまで実行を続ける |
   | ステップオーバー | `F8` | 現在の行を実行して次の行へ（メソッド内部には入らない） |
   | ステップイン | `F7` | 現在の行のメソッド内部に入る |
   | ステップアウト | `Shift+F8` | 現在のメソッドを抜けて呼び出し元へ戻る |

5. 「Variables」パネルで現在のローカル変数の値を確認できます

#### ステップ 5: フロントエンドをローカル起動

```bash
cd apps/frontend
npm install      # 初回のみ。依存パッケージをダウンロードする
npm run dev      # 開発サーバーを起動する
```

`http://localhost:5173` でアクセスできます。  
Vite の設定により、`/api` へのリクエストは自動的に `localhost:8080`（バックエンド）へ転送されます。

---

### C. VSCode + ローカル起動（デバッグ向け）

VSCode で Java のブレークポイントデバッグと TypeScript のソースマップデバッグの両方が使えます。  
JDK 21・Node.js・VSCode（拡張機能インストール済み）が必要です。

#### ステップ 1: DB コンテナのみ起動

```bash
docker compose up db
```

#### ステップ 2: VSCode でプロジェクトを開く

プロジェクトルート（`skilize/`）を VSCode で開きます。

```bash
code .
```

初回起動時に Java Extension Pack が `apps/backend` を Gradle プロジェクトとして自動認識します。  
右下に「Building workspace...」と表示されている間は認識処理中です（1〜2 分）。

#### ステップ 3: launch.json の確認

リポジトリには `.vscode/launch.json` が同梱されており、バックエンド・フロントエンド両方のデバッグ設定が含まれています。  
追加の設定変更は不要です。

<details>
<summary>launch.json の内容（参考）</summary>

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "SkilizeBackend (Debug)",
            "request": "launch",
            "mainClass": "com.skilize.BackendApplication",
            "projectName": "backend",
            "env": {
                "SPRING_DATASOURCE_URL": "jdbc:postgresql://localhost:5433/skilize",
                "SPRING_DATASOURCE_USERNAME": "skilize",
                "SPRING_DATASOURCE_PASSWORD": "password",
                "JWT_SECRET": "change-this-to-a-random-256-bit-secret-key-before-use",
                "AI_ENABLED": "false",
                "AI_SECRET_KEY": "local-secret",
                "SPRING_PROFILES_ACTIVE": "local"
            }
        },
        {
            "type": "chrome",
            "name": "SkilizeFrontend (Debug)",
            "request": "launch",
            "url": "http://localhost:5173",
            "webRoot": "${workspaceFolder}/apps/frontend/src",
            "sourceMapPathOverrides": {
                "/@fs/*": "${workspaceFolder}/*"
            }
        }
    ]
}
```

</details>

#### ステップ 4: バックエンドのデバッグ起動

1. サイドバーの「実行とデバッグ」パネルを開きます（`Ctrl+Shift+D` / `Cmd+Shift+D`）
2. 上部のドロップダウンから **「SkilizeBackend (Debug)」** を選択します
3. 「▶ デバッグの開始」（`F5`）をクリックします
4. デバッグコンソールに `Started BackendApplication` が表示されれば起動完了です

#### ステップ 5: バックエンドのブレークポイントの使い方

1. デバッグしたい Java ファイルを開きます
2. 行番号の左側の余白をクリックすると **赤い丸（ブレークポイント）** が表示されます
3. ブラウザから対象の処理を呼び出すと、ブレークポイントで実行が一時停止します
4. 停止中は以下の操作ができます:

   | 操作 | ショートカット | 説明 |
   |---|---|---|
   | 再開 | `F5` | 次のブレークポイントまで実行を続ける |
   | ステップオーバー | `F10` | 現在の行を実行して次の行へ（メソッド内部には入らない） |
   | ステップイン | `F11` | 現在の行のメソッド内部に入る |
   | ステップアウト | `Shift+F11` | 現在のメソッドを抜けて呼び出し元へ戻る |

5. 「変数」パネルで現在の変数の値を確認できます

#### ステップ 6: フロントエンドの起動とデバッグ

ターミナルで開発サーバーを起動します:

```bash
cd apps/frontend
npm install      # 初回のみ
npm run dev
```

フロントエンドのブレークポイントデバッグ（`.tsx` / `.ts` ファイル）を使う場合:

1. Vite 開発サーバーが `http://localhost:5173` で起動している状態にします
2. 「実行とデバッグ」パネルで **「SkilizeFrontend (Debug)」** を選択して `F5` を押します
3. Chrome が自動的に開きます
4. VSCode で `.tsx` / `.ts` ファイルの行番号左をクリックしてブレークポイントを設定します
5. ブラウザで操作すると VSCode 側でブレークポイントに停止します

> **注意**: フロントエンドデバッグには Chrome がインストールされている必要があります。  
> Chrome の代わりに Edge を使う場合は、`launch.json` の `"type": "chrome"` を `"type": "msedge"` に変更してください。

---

### D. Cursor + ローカル起動（デバッグ向け）

Cursor は VSCode をベースとしているため、**起動・デバッグの手順は VSCode と同一**です。  
上記「[C. VSCode + ローカル起動（デバッグ向け）](#c-vscode--ローカル起動デバッグ向け)」をそのまま適用してください。

**Cursor 固有の補足**:

- `.vscode/launch.json` は Cursor でも同じように機能します（フォルダ名 `.vscode` のままで動作します）
- Cursor の AI 機能（Cmd+K や Cmd+L）を使ってコードの説明や修正の提案を受けながらデバッグすることができます

---

### E. Eclipse + ローカル起動（デバッグ向け）

Eclipse でバックエンドのブレークポイントデバッグができます。  
フロントエンドのデバッグは Eclipse では非対応のため、ブラウザの DevTools を使います。

JDK 21・Node.js・Eclipse（Spring Tools 4 インストール済み）が必要です。

#### ステップ 1: DB コンテナのみ起動

```bash
docker compose up db
```

#### ステップ 2: プロジェクトのインポート

1. Eclipse を起動します
2. 「File」→「Import...」→「Gradle」→「Existing Gradle Project」を選択します
3. 「Project root directory」に `apps/backend` のパスを指定します

   | OS | 例 |
   |---|---|
   | Windows | `C:\git\skilize\apps\backend` |
   | Mac | `/Users/yourname/git/skilize/apps/backend` |

4. 「Finish」をクリックします
5. Gradle のビルドが完了するまで待ちます（右下のステータスバーで進捗確認）

#### ステップ 3: 実行構成に環境変数を設定

1. 「Run」→「Debug Configurations...」を開きます
2. 左側の「Spring Boot App」を展開し、`BackendApplication` を選択します  
   （表示されない場合は「Spring Boot App」を右クリック →「New Configuration」で作成します）
3. 「Arguments」タブ → 「VM arguments」に以下を入力します（**1行ずつ**）:

   ```
   -DSPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/skilize
   -DSPRING_DATASOURCE_USERNAME=skilize
   -DSPRING_DATASOURCE_PASSWORD=password
   -DJWT_SECRET=change-this-to-a-random-256-bit-secret-key-before-use
   -DAI_ENABLED=false
   -DAI_SECRET_KEY=local-secret
   -DSPRING_PROFILES_ACTIVE=local
   ```

   または「Environment」タブから Key/Value 形式で設定することもできます:

   | Key | Value |
   |---|---|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/skilize` |
   | `SPRING_DATASOURCE_USERNAME` | `skilize` |
   | `SPRING_DATASOURCE_PASSWORD` | `password` |
   | `JWT_SECRET` | `change-this-to-a-random-256-bit-secret-key-before-use` |
   | `AI_ENABLED` | `false` |
   | `AI_SECRET_KEY` | `local-secret` |
   | `SPRING_PROFILES_ACTIVE` | `local` |

4. 「Apply」→「Debug」をクリックします

#### ステップ 4: バックエンドのブレークポイントの使い方

1. デバッグしたい Java ファイルを Package Explorer からダブルクリックで開きます
2. 行番号の左側の余白を **ダブルクリック** すると **青い丸（ブレークポイント）** が表示されます  
   （右クリック →「Toggle Breakpoint」でも設定できます）
3. ブラウザから対象の処理を呼び出すと、「Confirm Perspective Switch」ダイアログが表示されます  
   → 「Switch」を選択すると Debug パースペクティブに切り替わり、ブレークポイントで停止します
4. 停止中は以下の操作ができます:

   | 操作 | ショートカット | 説明 |
   |---|---|---|
   | 再開 | `F8` | 次のブレークポイントまで実行を続ける |
   | ステップオーバー | `F6` | 現在の行を実行して次の行へ（メソッド内部には入らない） |
   | ステップイン | `F5` | 現在の行のメソッド内部に入る |
   | ステップアウト | `F7` | 現在のメソッドを抜けて呼び出し元へ戻る |

5. 「Variables」ビューで現在の変数の値を確認できます
6. 「Expressions」ビューに式を入力すると任意の値を評価できます

#### ステップ 5: フロントエンドの起動

ターミナルで開発サーバーを起動します:

```bash
cd apps/frontend
npm install      # 初回のみ
npm run dev
```

`http://localhost:5173` でアクセスできます。

#### フロントエンドのデバッグ（ブラウザ DevTools）

Eclipse にはフロントエンドデバッガーがないため、ブラウザの DevTools を使います。

1. `http://localhost:5173` を Chrome / Edge で開きます
2. `F12` キーで DevTools を開きます
3. 「Sources」タブ → 左側ツリーから `src/` 以下の `.tsx` / `.ts` ファイルを開きます  
   （Vite がソースマップを出力しているため、ビルド前のコードがそのまま表示されます）
4. 行番号をクリックしてブレークポイントを設定します
5. ブラウザで操作すると DevTools 側でブレークポイントに停止します

---

## 6. 動作確認

### アクセス先

| 方法 | URL | 説明 |
|---|---|---|
| Docker 一括起動（A） | http://localhost:8081 | nginx 経由（本番に近い構成） |
| IDE + ローカル（B〜E） | http://localhost:5173 | Vite 直接（コード変更が即反映される） |
| バックエンド API 直接 | http://localhost:8080/api/health | `{"status":"ok"}` が返れば正常 |

### 確認手順

1. ブラウザで上記 URL を開く
2. ログイン画面が表示されることを確認する
3. 下記のテストユーザーでログインして各機能を確認する

---

## 7. テストユーザー

`scripts/db/init.sql` 適用後（Docker 起動時に自動実行）に以下のユーザーが使えます。

| ユーザーID | パスワード | ロール | 確認できる機能 |
|---|---|---|---|
| `admin` | `admin` | ADMIN | マスタ管理・ユーザー管理・年度設定など全機能 |
| `tl01` | `tl01` | TL | チーム照会・面談記録・マスタ参照 |
| `user01` | `user01` | GENERAL | 自分の棚卸入力・ダッシュボード・グラフ閲覧 |
| `user02` | `user02` | GENERAL | 自分の棚卸入力・ダッシュボード・グラフ閲覧 |

---

## 8. よく使うコマンド

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

### フロントエンド（B〜E 方法・ローカル直接起動時）

```bash
cd apps/frontend
npm install        # 依存パッケージのインストール（初回または package.json 変更後）
npm run dev        # 開発サーバー起動
npm run build      # 本番用ビルド（成果物が dist/ に生成される）
npm run lint       # ESLint によるコードチェック
```

---

### Node.js バージョンの切り替え

このプロジェクトは **Node.js 20 LTS** が必要です（v18 以下では Vite 8 / Vitest 3 / jsdom 26 が動作しません）。

#### ローカル直接起動（方法 B〜E）の場合

プロジェクトルートに `.nvmrc` ファイル（内容: `20`）があるため、nvm を使うと自動で切り替わります。

```bash
# .nvmrc に記載のバージョン（20）に切り替える
nvm use
```

nvm をインストールしていない場合は以下から取得してください。

| OS | インストール先 |
|---|---|
| Mac / Linux | [github.com/nvm-sh/nvm](https://github.com/nvm-sh/nvm) |
| Windows | [github.com/coreybutler/nvm-windows](https://github.com/coreybutler/nvm-windows) |

---

## 9. トラブルシューティング

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

- **ローカル起動（B〜E 方法）の場合**: IDE でバックエンドが起動しているか確認します（ポート 8080）

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
node --version   # v20.x.x 以上であることを確認
```

バージョンが古い場合は nvm でバージョンを切り替えてください。プロジェクトルートで以下を実行すると `.nvmrc` に基づいて v20 が自動的に適用されます。

```bash
nvm use
```

---

### IntelliJ で Gradle のインポートに失敗する

以下を確認してください:

1. `apps/backend/build.gradle` をプロジェクトとしてインポートしているか
2. 「File」→「Project Structure」→「SDKs」で JDK 21 が登録されているか
3. 登録されていない場合は「+」→「JDK の追加」でインストール先フォルダを指定します  
   （例: Mac は `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`）

---

### VSCode でバックエンドが起動しない（Java プロジェクト認識エラー）

**症状**: 「SkilizeBackend (Debug)」を実行すると `Build failed` や `Project not found` エラーが出る

**対処**:
1. `Ctrl+Shift+P` / `Cmd+Shift+P` でコマンドパレットを開き、「Java: Clean Java Language Server Workspace」を実行します
2. VSCode を再起動します
3. 右下に「Building workspace...」が表示されなくなるまで待ちます（1〜2 分）
4. 再度デバッグを実行します

---

### Eclipse でブレークポイントに止まらない

**症状**: ブレークポイントを設定しても、処理を呼び出しても停止しない

**対処**:
1. 「Debug As」→「Spring Boot App」（または「Java Application」）で起動しているか確認します  
   （「Run As」で起動するとデバッグモードにならず、ブレークポイントが無効になります）
2. ブレークポイントのアイコンに「×」マークが付いている場合は、コードが変更されてビルドが追いついていない状態です。「Project」→「Build All」（`Ctrl+B`）を実行してから再試行してください

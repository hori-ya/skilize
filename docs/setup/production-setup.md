# 本番環境構築手順書

## データベース構成について（試用期間中の方針）

> **注意**: 本システムの設計書・要件定義書（`docs/architecture/overview.md`、`docs/requirements/non-functional/non-functional-requirements.md`）では、データベースに **AWS RDS（PostgreSQL 16.4）** を使用する前提で記載されています。
>
> **試用期間中はコスト削減のため、RDS を使用せず、EC2 上の Docker コンテナで PostgreSQL を運用します。**  
> アプリケーション側の接続方法（JDBC URL・環境変数）は RDS でも同一のため、将来の RDS 切り替え時にアプリコードの変更は不要です。  
> RDS への移行手順については本文書末尾の「[RDS 移行手順](#11-rds-移行手順)」を参照してください。

---

## Elastic IP について（試用期間中の方針）

> **注意**: 本システムの設計上、EC2 には **Elastic IP**（固定パブリック IP アドレス）を割り当てることを推奨しています。
>
> **試用期間中はコスト削減および誤操作防止のため、Elastic IP を取得しない想定で運用します。**  
> Elastic IP を取得しない場合、EC2 インスタンスを停止・起動するたびにパブリック IP アドレスが変わります。  
> IP が変わった際に必要な再設定手順については「[12. Elastic IP を使用しない場合の手順（試用期間向け）](#12-elastic-ip-を使用しない場合の手順試用期間向け)」を参照してください。

---

## 目次

1. [AWS アカウントの作成と初期設定](#1-aws-アカウントの作成と初期設定)
2. [EC2 インスタンスの作成](#2-ec2-インスタンスの作成)
3. [Elastic IP の設定](#3-elastic-ip-の設定)
4. [SSH 接続](#4-ssh-接続)
5. [Docker・Git のインストール](#5-dockergit-のインストール)
6. [リポジトリのクローン](#6-リポジトリのクローン)
7. [環境変数の設定](#7-環境変数の設定)
8. [アプリケーションの起動](#8-アプリケーションの起動)
9. [動作確認と初期設定](#9-動作確認と初期設定)
10. [GitHub Actions による CI](#10-github-actions-による-ci)
11. [RDS 移行手順](#11-rds-移行手順)
12. [Elastic IP を使用しない場合の手順（試用期間向け）](#12-elastic-ip-を使用しない場合の手順試用期間向け)
13. [運用手順](#13-運用手順)
14. [トラブルシューティング](#14-トラブルシューティング)

---

## 1. AWS アカウントの作成と初期設定

### 1.1 AWS アカウントの作成

AWS（Amazon Web Services）はクラウドサービスのプラットフォームです。  
EC2（仮想サーバー）などのサービスを利用するには、まず AWS アカウントが必要です。

> **費用について**: AWS は使用した分だけ課金される従量制です。  
> EC2 の `t3.small` を 1 か月稼働させると概算で $15〜20 程度かかります（2025年時点）。  
> 後述の「請求アラート」を設定し、想定外の課金を防ぐことを推奨します。

1. [aws.amazon.com](https://aws.amazon.com/jp/) にアクセスし、「AWSアカウントを作成」をクリックします
2. メールアドレス・パスワード・AWSアカウント名を入力します
3. 連絡先情報（氏名・住所・電話番号）を入力します
  ※ アカウント種別は「個人」を選択してください
4. クレジットカード情報を入力します（無料利用枠の確認のみでも登録が必要です）
5. 電話またはテキストメッセージで本人確認を行います
6. サポートプランを選択します（「ベーシックサポート（無料）」で問題ありません）
7. 登録完了後、マネジメントコンソール（管理画面）にサインインします

---

### 1.2 MFA（多要素認証）の設定

AWS のルートアカウントは非常に強力な権限を持つため、乗っ取りを防ぐために MFA を設定します。  
MFA とは、パスワードに加えてスマートフォンのアプリで生成されるコードを使った二段階認証です。

1. マネジメントコンソール右上の「アカウント名」→「セキュリティ認証情報」を開きます
2. 「多要素認証（MFA）」セクションの「MFA デバイスを割り当てる」をクリックします
3. 「認証アプリケーション」を選択します
4. スマートフォンに **Google Authenticator** または **Authy** をインストールして QR コードをスキャンします
5. アプリに表示される 6 桁のコードを 2 回入力して完了します

---

### 1.3 IAM ユーザーの作成（推奨）

ルートアカウントは日常の作業には使わず、権限を絞った IAM ユーザーを作成して使うことが AWS のベストプラクティスです。  
IAM ユーザーとは、AWS アカウント内に作る「操作用のサブアカウント」のようなものです。

1. マネジメントコンソールの検索バーで「IAM」と入力して開きます
2. 左メニュー「ユーザー」→「ユーザーの作成」をクリックします
3. ユーザー名を入力します（例: `skilize-admin`）
4. 「AWS マネジメントコンソールへのアクセスを提供する」にチェックを入れます
5. 「IAM ユーザーを作成したい」を選択し、パスワードを設定します
6. 権限の設定で「ポリシーを直接アタッチする」を選択し、`AdministratorAccess` を付与します
  （本番運用が安定したら、必要な権限のみに絞ることを推奨します）
7. 作成完了後、「コンソールサインイン URL」をメモしておきます
  （例: `https://123456789012.signin.aws.amazon.com/console`）
8. 以降の作業はこの IAM ユーザーでサインインして行います

---

### 1.4 請求アラートの設定

想定外の高額請求を防ぐため、一定金額を超えたらメールで通知するアラートを設定します。

1. マネジメントコンソール右上の「アカウント名」→「請求とコスト管理」を開きます
2. 左メニュー「予算」→「予算を作成」をクリックします
3. 「使用コスト予算」を選択します
4. 予算名（例: `skilize-monthly`）と予算額（例: `3000` 円）を入力します
5. 「アラートしきい値の追加」で 80% 到達時に通知メールが届くよう設定します
6. 通知先のメールアドレスを入力して完了します

---

### 1.5 リージョンの選択

リージョンとは、AWS のサーバーが置かれている地域です。  
日本のユーザーが使うため、**アジアパシフィック（東京）** を選択します。

マネジメントコンソール右上のリージョン表示（例: `us-east-1`）をクリックし、  
「**アジアパシフィック（東京）ap-northeast-1**」を選択してください。

> **重要**: リージョンを間違えると、作成した EC2 が見つからないように見えることがあります。  
> 作業中は常に「東京」リージョンになっているか確認してください。

---

## 2. EC2 インスタンスの作成

EC2（Elastic Compute Cloud）は AWS が提供する仮想サーバーです。  
このサーバー上でアプリケーションを動かします。

### 2.1 キーペアの作成

EC2 にリモートで接続するための「鍵」を事前に作成します。  
物理的な鍵と同じで、秘密鍵（`.pem` ファイル）を手元に保管し、EC2 に公開鍵を登録します。

1. マネジメントコンソールで「EC2」を開きます
2. 左メニュー「ネットワーク & セキュリティ」→「キーペア」を開きます
3. 「キーペアを作成」をクリックします
4. 以下を設定します:
  - **名前**: `skilize-key`
  - **キーペアのタイプ**: RSA
  - **プライベートキーファイル形式**:
    - Mac/Linux: `.pem`
    - Windows（PuTTY を使う場合）: `.ppk`
    - Windows（Windows Terminal/PowerShell を使う場合）: `.pem`
5. 「キーペアを作成」をクリックするとファイルが自動でダウンロードされます

> **重要**: ダウンロードされた `.pem`（または `.ppk`）ファイルは**絶対に紛失しないよう**保管してください。  
> このファイルは再ダウンロードできません。紛失した場合はキーペアを作り直す必要があります。  
> 推奨保管場所: `C:\Users\<ユーザー名>\.ssh\` (Windows) または `~/.ssh/` (Mac/Linux)

---

### 2.2 セキュリティグループの作成

セキュリティグループとは、EC2 への通信の「許可リスト」です。  
ここで許可していない通信は自動的にブロックされます。

1. 左メニュー「ネットワーク & セキュリティ」→「セキュリティグループ」を開きます
2. 「セキュリティグループを作成」をクリックします
3. 以下を入力します:
  - **セキュリティグループ名**: `skilize-sg`
  - **説明**: `Skilize production security group`
  - **VPC**: デフォルト VPC を選択
4. 「インバウンドルール」に以下を追加します:


| タイプ   | プロトコル | ポート範囲 | ソース           | 説明                    |
| ----- | ----- | ----- | ------------- | --------------------- |
| SSH   | TCP   | 22    | 自分の IP（マイ IP） | 管理用 SSH 接続            |
| HTTP  | TCP   | 80    | 0.0.0.0/0     | アプリへの HTTP アクセス       |
| HTTPS | TCP   | 443   | 0.0.0.0/0     | アプリへの HTTPS アクセス（将来用） |


> **SSH の「マイ IP」について**: ソースの選択肢で「マイ IP」を選ぶと、現在使っている IP アドレスが自動入力されます。  
> 自宅・オフィスなど接続場所が変わる場合は、その都度 IP を更新するか、社内のグローバル IP を固定して設定してください。

> **PostgreSQL ポート（5432）は追加しない**: DB は EC2 内部でのみ使用し、外部に公開しません。

1. アウトバウンドルールはデフォルト（全通信許可）のままで問題ありません
2. 「セキュリティグループを作成」をクリックします

---

### 2.3 インスタンスの起動

1. 左メニュー「インスタンス」→「インスタンスを起動」をクリックします
2. 以下を設定します:

**名前とタグ**

- **名前**: `skilize-prod`

**AMI（OS の種類）**

- 検索欄に「Amazon Linux 2023」と入力し、「Amazon Linux 2023 AMI」を選択します  
`ami-xxxxxxxx (64 ビット x86)` と表示されているものを使います

**インスタンスタイプ（サーバーのスペック）**

- `t3.small`（最低限・月額 $15 程度）または `t3.medium`（推奨・月額 $30 程度）を選択します

**キーペア**

- 「2.1 キーペアの作成」で作成した `skilize-key` を選択します

**ネットワーク設定**

- 「セキュリティグループを選択する」を選び、「2.2」で作成した `skilize-sg` を選択します

**ストレージの設定**

- サイズを `30` GB に変更します
- ボリュームタイプは `gp3` のままでよいです

1. 右側「インスタンスを起動」ボタンをクリックします
2. 「インスタンスを正常に起動しました」と表示されれば完了です
3. 「すべてのインスタンスを表示」でインスタンス一覧に戻り、状態が「実行中」になるまで待ちます（1〜2 分）

---

## 3. Elastic IP の設定

通常の EC2 インスタンスに割り当てられる IP アドレスは、インスタンスを停止・起動するたびに変わります。  
Elastic IP は「固定の IP アドレス」で、インスタンスを再起動しても同じ IP を使い続けられます。

1. 左メニュー「ネットワーク & セキュリティ」→「Elastic IP アドレス」を開きます
2. 「Elastic IP アドレスを割り当てる」をクリックします
3. 設定はデフォルトのまま「割り当て」をクリックします（IP アドレスが発行されます）
4. 発行された IP アドレスを選択し、「アクション」→「Elastic IP アドレスの関連付け」をクリックします
5. 「インスタンス」欄で先ほど作成した `skilize-prod` を選択し、「関連付け」をクリックします
6. インスタンス一覧に戻り、「パブリック IPv4 アドレス」に Elastic IP が表示されていることを確認します

> **費用について**: Elastic IP はインスタンスに関連付けられている間は無料ですが、インスタンスを**停止した状態**で保持し続けると課金されます（約 $0.005/時間）。  
> 長期間使わない場合はインスタンスを停止するだけでなく、Elastic IP の関連付けも解除してください。

> **試用期間中（Elastic IP を使用しない場合）**: このセクションはスキップして「4. SSH 接続」へ進んでください。  
> EC2 インスタンス一覧の「パブリック IPv4 アドレス」を `<Elastic-IP>` の代わりに使用します。  
> インスタンス停止・起動後の再設定手順は「[12. Elastic IP を使用しない場合の手順（試用期間向け）](#12-elastic-ip-を使用しない場合の手順試用期間向け)」を参照してください。

---

## 4. SSH 接続

SSH とは、EC2 サーバーにリモートでコマンド操作するための接続方式です。

### 4.1 Mac / Linux の場合

```bash
# .pem ファイルの権限を変更する（所有者のみ読み取り可能にする）
# ※ これをしないと SSH 接続時にエラーになります
chmod 400 ~/.ssh/skilize-key.pem

# EC2 に接続する
ssh -i ~/.ssh/skilize-key.pem ec2-user@<Elastic-IP>
```

`<Elastic-IP>` は EC2 のインスタンス一覧で確認できます（例: `13.115.xxx.xxx`）。

初回接続時に以下のメッセージが表示されます。`yes` と入力して続行します:

```
Are you sure you want to continue connecting (yes/no/[fingerprint])? yes
```

`[ec2-user@ip-xxx-xxx-xxx-xxx ~]$` というプロンプトが表示されれば接続成功です。

---

### 4.2 Windows の場合

Windows 10 以降はデフォルトで OpenSSH クライアントが使えます。  
**PowerShell** または **Windows Terminal** を開いて以下を実行します。

```powershell
# .pem ファイルのアクセス権を設定する（管理者のみ読み取り可能にする）
icacls "C:\Users\<ユーザー名>\.ssh\skilize-key.pem" /inheritance:r /grant:r "$($env:USERNAME):(R)"

# EC2 に接続する
ssh -i "C:\Users\<ユーザー名>\.ssh\skilize-key.pem" ec2-user@<Elastic-IP>
```

> **PowerShell で `ssh` コマンドが見つからない場合**:  
> 「設定」→「アプリ」→「オプション機能」→「OpenSSH クライアント」をインストールしてください。

---

## 5. Docker・Git のインストール

EC2 に SSH 接続した状態で、以下のコマンドを順番に実行します。

### 5.1 パッケージを最新化する

```bash
sudo dnf update -y
```

`dnf` は Amazon Linux 2023 のパッケージマネージャーです（Mac の Homebrew、Windows の winget に相当します）。  
`-y` はすべての確認を自動で「はい」と答えるオプションです。

---

### 5.2 Git をインストールする

```bash
sudo dnf install -y git

# インストール確認
git --version
# git version 2.xx.x と表示されれば OK
```

---

### 5.3 Docker をインストールする

```bash
# Docker 本体をインストール
sudo dnf install -y docker

# Docker サービスを起動する
sudo systemctl start docker

# OS 再起動時も Docker が自動起動するよう設定する
sudo systemctl enable docker

# ec2-user（現在のログインユーザー）を docker グループに追加する
# ※ これにより sudo なしで docker コマンドを使えるようになる
sudo usermod -aG docker ec2-user

# グループ変更を反映するために一度ログアウトする
exit
```

SSH 接続が切れるので、**再度 SSH で接続し直します**。

```bash
# 再接続（Mac/Linux）
ssh -i ~/.ssh/skilize-key.pem ec2-user@<Elastic-IP>

# 再接続（Windows PowerShell）
ssh -i "C:\Users\<ユーザー名>\.ssh\skilize-key.pem" ec2-user@<Elastic-IP>
```

接続後、動作を確認します:

```bash
docker --version
# Docker version 27.x.x と表示されれば OK

docker ps
# sudo なしでエラーなく実行できれば OK
```

---

### 5.4 Docker Compose をインストールする

```bash
sudo dnf install -y docker-compose-plugin

# インストール確認
docker compose version
# Docker Compose version v2.x.x と表示されれば OK
```

---

## 6. リポジトリのクローン

### 6.1 HTTPS でクローンする場合（パブリックリポジトリ）

```bash
cd /home/ec2-user
git clone <リポジトリURL> skilize
cd skilize
```

---

### 6.2 SSH でクローンする場合（プライベートリポジトリ）

プライベートリポジトリの場合は、EC2 に SSH キーを設定してからクローンします。

**EC2 上で SSH キーを生成する**

```bash
# SSH キーペアを生成する（Enter を 3 回押してデフォルト設定で作成）
ssh-keygen -t ed25519 -C "ec2-skilize"

# 生成された公開鍵を表示する
cat ~/.ssh/id_ed25519.pub
```

表示された `ssh-ed25519 AAAA...` から始まる文字列をコピーします。

**GitHub へ公開鍵を登録する**

1. GitHub にログインし、右上のアイコン →「Settings」を開きます
2. 左メニュー「SSH and GPG keys」→「New SSH key」をクリックします
3. タイトルに `ec2-skilize` など識別できる名前を入力します
4. 「Key」欄に先ほどコピーした公開鍵を貼り付けます
5. 「Add SSH key」で保存します

**接続テストとクローン**

```bash
# GitHub への SSH 接続テスト
ssh -T git@github.com
# "Hi <username>! You've successfully authenticated..." と表示されれば OK

# SSH URL でクローン
cd /home/ec2-user
git clone git@github.com:<組織名またはユーザー名>/<リポジトリ名>.git skilize
cd skilize
```

---

## 7. 環境変数の設定

`.env.example` をコピーして本番用の設定ファイルを作成します。

```bash
cp .env.example .env
```

ファイルを編集します（`vi` はターミナル上のテキストエディタです）:

```bash
vi .env
```

> `**vi` の基本操作**:
>
> - `i` キーで編集モードに入る（文字を入力できるようになる）
> - `Esc` キーで編集モードを抜ける
> - `:wq` + `Enter` で保存して終了
> - `:q!` + `Enter` で保存せずに終了
>
> `vi` が難しければ `nano .env` を使うと、より直感的に編集できます。  
> `nano` では `Ctrl+X` → `Y` → `Enter` で保存して終了できます。

### 設定内容

以下の内容に書き換えます。`<...>` の部分は実際の値に置き換えてください。

```env
# ─────────────────────────────────────────────────────
# Docker Compose の設定（本番用ファイルを使う）
# ─────────────────────────────────────────────────────
COMPOSE_FILE=infra/compose/docker-compose.prod.yml

# ─────────────────────────────────────────────────────
# データベース接続情報
# ─────────────────────────────────────────────────────
# 本番では推測されにくいパスワードに変更する
DB_NAME=skilize
DB_USER=skilize
DB_PASSWORD=<強力なパスワード>

# ─────────────────────────────────────────────────────
# JWT（認証トークン）の設定
# ─────────────────────────────────────────────────────
# ランダムな文字列（32文字以上）。下記のコマンドで生成できる
# コマンド: openssl rand -base64 32
JWT_SECRET=<ランダムな文字列>
JWT_EXPIRATION_MS=28800000   # 8時間

# ─────────────────────────────────────────────────────
# CORS（アクセス許可するオリジン）
# ─────────────────────────────────────────────────────
# 本番でアクセスするドメインまたは IP アドレスを指定する
FRONTEND_ORIGIN=http://<Elastic-IP>

# ─────────────────────────────────────────────────────
# Spring Boot のプロファイル（本番は prod に設定）
# ─────────────────────────────────────────────────────
# prod にすることで Flyway（DB マイグレーション）が有効になる
SPRING_PROFILES_ACTIVE=prod

# ─────────────────────────────────────────────────────
# AI 機能の設定
# ─────────────────────────────────────────────────────
AI_ENABLED=true

# OpenAI を使う場合（デフォルト）
LLM_PROVIDER=openai
LLM_MODEL=gpt-4o
OPENAI_API_KEY=<OpenAI の API キー>

# Anthropic（Claude）を使う場合は以下を使用
# LLM_PROVIDER=anthropic
# LLM_MODEL=claude-opus-4-7
# ANTHROPIC_API_KEY=<Anthropic の API キー>

# AI サービスとの内部通信認証キー（ランダムな文字列）
AI_SECRET_KEY=<ランダムな文字列>

# AI サービスの URL（変更不要）
AI_SERVICE_URL=http://ai:8000
```

### ランダム文字列の生成

`JWT_SECRET` や `AI_SECRET_KEY` に使うランダムな文字列は以下のコマンドで生成できます:

```bash
openssl rand -base64 32
# 例: K9mXvB3nPqR7sT1uW6yZ2cE4aG0jL5oN8dF+hI/k=
```

このコマンドを 2 回実行し、それぞれ `JWT_SECRET` と `AI_SECRET_KEY` に設定します。

### ファイルのアクセス権を設定する

`.env` にはパスワードや API キーが含まれるため、他のユーザーが読めないよう保護します:

```bash
chmod 600 .env
```

### バックアップ用ディレクトリを作成する

```bash
mkdir -p /home/ec2-user/backups
```

---

## 8. アプリケーションの起動

### 初回起動

```bash
docker compose up --build -d
```

- `--build`: Docker イメージをビルド（コンパイル・パッケージング）します
- `-d`: バックグラウンドで起動します（ターミナルを占有しません）

初回はイメージのビルドがあるため 5〜15 分程度かかります。

### 起動状態の確認

```bash
docker compose ps
```

以下のように全サービスが `running` または `healthy` になっていれば正常です:

```
NAME                  STATUS
skilize-db-1          running (healthy)
skilize-backend-1     running
skilize-ai-1          running
skilize-nginx-1       running
```

`Exit` や `Restarting` が表示されている場合はログを確認します:

```bash
docker compose logs backend   # バックエンドのログ（エラー内容が確認できる）
docker compose logs db        # DB のログ
docker compose logs ai        # AI モジュールのログ
```

### Flyway によるスキーマ自動作成

`SPRING_PROFILES_ACTIVE=prod` の状態でバックエンドが起動すると、Flyway が DB スキーマを自動的に作成します。  
バックエンドのログに以下が表示されれば正常に完了しています:

```
Successfully applied 7 migrations to schema "public"
```

---

## 9. 動作確認と初期設定

### 9.1 ヘルスチェック

EC2 上で以下を実行してアプリが応答しているか確認します:

```bash
curl http://localhost/api/health
# {"status":"ok"} のようなレスポンスが返れば正常
```

### 9.2 ブラウザからのアクセス

自分の PC のブラウザで `http://<Elastic-IP>` を開き、ログイン画面が表示されることを確認します。

---

### 9.3 初期管理者ログインとセキュリティ設定

Flyway によって初期データ（`V4__test_data.sql`）が投入され、以下のユーザーが使えます。


| ユーザーID   | パスワード    | ロール     |
| -------- | -------- | ------- |
| `admin`  | `admin`  | ADMIN   |
| `tl01`   | `tl01`   | TL      |
| `user01` | `user01` | GENERAL |
| `user02` | `user02` | GENERAL |


**本番運用開始前に必ず以下を実施してください:**

1. `**admin` でログインし、パスワードを変更する**
  ログイン後、右上のメニューから「パスワード変更」を選択します
2. **本番ユーザーを登録する**
  ADMIN メニュー →「ユーザー管理」から実際に使用するユーザーを登録します  
   初期パスワードは自動生成され、登録完了画面に一度だけ表示されます
3. **テストユーザーを無効化する**
  `tl01`・`user01`・`user02` はテスト用のため、本番では「ユーザー管理」から無効化します

---

## 10. GitHub Actions による CI

GitHub Actions を使って、`main` ブランチへの push または Pull Request のタイミングで自動的にテストを実行します。

### CI で実行されるテスト

| ジョブ | テスト対象 | 使用ツール |
|---|---|---|
| `backend` | AuthServiceTest・JwtUtilTest・AuthControllerTest・InventoryServiceComparisonTest | JUnit 5 / Gradle |
| `frontend` | LoginPage.test.tsx・InventoryHistoryPage.test.tsx | Vitest |

バックエンドのユニットテスト・Web レイヤーテストは DB 接続不要のため、PostgreSQL の別途セットアップは不要です。

---

### 10.1 前提条件

- リポジトリが GitHub でホストされていること
- `apps/frontend/package-lock.json` がコミットされていること（`npm ci` が利用するため）
- GitHub Personal Access Token（PAT）に **`workflow` スコープ**が付与されていること（`.github/workflows/` 配下のファイルをプッシュするために必要）

PAT に `workflow` スコープがない場合、プッシュ時に以下のエラーが発生します:

```
! [remote rejected] main -> main (refusing to allow a Personal Access Token to create or update workflow ... without `workflow` scope)
```

スコープを追加するには GitHub → **Settings** → **Developer settings** → **Personal access tokens** でトークンを編集し、`workflow` にチェックを入れて保存します。その後、Windows の「資格情報マネージャー」で `github.com` のエントリを新しいトークンで更新してください。

`package-lock.json` がまだコミットされていない場合は以下を実行します:

```bash
cd apps/frontend
npm install   # package-lock.json が生成される
git add package-lock.json
git commit -m "add package-lock.json"
```

---

### 10.2 ワークフローファイルの作成

リポジトリルートに `.github/workflows/ci.yml` を作成します。

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  # ─── バックエンドテスト ───────────────────────────────────────
  backend:
    name: Backend Unit Tests (JUnit 5)
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: gradle

      - name: Grant execute permission for gradlew
        run: chmod +x apps/backend/gradlew

      - name: Run backend unit tests
        working-directory: apps/backend
        run: |
          ./gradlew test \
            --tests "com.skilize.auth.application.AuthServiceTest" \
            --tests "com.skilize.shared.infrastructure.JwtUtilTest" \
            --tests "com.skilize.auth.presentation.AuthControllerTest" \
            --tests "com.skilize.inventory.application.InventoryServiceComparisonTest"

      - name: Upload test report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: backend-test-report
          path: apps/backend/build/reports/tests/test/

  # ─── フロントエンドテスト ─────────────────────────────────────
  frontend:
    name: Frontend Tests (Vitest)
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: apps/frontend/package-lock.json

      - name: Install dependencies
        working-directory: apps/frontend
        run: npm ci

      - name: Run frontend tests
        working-directory: apps/frontend
        run: npm test

      - name: Upload test report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: frontend-test-report
          path: apps/frontend/test-results/
```

このファイルを作成してコミット・プッシュするだけで CI が有効になります。GitHub Actions の追加設定は不要です。

```bash
git add .github/workflows/ci.yml
git commit -m "add GitHub Actions CI workflow"
git push origin main
```

---

### 10.3 CI 実行結果の確認

1. GitHub のリポジトリページを開きます
2. 上部の「Actions」タブをクリックします
3. 最新のワークフロー実行が一覧表示されます
   - ✅ 緑のチェックマーク → 全テスト通過
   - ❌ 赤の×マーク → テスト失敗あり

---

### 10.4 テストレポートのダウンロード

テスト結果の詳細は Artifacts としてダウンロードできます。

1. 「Actions」タブで対象のワークフロー実行をクリックします
2. ページ下部の「Artifacts」セクションに `backend-test-report` と `frontend-test-report` が表示されます
3. クリックしてダウンロードし、zip を展開します
4. `backend-test-report/index.html` をブラウザで開くと、テストケースごとの合否と失敗時のスタックトレースを確認できます

---

### 10.5 CI が失敗した場合の対処

**ログの確認**

1. 「Actions」タブ → 失敗したワークフロー実行をクリックします
2. 失敗したジョブ（`backend` または `frontend`）をクリックします
3. 失敗したステップを展開するとエラーメッセージが確認できます

**よくある失敗原因**

| 症状 | 原因 | 対処 |
|---|---|---|
| `gradlew: Permission denied` | gradlew に実行権限がない | `git update-index --chmod=+x apps/backend/gradlew` を実行してコミット |
| `npm ci` 失敗 | `package-lock.json` がコミットされていない | `apps/frontend/package-lock.json` をコミットする |
| `Could not find class ...` | テストクラスのパッケージ名が変わった | ワークフローの `--tests` 引数のクラス名を修正する |
| バックエンドのコンパイルエラー | 本番コードに構文エラーがある | エラーメッセージを元にコードを修正する |

---

## 11. RDS 移行手順

試用期間終了後に PostgreSQL を RDS へ切り替える手順です。  
アプリケーションコードの変更は不要で、接続先の設定変更だけで移行できます。

### 11.1 RDS インスタンスの作成

1. AWS マネジメントコンソールで「RDS」を開きます
2. 「データベースの作成」をクリックします
3. 以下を設定します:
  - **エンジン**: PostgreSQL
  - **バージョン**: 16.4
  - **テンプレート**: 本番稼働用（または「開発/テスト」でコスト削減）
  - **インスタンスクラス**: `db.t3.micro`（小規模向け）
  - **DB インスタンス識別子**: `skilize-db`
  - **マスターユーザー名**: `.env` の `DB_USER` と同じ値
  - **マスターパスワード**: `.env` の `DB_PASSWORD` と同じ値
  - **ストレージ**: 20 GB（自動スケーリング有効推奨）
  - **VPC**: EC2 と同じ VPC
4. 「接続」セクションで「EC2 コンピューティングリソースに接続」を選択し、EC2 インスタンスを指定します
  （セキュリティグループが自動設定されます）
5. 「データベースの作成」をクリックします（作成完了まで 5〜10 分かかります）

RDS の「エンドポイント」をメモしておきます（例: `skilize-db.xxxx.ap-northeast-1.rds.amazonaws.com`）

---

### 11.2 データ移行

EC2 上で以下を実行します。

```bash
# 1. 現在の EC2 上の DB からデータをダンプ（バックアップ）する
cd /home/ec2-user/skilize
docker compose exec db pg_dump -U skilize skilize > /home/ec2-user/migration.sql

# 2. RDS に psql クライアントをインストール（まだ入っていない場合）
sudo dnf install -y postgresql16

# 3. RDS にダンプデータをリストアする
psql -h <RDSエンドポイント> -U skilize -d skilize < /home/ec2-user/migration.sql
# パスワードを求められたら DB_PASSWORD の値を入力する
```

---

### 11.3 接続先の切り替え

`infra/compose/docker-compose.prod.yml` の `backend` と `ai` の接続先を RDS に変更します:

```bash
vi infra/compose/docker-compose.prod.yml
```

**backend の environment を変更**（`db:5432` → RDS エンドポイントに変更）:

```yaml
environment:
  SPRING_PROFILES_ACTIVE: prod
  SPRING_DATASOURCE_URL: jdbc:postgresql://<RDSエンドポイント>:5432/${DB_NAME}
  SPRING_DATASOURCE_USERNAME: ${DB_USER}
  SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
```

**ai の environment を変更**:

```yaml
DATABASE_URL: postgresql://${DB_USER}:${DB_PASSWORD}@<RDSエンドポイント>:5432/${DB_NAME}
```

また、`db` サービスと `volumes.db_data` の定義も削除します（EC2 上の DB は不要になるため）。

---

### 11.4 アプリの再起動と確認

```bash
docker compose down
docker compose up -d
```

起動後に `docker compose logs backend` でエラーがないことを確認します。  
ブラウザからログインできれば移行完了です。

---

## 12. Elastic IP を使用しない場合の手順（試用期間向け）

試用期間中はコスト削減のため Elastic IP を取得しません。  
EC2 インスタンスを停止・起動するたびにパブリック IP アドレスが変わるため、起動のたびに以下の手順で設定を更新してください。

### 12.1 初回セットアップ時の読み替え

「

1. Elastic IP の設定」をスキップし、以降の手順内に出てくる `<Elastic-IP>` はすべて

EC2 インスタンス一覧の「パブリック IPv4 アドレス」（例: `13.115.xxx.xxx`）に読み替えてください。

---

### 12.2 インスタンス停止・起動後の再設定手順

**手順 1: 新しいパブリック IP を確認する**

AWS マネジメントコンソール →「EC2」→「インスタンス」を開き、  
`skilize-prod` の「パブリック IPv4 アドレス」列に表示された新しい IP をメモします。

---

**手順 2: 新しい IP で SSH 接続する**

```bash
# Mac/Linux
ssh -i ~/.ssh/skilize-key.pem ec2-user@<新しいIP>

# Windows PowerShell
ssh -i "C:\Users\<ユーザー名>\.ssh\skilize-key.pem" ec2-user@<新しいIP>
```

> **「接続拒否」される場合**: セキュリティグループの SSH ルールを「マイ IP」で設定している場合、  
> 作業 PC の IP が変わっているとブロックされます。  
> AWS マネジメントコンソール →「EC2」→「セキュリティグループ」→「skilize-sg」を開き、  
> SSH インバウンドルールのソース IP を現在の自分の IP に更新してください。

---

**手順 3: `.env` の `FRONTEND_ORIGIN` を更新する**

`FRONTEND_ORIGIN` は CORS の許可オリジンです。  
IP が変わるとブラウザからの API リクエストがブロックされるため、**必ず更新が必要です**。

```bash
cd /home/ec2-user/skilize
nano .env   # または vi .env
```

以下の行を新しい IP に書き換えます:

```env
FRONTEND_ORIGIN=http://<新しいIP>
```

保存後、バックエンドコンテナを再起動して CORS 設定を反映させます:

```bash
docker compose restart backend
```

---

**手順 4: コンテナの起動を確認する**

EC2 インスタンスの起動時に Docker コンテナは自動的に再起動します（`restart: always` 設定のため）。  
念のため起動状態を確認します:

```bash
docker compose ps
# 全サービスが running / healthy になっていることを確認する
```

`Exit` 状態のサービスがある場合は手動で起動します:

```bash
docker compose up -d
```

---

**手順 5: 動作確認**

```bash
curl http://localhost/api/health
# {"status":"ok"} が返れば正常
```

ブラウザで `http://<新しいIP>` を開き、ログイン画面が表示されることを確認します。

---

### 12.3 停止・起動後の再設定チェックリスト


| 対象                       | 対応内容                             | 備考                         |
| ------------------------ | -------------------------------- | -------------------------- |
| 新しいパブリック IP              | EC2 コンソールで確認してメモする               | 手順 1                       |
| SSH 接続                   | 新しい IP で接続し直す                    | 手順 2                       |
| セキュリティグループ（SSH ルール）      | 自分の PC の IP が変わった場合のみ更新          | 接続拒否された場合に確認               |
| `.env` `FRONTEND_ORIGIN` | 新しい IP に書き換える                    | 更新しないと CORS エラーでブラウザから使えない |
| バックエンドコンテナ               | `docker compose restart backend` | CORS 設定の反映                 |
| ブラウザのブックマーク・アクセス URL     | 新しい IP で開き直す                     | —                          |


---

## 13. 運用手順

### アプリケーションの停止・再起動

```bash
# 停止
docker compose down

# 再起動
docker compose up -d

# 特定サービスのみ再起動（DB は停止しない）
docker compose restart backend
docker compose restart ai
```

---

### アプリケーションの更新（新バージョンデプロイ）

```bash
cd /home/ec2-user/skilize

# 最新コードを取得
git pull origin main

# バックエンド・AI・nginx を再ビルドして起動（DB は停止しない）
docker compose up --build -d backend ai nginx
```

> `SPRING_PROFILES_ACTIVE=prod` のため、スキーマ変更がある場合は Flyway がバックエンド起動時に自動で適用します。

---

### データベースのバックアップ

EC2 上の DB は RDS のような自動バックアップ機能がないため、手動で定期バックアップを行います。

#### 手動バックアップ

```bash
cd /home/ec2-user/skilize
docker compose exec -T db pg_dump -U skilize skilize > /home/ec2-user/backups/backup_$(date +%Y%m%d).sql

# バックアップファイルを確認
ls -lh /home/ec2-user/backups/
```

#### S3 への自動バックアップ（推奨）

AWS S3 はファイルを安全に保管できるストレージサービスです。EC2 が壊れても S3 のバックアップは残ります。

```bash
# AWS CLI のインストール（未インストールの場合）
sudo dnf install -y aws-cli

# S3 へアップロード（バケット名は事前に S3 コンソールで作成しておく）
aws s3 cp /home/ec2-user/backups/backup_$(date +%Y%m%d).sql s3://<バケット名>/db-backups/
```

#### cron による自動バックアップの設定

毎日 AM 2:00 に自動バックアップを実行する設定例です:

```bash
crontab -e
```

以下を追記します（`i` キーで編集モードに入り、貼り付け後 `Esc` → `:wq` で保存）:

```
0 2 * * * cd /home/ec2-user/skilize && docker compose exec -T db pg_dump -U skilize skilize > /home/ec2-user/backups/backup_$(date +\%Y\%m\%d).sql
```

---

### バックアップからのリストア

```bash
cat /home/ec2-user/backups/backup_YYYYMMDD.sql | docker compose exec -T db psql -U skilize -d skilize
```

---

### ログの確認

```bash
# リアルタイムログ（全サービス）
docker compose logs -f

# 直近 100 行のみ表示
docker compose logs --tail=100 backend
```

---

### OS・Docker のメンテナンス

```bash
# OS のパッケージ更新
sudo dnf update -y

# 使用していない Docker イメージを削除（ディスクの節約）
docker image prune -f
```

---

## 14. トラブルシューティング

### バックエンドが起動しない

**確認コマンド**:

```bash
docker compose logs backend
```

**よくある原因と対処**:


| 症状                               | 原因                      | 対処                                             |
| -------------------------------- | ----------------------- | ---------------------------------------------- |
| `Connection refused`             | DB がまだ起動中               | 数十秒待ってから `docker compose ps` で `healthy` を確認する |
| `password authentication failed` | `.env` の DB パスワードが誤っている | `.env` の `DB_PASSWORD` を確認する                   |
| `JWT_SECRET` 関連のエラー              | `JWT_SECRET` が未設定       | `.env` に `JWT_SECRET` を設定する                    |


---

### Flyway マイグレーションエラー

```bash
docker compose logs backend | grep -i flyway
```

**原因**: マイグレーション済みのファイル（`V1__` 〜）を後から編集した場合に発生します。  
**対処**: 編集したファイルを元に戻すか、新しいバージョン（`V8__xxx.sql` など）で変更を追加します。

---

### AI モジュールが動かない

```bash
docker compose logs ai
```

- `OPENAI_API_KEY` または `ANTHROPIC_API_KEY` が未設定・誤設定の場合に発生します
- `AI_ENABLED=false` に設定すれば AI 機能をオフにしてアプリ本体は動作させられます

---

### ディスク容量不足

```bash
df -h                      # ディスク使用状況を確認
docker system df           # Docker が使用している容量を確認
docker image prune -f      # 未使用イメージを削除
# ※ docker volume prune は DB データを消してしまうため実行しないこと
```

---

### コンテナが再起動を繰り返す（restart loop）

```bash
docker compose logs --tail=50 <サービス名>
```

`restart: always` の設定のため、エラーがあっても自動再起動し続けます。  
ログでエラー内容を特定し、根本原因を修正してから再起動します。

---

### EC2 を再起動したらアプリが起動していない

Docker サービスの自動起動を確認します:

```bash
sudo systemctl status docker   # Docker が動いているか確認
sudo systemctl enable docker   # 自動起動が無効なら有効化
```

アプリの自動起動を設定するには `/etc/rc.local` に起動コマンドを追加します:

```bash
sudo vi /etc/rc.local
```

以下を追記します:

```bash
cd /home/ec2-user/skilize && docker compose up -d
```

保存後、実行権限を付与します:

```bash
sudo chmod +x /etc/rc.local
```

---

### SSH 接続できない

**Mac/Linux の場合**:

```bash
# 権限エラー（WARNING: UNPROTECTED PRIVATE KEY FILE!）が出た場合
chmod 400 ~/.ssh/skilize-key.pem
```

**Windows の場合**:

PowerShell でパーミッションを再設定します:

```powershell
icacls "C:\Users\<ユーザー名>\.ssh\skilize-key.pem" /inheritance:r /grant:r "$($env:USERNAME):(R)"
```

**それでも繋がらない場合**:

1. EC2 インスタンスが「実行中」になっているか確認します
2. セキュリティグループのインバウンドルールに SSH（ポート 22）が追加されているか確認します
3. SSH の許可 IP が現在の自分の IP と一致しているか確認します（IP が変わった場合はセキュリティグループを更新）


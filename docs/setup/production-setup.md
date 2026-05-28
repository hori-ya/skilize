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
13. [HTTPS 対応（アプローチ B: ALB + ACM）](#13-https-対応アプローチ-b-alb--acm)
14. [HTTPS 対応（アプローチ C: Let's Encrypt + Certbot）](#14-https-対応アプローチ-c-lets-encrypt--certbot)
15. [運用手順](#15-運用手順)
16. [トラブルシューティング](#16-トラブルシューティング)

---

## 1. AWS アカウントの作成と初期設定

### 1.1 AWS アカウントの作成

AWS（Amazon Web Services）はクラウドサービスのプラットフォームです。  
EC2（仮想サーバー）などのサービスを利用するには、まず AWS アカウントが必要です。

> **費用について**: AWS は使用した分だけ課金される従量制です。  
> EC2 を 1 か月常時稼働させた場合の概算（2025年時点）: `t3.small` 約 $15〜20、`t3.medium` 約 $35〜40。  
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

> **リージョン一覧をクリックしても切り替わらない場合**:  
> 右上の歯車アイコン（⚙）→「全てのユーザー設定を表示」→「ローカリゼーションとデフォルトのリージョン」の編集から「アジアパシフィック (東京)」を設定してください。

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

> **説明欄について**: AWS の説明欄は英数字・記号のみ有効です（日本語不可）。  
> 「AWS 入力値」列の文字列をそのまま貼り付けてください。

| タイプ   | プロトコル | ポート範囲 | ソース           | 説明（AWS 入力値）              | 意味           |
| ----- | ----- | ----- | ------------- | ------------------------- | ------------ |
| SSH   | TCP   | 22    | 自分の IP（マイ IP） | `SSH admin access`        | 管理用 SSH 接続   |
| HTTP  | TCP   | 80    | 0.0.0.0/0     | `HTTP app access`         | アプリへの HTTP アクセス |
| HTTPS | TCP   | 443   | 0.0.0.0/0     | `HTTPS app access future` | HTTPS アクセス（将来用） |


> **SSH の「マイ IP」について**: ソースの選択肢で「マイ IP」を選ぶと、現在使っている IP アドレスが自動入力されます。  
> 自宅・オフィスなど接続場所が変わる場合は、その都度 IP を更新するか、社内のグローバル IP を固定して設定してください。

> **PostgreSQL ポート（5432）は追加しない**: DB は EC2 内部でのみ使用し、外部に公開しません。

5. アウトバウンドルールはデフォルト（全通信許可）のままで問題ありません
6. 「セキュリティグループを作成」をクリックします

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

3. 右側「インスタンスを起動」ボタンをクリックします
4. 「インスタンスを正常に起動しました」と表示されれば完了です
5. 「すべてのインスタンスを表示」でインスタンス一覧に戻り、状態が「実行中」になるまで待ちます（1〜2 分）

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

### 4.3 Windows の場合（WinSCP を使う場合）

WinSCP は Windows 向けのグラフィカルな SFTP/SCP クライアントです。  
コマンド操作なしにドラッグ＆ドロップでファイルを転送でき、EC2 上のファイルをローカルのエディタで直接編集することもできます。

> **PowerShell（4.2）との使い分け**: ファイルの転送・編集には WinSCP が便利です。`docker compose` などのコマンド実行には PowerShell または WinSCP の内蔵ターミナルを使います。

#### インストール

1. [winscp.net](https://winscp.net/eng/download.php) からインストーラーをダウンロードして実行します
2. インストール完了後、WinSCP を起動します

#### EC2 への接続

1. 起動直後に「ログイン」ダイアログが開きます（開かない場合はメニュー「セッション」→「新しいセッション」を選択します）
2. 以下を入力します:

   | 項目 | 入力値 |
   |---|---|
   | ファイルプロトコル | SFTP |
   | ホスト名 | EC2 のパブリック IP アドレス（例: `13.115.xxx.xxx`） |
   | ポート番号 | 22 |
   | ユーザー名 | `ec2-user` |
   | パスワード | （空欄のまま） |

3. 「高度な設定...」をクリックし、左メニューの「SSH」→「認証」を開きます
4. 「秘密鍵ファイル」欄の「...」ボタンをクリックして、ダウンロードした `skilize-key.pem` を選択します
   - 「OpenSSH 形式の秘密鍵を PuTTY 形式に変換しますか？」というダイアログが表示された場合は「はい」をクリックします
   - 変換後の `.ppk` ファイルの保存先を指定して「保存」をクリックします（以後はこの `.ppk` を使用します）
5. 「OK」をクリックして設定を閉じます
6. 「保存」をクリックしてセッションを保存しておくと次回から入力が不要になります
7. 「ログイン」をクリックします

初回接続時に「サーバーのホストキーがキャッシュにありません」というダイアログが表示されます。  
内容を確認して「はい」をクリックしてください。

接続が成功すると、左側にローカル PC・右側に EC2 のファイルシステムが表示されます。

#### ファイルの転送

ファイルをパネル間でドラッグ＆ドロップするだけで転送できます。

| 操作 | 方法 |
|---|---|
| `.env` のアップロード | ローカル（左）の `.env` を右パネルの `/home/ec2-user/skilize/` にドラッグ |
| バックアップ SQL のダウンロード | 右パネルの `/home/ec2-user/backups/` 内ファイルを左パネルにドラッグ |
| `infra/certs/` への証明書アップロード | ローカルの `.cer` ファイルを右パネルの対応パスにドラッグ |

#### ファイルの直接編集

EC2 上のテキストファイルをローカルのエディタで直接開いて編集できます。

1. 右パネルで編集したいファイル（例: `/home/ec2-user/skilize/.env`）を右クリックします
2. 「編集」を選択するとローカルのテキストエディタで開きます
3. 編集して保存すると自動的に EC2 へ反映されます

> **注意**: デフォルトのエディタは Windows メモ帳です。VS Code などに変更するには「オプション」→「環境設定」→「エディター」で設定してください。

#### SSH ターミナルを開く

WinSCP から SSH ターミナルを起動して `docker compose` コマンドなどを実行できます。

1. 接続中に上部メニュー「コマンド」→「ターミナルを開く」を選択します（または `Ctrl+T`）
2. ターミナルウィンドウが開き、コマンドを実行できます

> ターミナルの動作が不安定な場合は PowerShell（4.2）での SSH 接続を使用してください。

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

### 5.4 Docker Compose・Buildx をインストールする

Amazon Linux 2023 の標準リポジトリには `docker-compose-plugin` と `docker-buildx-plugin` が含まれていないため、公式バイナリを直接インストールします。

```bash
# Docker CLI プラグイン用ディレクトリを作成
sudo mkdir -p /usr/local/lib/docker/cli-plugins

# Docker Compose の最新バイナリをダウンロード
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose

# Docker Buildx の最新バイナリをダウンロード
sudo curl -SL "$(curl -s https://api.github.com/repos/docker/buildx/releases/latest \
  | grep 'browser_download_url.*linux-amd64"' \
  | cut -d '"' -f 4)" \
  -o /usr/local/lib/docker/cli-plugins/docker-buildx

# 実行権限を付与
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-buildx

# インストール確認
docker compose version
# Docker Compose version v2.x.x と表示されれば OK

docker buildx version
# github.com/docker/buildx v0.x.x と表示されれば OK
```

---

## 6. リポジトリのクローン

> **推奨**: EC2 では後述の `git pull`（デプロイ更新）のために SSH 認証が必要です。  
> **6.2 の SSH 方式でクローンすることを推奨します。**  
> HTTPS でクローンした場合も、6.2 の手順で SSH に切り替えられます。

### 6.1 HTTPS でクローンする場合

```bash
cd /home/ec2-user
git clone <リポジトリURL> skilize
cd skilize
```

クローン後、6.2 の「SSH キーの生成と GitHub 登録」を行い、リモート URL を SSH に切り替えます:

```bash
git remote set-url origin git@github.com:<組織名またはユーザー名>/<リポジトリ名>.git
```

---

### 6.2 SSH でクローンする場合（推奨）

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

> **vi の基本操作**:
>
> - `i` キーで編集モードに入る（文字を入力できるようになる）
> - `Esc` キーで編集モードを抜ける
> - `:wq` + `Enter` で保存して終了
> - `:q!` + `Enter` で保存せずに終了
>
> `vi` が難しければ `nano .env` を使うと、より直感的に編集できます。  
> `nano` では `Ctrl+X` → `Y` → `Enter` で保存して終了できます。

> **WinSCP を使う場合**: ターミナルコマンドを使わずに `.env` を編集できます。  
> WinSCP で EC2 に接続後、右パネルで `/home/ec2-user/skilize/.env` を右クリック →「編集」を選択してください。  
> ローカルのテキストエディタで編集・保存すると自動的に EC2 へ反映されます（「4.3 WinSCP を使う場合」参照）。

### 設定内容

以下の内容に書き換えます。`<...>` の部分は実際の値に置き換えてください。

```env
# ─────────────────────────────────────────────────────
# Docker Compose の設定（本番用ファイルを使う）
# ─────────────────────────────────────────────────────
COMPOSE_FILE=infra/compose/docker-compose.prod.yml

# フロントエンドコンテナの Node.js バージョン
NODE_VERSION=20

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
# 8時間（インラインコメント不可。値のみ記載すること）
JWT_EXPIRATION_MS=28800000

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

# ─────────────────────────────────────────────────────
# テストデータの設定
# ─────────────────────────────────────────────────────
# true にするとテストユーザー（tl01/user01/user02）が DB に追加される
# 試用期間中の動作確認に使用する。通常運用では false にすること。
# ※ 一度 true で起動したあと false に戻してもテストデータは削除されない（手動削除が必要）
LOAD_TEST_DATA=false
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
skilize-backend-1     running (healthy)
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
# LOAD_TEST_DATA=false（デフォルト）の場合
Successfully applied 7 migrations to schema "public"

# LOAD_TEST_DATA=true（テストデータあり）の場合
Successfully applied 8 migrations to schema "public"
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

Flyway（`V2__required_data.sql`）によって管理者ユーザーが作成され、以下のアカウントでログインできます。

| ユーザーID  | 初期パスワード | ロール |
| --------- | ----------- | ----- |
| `admin`   | `admin`     | ADMIN |

> **デフォルト（`LOAD_TEST_DATA=false`）では tl01・user01・user02 は作成されません。**  
> これらのテストユーザーは `V4__test_data.sql`（`db/testdata/` 配置）で管理されています。  
> 試用期間中に動作確認が必要な場合は、`.env` に `LOAD_TEST_DATA=true` を設定して起動してください。

**本番運用開始前に必ず以下を実施してください:**

1. **`admin` でログインし、パスワードを変更する**  
  ログイン後、右上のメニューから「パスワード変更」を選択します  
  （`is_initial_password=true` のため、パスワード変更画面に自動遷移します）
2. **本番ユーザーを登録する**  
  ADMIN メニュー →「ユーザー管理」から実際に使用するユーザーを登録します  
  初期パスワードは自動生成され、登録完了画面に一度だけ表示されます

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
| `rolldownVersion is missing` | vite.config.ts に vitest の `test` 設定が混在している | `vitest.config.ts` を分離して `vite.config.ts` から `test` を削除する |

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

---

### 12.0 EC2 インスタンスの停止と起動

使用しない時間帯はインスタンスを停止することで EC2 の時間課金を抑えられます。

> **費用の目安**: インスタンスを停止すると EC2 の時間課金（`t3.medium` で約 $0.052/時間）が停止します。  
> ただし EBS（ストレージ）の費用（30GB の場合 約 $2〜3/月）は停止中も発生します。  
> EBS 費用も含めて完全に止めたい場合は「12.4 試用期間終了後のリソース削除」を参照してください。

**インスタンスの停止手順**

1. AWS マネジメントコンソール →「EC2」→「インスタンス」を開く
2. `skilize-prod` を選択する
3. 右上の「インスタンスの状態」→「インスタンスを停止」をクリック
4. 確認ダイアログで「停止」をクリック
5. ステータスが「stopped」になれば停止完了

**インスタンスの起動手順**

1. AWS マネジメントコンソール →「EC2」→「インスタンス」を開く
2. `skilize-prod` を選択する
3. 右上の「インスタンスの状態」→「インスタンスを起動」をクリック
4. ステータスが「running」になるまで 1〜2 分待つ

> **起動後は必ず「12.2 インスタンス停止・起動後の再設定手順」を実施してください。**  
> Elastic IP を使用していないため、起動のたびにパブリック IP が変わります。

---

### 12.1 初回セットアップ時の読み替え

「3. Elastic IP の設定」をスキップし、以降の手順内に出てくる `<Elastic-IP>` はすべて EC2 インスタンス一覧の「パブリック IPv4 アドレス」（例: `13.115.xxx.xxx`）に読み替えてください。

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

### 12.4 試用期間終了後のリソース削除（EBS 費用も含め完全停止）

EC2 インスタンスを「停止」しても EBS（ストレージ）の費用は発生し続けます。  
本番稼働開始まで AWS 費用を完全にゼロにしたい場合は、インスタンスを「**終了（terminate）**」してください。  
終了するとインスタンスと EBS が完全に削除され、すべての費用が止まります。

> **注意**: 終了するとインスタンスと DB データが**完全に失われます**。  
> 必要なデータは必ずバックアップしてから実施してください。

---

**手順 1: DB データをバックアップする**

インスタンスを起動した状態で SSH 接続して実行します:

```bash
cd /home/ec2-user/skilize
docker compose exec -T db pg_dump -U skilize skilize > /home/ec2-user/backup_final.sql
```

バックアップファイルをローカル PC に転送します:

```bash
# Mac / Linux
scp -i ~/.ssh/skilize-key.pem ec2-user@<EC2のIP>:/home/ec2-user/backup_final.sql ./

# Windows PowerShell
scp -i "C:\Users\<ユーザー名>\.ssh\skilize-key.pem" ec2-user@<EC2のIP>:/home/ec2-user/backup_final.sql .
```

> **WinSCP を使う場合**: 接続後、右パネルで `/home/ec2-user/backup_final.sql` を選択し、左パネルの保存先にドラッグ＆ドロップするだけでダウンロードできます（「4.3 WinSCP を使う場合」参照）。

---

**手順 2: EC2 インスタンスを終了する**

1. AWS マネジメントコンソール →「EC2」→「インスタンス」を開く
2. `skilize-prod` を選択する
3. 「インスタンスの状態」→「**インスタンスを終了**」をクリック
4. 確認ダイアログで「終了」をクリック
5. ステータスが `terminated` になれば完了

> **EBS の自動削除について**: デフォルト設定ではルート EBS ボリュームは「削除オン終了」が有効になっているため、インスタンス終了と同時に自動削除されます。  
> 念のため「EC2」→「ボリューム」でボリュームが残っていないことを確認してください。

---

**手順 3: 残存リソースを確認・削除する**

| リソース               | 確認場所                  | 費用       | 対応                           |
| ------------------ | --------------------- | -------- | ---------------------------- |
| EBS スナップショット       | EC2 → スナップショット        | $0.05/GB/月 | 不要なら削除する                     |
| キーペア               | EC2 → キーペア            | 無料       | 本番で再利用するなら保持、不要なら削除          |
| セキュリティグループ         | EC2 → セキュリティグループ      | 無料       | 本番で再利用するなら保持、不要なら削除          |

---

---

**手順 4: 請求ダッシュボードで費用が止まっていることを確認する**

インスタンス終了後 1〜2 日以内に AWS の請求ダッシュボードで確認します。

1. AWS マネジメントコンソール右上の「アカウント名」→「請求とコスト管理」を開きます
2. 左メニュー「請求書」または「コストエクスプローラー」を開きます
3. EC2 と EBS の課金が発生していないことを確認します

> **ポイント**: AWS の請求は 1〜2 日の遅延があります。終了直後の請求額が $0 にならなくても正常です。  
> 数日後に再確認して EC2・EBS の項目が消えていれば問題ありません。

残存リソースがないかも「EC2」→「ボリューム」「スナップショット」「Elastic IP」で念のため確認してください。

---

**本番稼働開始時**: 改めて「[2. EC2 インスタンスの作成](#2-ec2-インスタンスの作成)」から手順を実施してください。  
DB データは手順 1 のバックアップから「[15. 運用手順 > バックアップからのリストア](#バックアップからのリストア)」でリストアできます。

---

## 13. HTTPS 対応（アプローチ B: ALB + ACM）

Application Load Balancer で TLS を終端し、AWS Certificate Manager の証明書を無料・自動更新で管理する方法です。

**特徴**
- 証明書の取得・更新が完全自動（ACM 管理）
- EC2 側のアプリ設定変更が最小限（docker-compose・nginx の変更不要）
- ALB の追加費用が発生（約 $20〜/月）
- 公開ドメインと Route 53 または外部 DNS が必要

---

### 13.1 前提条件の確認

- 公開ドメインが取得済みで、Route 53 または外部 DNS で管理されていること
- EC2 に Elastic IP が割り当て済みであること（「3. Elastic IP の設定」参照）

---

### 13.2 ACM で証明書を申請する

1. AWS マネジメントコンソールで「**Certificate Manager（ACM）**」を検索して開きます
2. **リージョンが「東京（ap-northeast-1）」になっていることを確認します**（ALB と同じリージョンが必要）
3. 「証明書をリクエスト」→「パブリック証明書をリクエスト」→「次へ」をクリックします
4. 「完全修飾ドメイン名」に使用するドメインを入力します（例: `skilize.example.com`）
5. 「検証方法」は「**DNS 検証**」を選択します
6. 「リクエスト」をクリックします

**DNS 検証レコードを追加する**

申請後、証明書の詳細画面に `CNAME 名` と `CNAME 値` が表示されます。

| DNS プロバイダー | 操作 |
|---|---|
| Route 53 | 証明書詳細画面の「Route 53 でレコードを作成」ボタンをクリックすると自動設定されます |
| 外部 DNS（お名前.com 等） | 表示された CNAME 名・CNAME 値を DNS プロバイダーの管理画面で手動追加します |

DNS が反映されると（最長 30 分）、証明書のステータスが「**発行済み**」に変わります。  
「発行済み」になるまで次の手順に進まないでください。

---

### 13.3 ターゲットグループを作成する

ALB が EC2 にリクエストを転送するための設定です。

1. EC2 →「ターゲットグループ」→「ターゲットグループの作成」をクリックします
2. 以下を設定します:

   | 項目 | 設定値 |
   |---|---|
   | ターゲットタイプ | インスタンス |
   | ターゲットグループ名 | `skilize-tg` |
   | プロトコル | HTTP |
   | ポート | 80 |
   | ヘルスチェックパス | `/api/health` |

3. 「次へ」→ インスタンス一覧から `skilize-prod` を選択 →「保留中として以下を含める」をクリックします
4. 「ターゲットグループの作成」をクリックします

---

### 13.4 セキュリティグループを設定する

**ALB 用セキュリティグループを作成する**

1. EC2 →「セキュリティグループ」→「セキュリティグループを作成」をクリックします
2. 以下を入力します:
   - **名前**: `skilize-alb-sg`
   - **説明**: `Skilize ALB security group`
3. インバウンドルールを追加します:

   | タイプ | ポート | ソース | 説明（AWS 入力値） |
   |---|---|---|---|
   | HTTP | 80 | 0.0.0.0/0 | `ALB HTTP inbound` |
   | HTTPS | 443 | 0.0.0.0/0 | `ALB HTTPS inbound` |

4. 「セキュリティグループを作成」をクリックします

**EC2 の HTTP 許可を ALB のみに制限する**

これにより、ALB を経由せずに EC2 に直接 HTTP アクセスすることを防げます。

1. `skilize-sg` を開き、「インバウンドルールを編集」をクリックします
2. 既存の「HTTP（ポート 80 / 0.0.0.0/0）」ルールを削除します
3. 以下のルールを追加します:

   | タイプ | ポート | ソース | 説明（AWS 入力値） |
   |---|---|---|---|
   | HTTP | 80 | `skilize-alb-sg`（セキュリティグループを選択） | `HTTP from ALB only` |

4. 「ルールを保存」をクリックします

---

### 13.5 ALB を作成する

1. EC2 →「ロードバランサー」→「ロードバランサーの作成」→「Application Load Balancer」の「作成」をクリックします
2. 以下を設定します:

   | 項目 | 設定値 |
   |---|---|
   | 名前 | `skilize-alb` |
   | スキーム | インターネット向け |
   | IP アドレスタイプ | IPv4 |
   | VPC | EC2 と同じ VPC（デフォルト VPC） |
   | アベイラビリティゾーン | 東京リージョンの AZ を **2 つ以上**チェック |
   | セキュリティグループ | `skilize-alb-sg`（`default` は削除） |

3. **リスナーとルーティング**を設定します:

   | プロトコル | ポート | デフォルトアクション |
   |---|---|---|
   | HTTP | 80 | `https://#{host}:443/#{path}?#{query}` にリダイレクト（301） |
   | HTTPS | 443 | `skilize-tg` に転送 |

   HTTPS リスナーの「デフォルト SSL/TLS 証明書」では、13.2 で発行した ACM 証明書を選択します。

4. 「ロードバランサーの作成」をクリックします（作成完了まで数分かかります）

5. 作成完了後、ALB の「DNS 名」をメモします（例: `skilize-alb-xxxxxxxx.ap-northeast-1.elb.amazonaws.com`）

---

### 13.6 DNS レコードを ALB に向ける

**Route 53 を使用している場合**

1. Route 53 →「ホストゾーン」→ 対象ドメインを開きます
2. 「レコードを作成」をクリックします
3. 以下を設定します:
   - **レコード名**: `skilize`（サブドメインを使う場合。ルートドメインの場合は空欄）
   - **レコードタイプ**: A
   - **エイリアス**: ON
   - **トラフィックのルーティング先**: Application Load Balancer → アジアパシフィック（東京） → `skilize-alb` を選択
4. 「レコードを作成」をクリックします

**外部 DNS を使用している場合**

DNS プロバイダーの管理画面で以下の CNAME レコードを追加します:

| 名前 | タイプ | 値 |
|---|---|---|
| `skilize` | CNAME | ALB の DNS 名（例: `skilize-alb-xxxxxxxx.ap-northeast-1.elb.amazonaws.com`） |

---

### 13.7 .env の FRONTEND_ORIGIN を更新する

EC2 に SSH 接続して実行します:

```bash
cd /home/ec2-user/skilize
nano .env
```

以下の行を HTTPS の URL に更新します:

```env
FRONTEND_ORIGIN=https://skilize.example.com
```

バックエンドを再起動して CORS 設定を反映します:

```bash
docker compose restart backend
```

---

### 13.8 動作確認

1. ブラウザで `http://skilize.example.com` にアクセスします
2. 自動的に `https://` にリダイレクトされることを確認します
3. ブラウザのアドレスバーに錠前アイコンが表示されることを確認します
4. ログインできることを確認します

---

## 14. HTTPS 対応（アプローチ C: Let's Encrypt + Certbot）

> **【重要】公開 FQDN が必須です**  
> Let's Encrypt は **自動更新の有無に関わらず**、インターネットから到達可能な公開 FQDN が必ず必要です。  
> EC2 の IP アドレスや内部ドメインへの証明書発行はできません。  
> ドメインの A レコードが EC2 に向いており、ポート 80 がインターネットから到達可能な状態で実施してください。

**特徴**
- 証明書の取得・更新が無料
- 追加の AWS インフラ不要（ALB 費用なし）
- 有効期限が 90 日（本番では自動更新必須、試用期間中は手動更新でも可）
- 公開 FQDN とポート 80 のインターネット到達性が必要

---

### 14.1 DNS を EC2 に向ける

ドメインの A レコードを EC2 のパブリック IP アドレスに向けます。

| レコードタイプ | 名前 | 値 |
|---|---|---|
| A | `skilize`（サブドメインの場合） | EC2 のパブリック IP アドレス |

DNS が反映されるまで最長 30 分かかります。反映を確認してから次の手順に進みます:

```bash
# EC2 上で実行（nslookup が使えない場合: sudo dnf install -y bind-utils）
nslookup skilize.example.com
# EC2 の IP アドレスが返れば OK
```

---

### 14.2 リポジトリの設定を変更する（ローカル PC で実施）

**infra/docker/nginx/nginx.prod.conf を更新する**

まず証明書取得のための一時設定（HTTP + ACME チャレンジ対応）に置き換えます。  
既存の内容全体を以下に置き換えてください:

```nginx
# HTTP サーバー（証明書取得前の一時設定）
server {
    listen 8080;
    server_tokens off;

    # ACME チャレンジ（certbot が証明書取得時に使用する）
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    root /usr/share/nginx/html;
    index index.html;

    add_header X-Frame-Options        "SAMEORIGIN"                      always;
    add_header X-Content-Type-Options "nosniff"                         always;
    add_header X-XSS-Protection       "1; mode=block"                   always;
    add_header Referrer-Policy        "strict-origin-when-cross-origin" always;

    gzip            on;
    gzip_vary       on;
    gzip_min_length 1024;
    gzip_types      text/plain text/css application/javascript application/json image/svg+xml;

    location /api/ {
        proxy_pass         http://backend:8080;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }

    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires   1y;
        add_header Cache-Control          "public, immutable";
        add_header X-Frame-Options        "SAMEORIGIN"                      always;
        add_header X-Content-Type-Options "nosniff"                         always;
        add_header X-XSS-Protection       "1; mode=block"                   always;
        add_header Referrer-Policy        "strict-origin-when-cross-origin" always;
        try_files  $uri =404;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

**infra/compose/docker-compose.prod.yml の nginx サービスに追記する**

`nginx` サービスに HTTPS ポートと certbot ボリュームを追加し、`certbot` サービスを追加します:

```yaml
  nginx:
    build:
      context: ../../
      dockerfile: infra/docker/frontend/Dockerfile.prod
      args:
        NODE_VERSION: ${NODE_VERSION:-20}
        HTTP_PROXY: ${HTTP_PROXY:-}
        HTTPS_PROXY: ${HTTPS_PROXY:-}
        NO_PROXY: ${NO_PROXY:-}
        CA_CERT_ENABLED: ${CA_CERT_ENABLED:-false}
    ports:
      - "80:8080"
      - "443:8443"                                      # 追加
    volumes:                                             # 追加
      - /home/ec2-user/certs/letsencrypt:/etc/letsencrypt:ro
      - /home/ec2-user/certs/www:/var/www/certbot:ro
    depends_on:
      backend:
        condition: service_healthy
    restart: always

  certbot:                                               # 追加（nginx の後に記述）
    image: certbot/certbot
    volumes:
      - /home/ec2-user/certs/letsencrypt:/etc/letsencrypt
      - /home/ec2-user/certs/www:/var/www/certbot
```

変更をコミット・プッシュします:

```bash
git add infra/docker/nginx/nginx.prod.conf infra/compose/docker-compose.prod.yml
git commit -m "HTTPS対応: certbot + nginx ACME設定追加"
git push origin main
```

---

### 14.3 EC2 で証明書を取得する

EC2 に SSH 接続して実行します。

**ステップ 1: certbot 用ディレクトリを作成する**

```bash
mkdir -p /home/ec2-user/certs/letsencrypt
mkdir -p /home/ec2-user/certs/www
```

**ステップ 2: 最新コードを取得してデプロイする**

```bash
cd /home/ec2-user/skilize
git pull origin main
docker compose up --build -d nginx
```

**ステップ 3: 証明書を取得する**

```bash
docker compose run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email <メールアドレス> \
  --agree-tos \
  --no-eff-email \
  -d skilize.example.com
```

`<メールアドレス>` は証明書失効通知を受け取るアドレスです。  
`-d` には実際のドメイン名を指定してください。

成功すると以下のように表示されます:

```
Successfully received certificate.
Certificate is saved at: /home/ec2-user/certs/letsencrypt/live/skilize.example.com/fullchain.pem
Key is saved at:         /home/ec2-user/certs/letsencrypt/live/skilize.example.com/privkey.pem
```

---

### 14.4 HTTPS 対応の nginx 設定に切り替える（ローカル PC で実施）

`infra/docker/nginx/nginx.prod.conf` を HTTPS 対応版に更新します。  
`skilize.example.com` はすべて実際のドメイン名に変更してください:

```nginx
# HTTP → HTTPS リダイレクト
server {
    listen 8080;
    server_tokens off;

    # ACME チャレンジ（証明書更新時に引き続き使用する）
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

# HTTPS サーバー
server {
    listen 8443 ssl;
    server_tokens off;

    ssl_certificate     /etc/letsencrypt/live/skilize.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/skilize.example.com/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache   shared:SSL:10m;
    ssl_session_timeout 1d;

    root /usr/share/nginx/html;
    index index.html;

    add_header Strict-Transport-Security "max-age=63072000"           always;
    add_header X-Frame-Options           "SAMEORIGIN"                  always;
    add_header X-Content-Type-Options    "nosniff"                     always;
    add_header X-XSS-Protection          "1; mode=block"               always;
    add_header Referrer-Policy           "strict-origin-when-cross-origin" always;

    gzip            on;
    gzip_vary       on;
    gzip_min_length 1024;
    gzip_types      text/plain text/css application/javascript application/json image/svg+xml;

    location /api/ {
        proxy_pass         http://backend:8080;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto https;
        proxy_read_timeout 60s;
    }

    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires   1y;
        add_header Cache-Control             "public, immutable";
        add_header Strict-Transport-Security "max-age=63072000"           always;
        add_header X-Frame-Options           "SAMEORIGIN"                  always;
        add_header X-Content-Type-Options    "nosniff"                     always;
        add_header X-XSS-Protection          "1; mode=block"               always;
        add_header Referrer-Policy           "strict-origin-when-cross-origin" always;
        try_files  $uri =404;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

コミット・プッシュします:

```bash
git add infra/docker/nginx/nginx.prod.conf
git commit -m "HTTPS: nginx 設定を SSL 対応版に更新"
git push origin main
```

EC2 でデプロイします:

```bash
cd /home/ec2-user/skilize
git pull origin main
docker compose up --build -d nginx
```

---

### 14.5 .env を更新する

```bash
nano /home/ec2-user/skilize/.env
```

`FRONTEND_ORIGIN` を HTTPS に変更します:

```env
FRONTEND_ORIGIN=https://skilize.example.com
```

バックエンドを再起動します:

```bash
docker compose restart backend
```

---

### 14.6 証明書の自動更新を設定する（本番運用では必須）

Let's Encrypt 証明書の有効期限は **90 日**です。

> **試用期間中**: 自動更新の設定は任意です。ただし取得から **60 日を目安**に「14.7 手動更新」の手順で更新してください（期限切れするとアクセス不能になります）。  
> **本番運用開始後**: 必ず自動更新を設定してください。

EC2 で cron を設定します:

```bash
crontab -e
```

以下を追記します（`i` で編集モード → 貼り付け → `Esc` → `:wq` で保存）:

```
0 3 * * * cd /home/ec2-user/skilize && docker compose run --rm certbot renew --quiet && docker compose exec nginx nginx -s reload
```

毎日 AM 3:00 に実行されます。certbot は有効期限が 30 日未満になった場合のみ更新するため、毎日実行しても問題ありません。

---

### 14.7 手動更新（試用期間中の更新手順）

有効期限が切れる前（取得から 60 日後を目安）に EC2 上で実行します:

```bash
cd /home/ec2-user/skilize

# 現在の証明書の有効期限を確認する
docker compose run --rm certbot certificates

# 証明書を更新する（有効期限 30 日未満の場合のみ更新される）
docker compose run --rm certbot renew

# nginx をリロードして新しい証明書を適用する
docker compose exec nginx nginx -s reload
```

---

### 14.8 動作確認

1. ブラウザで `http://skilize.example.com` にアクセスします
2. 自動的に `https://` にリダイレクトされることを確認します
3. ブラウザのアドレスバーに錠前アイコンが表示されることを確認します
4. ログインできることを確認します

---

## 15. 運用手順

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

### DB クライアントツールからの接続

EC2 上の PostgreSQL（ポート 5432）はセキュリティグループで外部に公開していません。  
ローカル PC の GUI ツールから接続するには **SSH トンネリング**が必要です。

SSH トンネリングとは、SSH 接続を経由してリモートのポートをローカルに転送する仕組みです。  
例えば「ローカル PC の 15432 → EC2 の localhost:5432」と転送することで、DB ツールが `localhost:15432` に接続するだけで EC2 上の PostgreSQL にアクセスできます。

#### 接続情報

| 項目 | 値 |
|---|---|
| ホスト（SSH トンネル越し） | `localhost` |
| ポート（SSH トンネル越し） | `15432`（または任意の空きポート） |
| データベース名 | `skilize` |
| ユーザー名 | `skilize` |
| パスワード | `.env` の `DB_PASSWORD` の値 |

---

#### A. psql（SSH 接続後にコマンドで操作）

追加設定なしで使えます。EC2 に SSH 接続してからコンテナ内の psql を起動します。

```bash
cd /home/ec2-user/skilize
docker compose exec db psql -U skilize -d skilize
```

接続後の基本操作:

```sql
\dt                    -- テーブル一覧を表示
\d users               -- テーブル定義を表示
SELECT user_id, role, is_active FROM users;
\q                     -- 終了
```

---

#### B. GUI ツールの前提設定（SSH トンネル用のポートマッピング）

DBeaver・pgAdmin などの GUI ツールが SSH トンネルを使って EC2 の PostgreSQL に到達するには、  
Docker コンテナのポート 5432 が **EC2 ホストの localhost** に公開されている必要があります。

まず現在の設定を確認します（EC2 上で実行）:

```bash
grep -A 10 "^  db:" /home/ec2-user/skilize/infra/compose/docker-compose.prod.yml
```

`ports:` の記述がない場合は以下を追加します:

```bash
nano /home/ec2-user/skilize/infra/compose/docker-compose.prod.yml
```

`db` サービスに `ports` を追加します（`127.0.0.1` にのみバインドし、外部には公開しません）:

```yaml
  db:
    ports:
      - "127.0.0.1:5432:5432"
```

> **セキュリティ**: `127.0.0.1:5432:5432` は EC2 のローカルホストにのみバインドします。  
> セキュリティグループでポート 5432 を開けていない限り、外部から直接アクセスすることはできません。

設定追加後、DB コンテナを再起動します:

```bash
cd /home/ec2-user/skilize
docker compose up -d db
```

---

#### C. pgAdmin

PostgreSQL 公式の GUI ツールです（pgAdmin 4）。「SSH Tunnel」タブで SSH トンネルを直接設定できます。

**インストール**: [pgadmin.org](https://www.pgadmin.org) からダウンロードしてインストールします

**接続設定**

1. 左ペインの「Servers」を右クリック →「Register」→「Server...」を選択します
2. 「General」タブで「Name」に `Skilize Prod` などを入力します
3. 「Connection」タブに以下を入力します:

   | 項目 | 値 |
   |---|---|
   | Host name/address | `localhost` |
   | Port | `5432` |
   | Maintenance database | `skilize` |
   | Username | `skilize` |
   | Password | `.env` の `DB_PASSWORD` の値 |

4. 「SSH Tunnel」タブを開き、「Use SSH tunneling」を有効にして以下を設定します:

   | 項目 | 値 |
   |---|---|
   | Tunnel host | EC2 のパブリック IP アドレス |
   | Tunnel port | `22` |
   | Username | `ec2-user` |
   | Authentication | `Identity file` |
   | Identity file | `skilize-key.pem` のパスを指定 |

5. 「Save」をクリックします

---

#### D. A5:SQL Mk-2

Windows 向けの無料 DB クライアントツールです。PostgreSQL・SSH トンネルに対応しています。

**インストール**: [a5m2.mmatsubara.com](https://a5m2.mmatsubara.com/) からダウンロードしてインストールします

**接続設定**

1. メニュー「データベース」→「データベースの追加と削除」を開きます
2. 「追加」ボタンをクリックし、「PostgreSQL（直接接続）」を選択します
3. 「基本」タブに以下を入力します:

   | 項目 | 値 |
   |---|---|
   | ホスト名 | `localhost` |
   | ポート番号 | `5432` |
   | データベース名 | `skilize` |
   | ユーザーID | `skilize` |
   | パスワード | `.env` の `DB_PASSWORD` の値 |

4. 「SSH2トンネル」タブを開き、以下を設定します:

   | 項目 | 値 |
   |---|---|
   | SSH2トンネルを使用する | チェックを入れる |
   | ホスト | EC2 のパブリック IP アドレス |
   | ポート | `22` |
   | ユーザー名 | `ec2-user` |
   | 認証方式 | 「公開鍵認証」を選択 |
   | 秘密鍵ファイル | `skilize-key.pem` のパスを指定 |

5. 「テスト接続」をクリックして接続できることを確認し、「OK」をクリックします

---

#### E. VSCode（Database Client 拡張機能）

VSCode の拡張機能「Database Client」を使う方法です。SSH トンネルを GUI 上で設定できます。

**インストール**

1. VSCode の拡張機能パネル（`Ctrl+Shift+X`）を開きます
2. `Database Client`（発行元: Weijan Chen）を検索してインストールします
   - 拡張機能 ID: `cweijan.vscode-database-client2`

**接続設定**

1. 左サイドバーに追加されたデータベースアイコンをクリックします
2. 「+」ボタン →「PostgreSQL」を選択します
3. 以下を入力します:

   | 項目 | 値 |
   |---|---|
   | Host | `localhost` |
   | Port | `5432` |
   | Username | `skilize` |
   | Password | `.env` の `DB_PASSWORD` の値 |
   | Database | `skilize` |

4. 「SSH」セクションを展開し、以下を設定します:

   | 項目 | 値 |
   |---|---|
   | SSH Host | EC2 のパブリック IP アドレス |
   | SSH Port | `22` |
   | SSH Username | `ec2-user` |
   | Private Key Path | `skilize-key.pem` のパスを指定 |

5. 「Connect」をクリックします

---

#### F. VSCode（PostgreSQL 拡張機能）

VSCode の拡張機能「PostgreSQL」を使う方法です。SSH トンネルのサポートがないため、先に手動でトンネルを確立してから接続します。

**インストール**

1. VSCode の拡張機能パネル（`Ctrl+Shift+X`）を開きます
2. `PostgreSQL`（発行元: Chris Kolkman）を検索してインストールします
   - 拡張機能 ID: `ckolkman.vscode-postgres`

**ステップ 1: SSH トンネルを張る**

別のターミナルで以下を実行し、接続したままにしておきます（`Ctrl+C` でトンネルが閉じます）:

```bash
# Mac/Linux
ssh -L 15432:localhost:5432 -i ~/.ssh/skilize-key.pem ec2-user@<EC2のIP> -N

# Windows PowerShell
ssh -L 15432:localhost:5432 -i "C:\Users\<ユーザー名>\.ssh\skilize-key.pem" ec2-user@<EC2のIP> -N
```

`-N`: コマンドを実行せず SSH トンネルのみを確立するオプションです。

**ステップ 2: VSCode から接続する**

1. コマンドパレット（`Ctrl+Shift+P`）を開き、「PostgreSQL: Add Connection」を実行します
2. 表示されるプロンプトに順番に入力します:

   | プロンプト | 入力値 |
   |---|---|
   | Hostname | `localhost` |
   | PostgreSQL port | `15432`（SSH トンネルのローカルポート） |
   | Database | `skilize` |
   | Username | `skilize` |
   | Password | `.env` の `DB_PASSWORD` の値 |
   | SSL mode | `disable` |

3. 左サイドバーの「PostgreSQL Explorer」に接続が追加されたことを確認します

---

#### 注意事項

- 本番 DB に対して UPDATE / DELETE などを直接実行する場合は、事前に必ずバックアップを取得してください
- 使用後は SSH トンネルを終了してください（`Ctrl+C` またはターミナルを閉じる）
- `DB_PASSWORD` は `.env` ファイルを直接確認するか、管理者に問い合わせてください

---

### ログの確認

**Docker コンテナのログ（コンソール出力）**

```bash
# リアルタイムログ（全サービス）
docker compose logs -f

# 直近 100 行のみ表示
docker compose logs --tail=100 backend
```

**アプリケーションログファイル**

バックエンドはコンテナ内の `/var/log/skilize/application.log` にもログを出力します。  
日次ローテーション（30日保持）のため、過去ログも参照できます。

```bash
# 最新ログをリアルタイム表示
docker compose exec backend tail -f /var/log/skilize/application.log

# 直近 100 行を表示
docker compose exec backend tail -100 /var/log/skilize/application.log

# エラーのみ絞り込み
docker compose exec backend grep "ERROR" /var/log/skilize/application.log

# 特定の requestId で絞り込み（問い合わせ追跡）
docker compose exec backend grep "<requestId>" /var/log/skilize/application.log
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

## 16. トラブルシューティング

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

### Flyway マイグレーションが実行されない（テーブルが作成されない）

**症状**: バックエンドは起動するが `\dt` でテーブルが存在しない。ログに Flyway の出力がない。

**原因**: Spring Boot 4.x の `FlywayAutoConfiguration` が起動しないケースが確認されています。  
本プロジェクトでは `FlywayConfig.java`（`shared/infrastructure/`）で明示的に Flyway Bean を定義することで回避済みです。

もし発生した場合はバックエンドのビルドが古いイメージを使っていないか確認します:

```bash
docker compose build backend
docker compose up -d backend
sleep 60
docker compose logs backend | grep -i flyway
```

`Successfully applied N migrations` が表示されれば正常です。

---

### Flyway マイグレーションエラー

```bash
docker compose logs backend | grep -i flyway
```

**原因**: マイグレーション済みのファイル（`V1__` 〜）を後から編集した場合に発生します。  
**対処**: 編集したファイルを元に戻すか、新しいバージョン（`V9__xxx.sql` など）で変更を追加します。

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

アプリの自動起動を設定するには、systemd サービスユニットを作成します（Amazon Linux 2023 は systemd ベースのため `/etc/rc.local` より確実です）:

```bash
sudo vi /etc/systemd/system/skilize.service
```

以下の内容を貼り付けます:

```ini
[Unit]
Description=Skilize Docker Compose Application
Requires=docker.service
After=docker.service network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/home/ec2-user/skilize
ExecStart=/usr/bin/docker compose up -d
ExecStop=/usr/bin/docker compose down
TimeoutStartSec=300

[Install]
WantedBy=multi-user.target
```

保存後、サービスを有効化します:

```bash
sudo systemctl daemon-reload
sudo systemctl enable skilize.service
sudo systemctl start skilize.service

# 状態確認
sudo systemctl status skilize.service
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


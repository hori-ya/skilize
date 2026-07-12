---
paths:
  - "**/*.java"
---

# Backend Code Style Rules

汎用的なサーバーサイド（バックエンド）実装における設計・命名規約。言語・フレームワークを問わず適用できる原則を記載する。

---

# レイヤードアーキテクチャ

feature（機能）単位でパッケージ/モジュールを分割し（package by feature）、各 feature 内部を責務ごとに以下の層へ分離する。

- presentation（Controller / Router。リクエスト検証・HTTPハンドリング）
- application（ユースケース。トランザクション境界・ワークフロー制御）
- domain（ビジネスルール・エンティティ・値オブジェクト・リポジトリインターフェース）
- infrastructure（DB実装・外部API連携・フレームワーク依存の実装詳細）

## 依存方向

```
presentation → application → domain
infrastructure → domain / application を実装する
```

禁止:
- domain が infrastructure や presentation に依存する
- feature 間の直接依存（共有が必要な場合は shared に切り出す）

## 各層の責務

| 層 | 責務 | 禁止事項 |
|---|---|---|
| presentation | リクエスト受付・入力検証・レスポンス整形 | ビジネスロジック、DB直接アクセス、トランザクション管理 |
| application | ユースケース制御・トランザクション境界・feature間の調整 | DB実装詳細への直接依存、肥大化したフレームワーク依存 |
| domain | ビジネスルール・不変条件・ドメインモデル | フレームワーク依存、DB実装詳細、外側の層への依存 |
| infrastructure | DB・外部API・永続化・技術的関心事の実装 | ビジネスロジックの実装 |

---

# 永続化レイヤーの分離（ORMを使う場合）

ORM（JPA/Hibernate等）を採用する場合、domain 層は ORM フレームワークに一切依存させず、永続化の実装詳細は infrastructure 層に閉じ込める。

```text
domain/
├── model/       ← 純粋なドメインモデル（ORMアノテーションなし）
└── repository/  ← Repository interface のみ（ORM実装クラスを継承しない）

infrastructure/persistence/
├── entity/      ← ORMエンティティ（アノテーションはここにのみ記述する）
├── repository/  ← ORM実装のリポジトリ + domain.repository の実装クラス
└── mapper/      ← エンティティ⇄ドメインモデルの変換
```

- ドメインモデルは「新規生成用ファクトリ」と「永続化状態からの復元用ファクトリ」を分けて用意する（後者は変換Mapperからのみ呼び出す）
- 他featureのモデルへの参照は、参照先featureが既にこの構成に従っている場合のみドメインモデルとして保持する。参照先がまだ移行されていない場合は、暫定的にORMエンティティを参照してよい（参照先の移行完了時に解消する）
- 単なるID以外の用途がない逆参照（子から親への参照等）は、オブジェクト全体ではなくID（スカラー値）で保持し、不要な変換・追加クエリを避ける

---

# 依存性注入

- コンストラクタインジェクションのみを使用する
- フィールドインジェクション（フィールドへ直接アノテーション付与する等）は禁止する

---

# トランザクション境界

- トランザクション境界は application 層に置く
- presentation 層・infrastructure 層でトランザクションを直接制御しない

---

# 共通化ルール（Commonization）

1. まず feature 内部に実装する（早すぎる共通化をしない）
2. 3回以上重複した場合のみ shared / common への切り出しを検討する
3. ビジネスロジックを shared / common に置かない（技術的関心事のみを置く）
4. 目的が不明瞭な util クラスの乱立を禁止する（例: `CommonUtil`, `BusinessUtil` のような汎用すぎる名前）
5. 共通化後も feature への逆依存（shared が特定 feature に依存すること）を作らない

---

# データ受け渡しオブジェクトの命名

責務ごとに以下のパターンで命名し、専用の入出力型を用意する（万能 DTO を使い回さない）。詳細は [naming-conventions.md](../naming-conventions.md) も参照。

| 種別 | 命名パターン | 役割 |
|---|---|---|
| HTTPリクエスト入力 | `XxxRequest` | Controller/Router が受け取る入力・バリデーション対象 |
| HTTPレスポンス出力 | `XxxResponse` | Controller/Router が組み立てて返す出力 |
| ユースケース入力 | `XxxCommand` | application 層への書き込み系入力 |
| ユースケース出力 | `XxxQueryResult` | application 層が返す参照系結果 |
| 変換コンポーネント | `XxxMapper` | Request → Command 等の変換専用コンポーネント |

判断基準:
- 「誰が組み立てるか」→ presentation層が組み立てる場合は Response、application層が組み立てる場合は QueryResult
- 「誰が受け取るか」→ 外部からの入力は Request、application層への入力は Command

禁止:
- 汎用的な `XxxDto` という命名（責務が曖昧になるため）
- application層のメソッド引数に Request クラスを直接渡すこと（必ず Mapper で Command に変換する）
- application層が presentation パッケージをインポートすること

---

# 命名規則

| 対象 | 規則 | 例 |
|---|---|---|
| クラス | PascalCase | `OrderService`, `CreateOrderRequest` |
| メソッド・フィールド | camelCase | `findById`, `createdAt` |
| DB テーブル・カラム | snake_case | `orders`, `created_at` |
| 定数 | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT` |
| ドメインモデル | `Xxx` | `Order`, `User` |
| Repository（インターフェース） | `XxxRepository` | `OrderRepository` |
| ORMエンティティ | `XxxEntity` | `OrderEntity` |
| ORM実装のリポジトリ | `XxxJpaRepository`（JPAの場合） | `OrderJpaRepository` |
| Repository実装 | `XxxRepositoryImpl` | `OrderRepositoryImpl` |
| エンティティ⇄ドメイン変換 | `XxxPersistenceMapper` | `OrderPersistenceMapper` |

---

# コントローラー / サービス設計

- Controller/Router は HTTP 処理・入力検証・レスポンス整形に専念する（目安: 1ファイル100行以内）
- application層のサービスは単一ユースケース単位で実装し、肥大化した fat service を作らない
- Repository は DB アクセスを集約し、SQL/ORM 実装はここに閉じ込める

---

# ログ出力ルール

詳細は [logging.md](../logging.md) を参照。

- 標準出力への直接書き込み（`print` 等）は禁止し、構造化ロギングフレームワークを使用する
- パスワード・トークン・個人情報はログに出力しない（詳細は [security.md](../security.md) を参照）

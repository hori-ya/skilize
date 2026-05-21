# Refactor: Feature内部構成の統一（Spring Boot）

## 目的
既存プロジェクトはすでに feature単位で分割済みのため、  
各feature内部のディレクトリ構成と責務定義のみを統一する。

DTOという曖昧な概念を廃止し、責務ベース（request/response/command/query）へ移行する。

---

# ■ 対象スコープ

各feature配下のみ変更対象とする。

例：
- user/
- order/
- payment/

feature間の構造は変更しない。

---

# ■ feature内部の統一構成

各featureは必ず以下の4層構造に統一する：
```
presentation
application
domain
infrastructure
```

---

# ■ 各層の標準構成

## ① presentation層（HTTP層）

責務：
- Controller
- HTTP入出力のみ
- ビジネスロジック禁止

構成：
```
presentation
├── XxxController.java
├── request
│ ├── XxxCreateRequest.java
│ ├── XxxUpdateRequest.java
│ └── XxxGetRequest.java（必要に応じて）
│
└── response
├── XxxResponse.java
├── XxxDetailResponse.java
└── XxxListResponse.java
```

---

## ② application層（ユースケース層）

責務：
- ユースケース制御
- 業務フローの組み立て
- domain呼び出しの調整

構成：
```
application
├── service
│ ├── XxxApplicationService.java
│ └── XxxQueryService.java（必要なら）
│
├── command
│ ├── XxxCreateCommand.java
│ ├── XxxUpdateCommand.java
│ └── XxxDeleteCommand.java
│
├── query
│ └── XxxQueryResult.java
│
└── mapper
└── XxxApplicationMapper.java
```

---

## ③ domain層（ビジネスルール）

責務：
- 業務ルールの中心
- 外部技術に依存しない

構成：
```
domain
├── model
│ ├── Xxx.java（Entity）
│ ├── XxxId.java（Value Object）
│ ├── XxxEmail.java（Value Object）
│ └── XxxStatus.java（Enum）
│
├── repository
│ └── XxxRepository.java（interface）
│
└── service
└── XxxDomainService.java（必要時のみ）
```

---

## ④ infrastructure層（技術実装）

責務：
- DBアクセス
- 外部API
- メッセージング
- 技術詳細

構成：
```
infrastructure
├── persistence
│ ├── jpa
│ │ ├── XxxJpaRepository.java
│ │ └── XxxJpaEntity.java（分離する場合）
│ │
│ └── XxxRepositoryImpl.java
│
├── external
│ ├── XxxApiClient.java
│
├── config
│ ├── JpaConfig.java
│
└── cache（必要なら）
└── XxxCacheRepository.java
```

---

# ■ 命名ルール（重要）

## DTOは禁止

DTOという名前は使用禁止とする。

代わりに以下を使用：

| 目的 | 命名 |
|------|------|
| API入力 | XxxRequest |
| API出力 | XxxResponse |
| 書き込み処理 | XxxCommand |
| 読み取り結果 | XxxQueryResult |

---

# ■ 依存ルール

依存方向は必ず下向きのみ：
```
presentation
↓
application
↓
domain
↓
infrastructure（実装のみ）
```

---

## NGルール

- domain → infrastructure 依存禁止
- application → presentation 依存禁止
- domain → application 依存禁止

---

# ■ データフロー

## 書き込み（Command）
```
Controller
→ Request
→ Command
→ ApplicationService
→ Domain
→ Repository
→ Infrastructure
→ DB
```

---

## 読み取り（Query）
```
Controller
→ Request
→ QueryService
→ Repository / DB
→ QueryResult
→ Response
```

---

# ■ リファクタリング指示

## ① DTOの整理

- DTOという名前のクラスをすべて削除またはリネーム
- 以下へ分類する：
  - Request（入力）
  - Response（出力）
  - Command（書き込み）
  - QueryResult（読み取り）

---

## ② パッケージ移動

- すべてfeature内部に収める
- 層ごとの責務に従って移動する

---

## ③ Controller修正

- ControllerはRequest/Responseのみ扱う
- ServiceにはCommand/Queryを渡すよう変換を挟む

---

## ④ Mapper導入

- request → command の変換は mapper に集約する
- Controller内に変換ロジックを散らさない

---

# ■ ゴール

- feature内部構造の完全統一
- DTO依存の排除
- 責務ベース設計への移行
- 可読性・保守性の向上
- レイヤー境界の明確化


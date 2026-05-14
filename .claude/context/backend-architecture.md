# Spring Boot Architecture Rules

このプロジェクトのバックエンドアーキテクチャルールを定義する。実装時は必ず遵守すること。

---

# Goal

* 小規模〜中規模で保守しやすい構成
* 将来的に大規模開発へ拡張可能
* DDDへ段階的に移行可能
* package by feature を採用
* feature内部を layer 分離
* 責務分離を徹底

---

# Package Structure

各 feature は以下の構成を持つ。

```text
feature
├── presentation
├── application
├── domain
└── infrastructure
```

例:

```text
user
├── presentation
├── application
├── domain
└── infrastructure
```

---

# Full Project Structure

```text
src/main/java/com/example/app

├── shared
│   ├── domain
│   ├── infrastructure
│   ├── presentation
│   └── application
│
├── user
├── order
├── billing
└── auth
```

---

# Layer Responsibilities

## presentation

責務:

* Controller
* Request/Response DTO
* Validation
* HTTP handling
* API response formatting

禁止:

* business logic
* SQL
* transaction control

---

## application

責務:

* UseCase
* ApplicationService
* transaction boundary
* workflow orchestration
* feature coordination

ルール:

* @Transactional は application に置く
* business workflow を記述
* domain を組み合わせる

禁止:

* SQL直接実装
* framework依存の肥大化

---

## domain

責務:

* business rule
* domain model
* aggregate
* value object
* repository interface
* invariant

ルール:

* domain は純粋に保つ
* Spring Frameworkへ極力依存しない
* JPA実装詳細を持ち込まない

禁止:

* Controller依存
* infrastructure依存
* SQL実装

---

## infrastructure

責務:

* JPA
* Repository implementation
* external API
* Redis
* S3
* security implementation
* persistence

ルール:

* 技術詳細を閉じ込める
* domain/application を実装する

---

# Dependency Rules

依存方向を厳守:

```text
presentation
    ↓
application
    ↓
domain

infrastructure → domain/application
```

禁止:

* domain → infrastructure
* domain → presentation
* feature間の直接依存

---

# Shared Rules

shared は最小限にする。

許可:

```text
shared/domain
- ValueObject
- DomainEvent
- CommonException

shared/infrastructure
- Security
- Config
- Logging
- Persistence Config
```

禁止:

* 業務ロジック
* feature固有ロジック
* 巨大common化

---

# Commonization Rules

共通化ルールを厳守する。

## Rule 1

まず feature 内部へ実装する。

## Rule 2

3回以上重複した場合のみ shared 化を検討する。

## Rule 3

business logic を shared/infrastructure に置かない。

## Rule 4

util クラス乱立禁止。

禁止例:

```text
common/util/BusinessUtil
common/service/CommonService
```

## Rule 5

共通化後も feature 依存を作らない。

---

# Coding Rules

## Controller

* HTTP処理のみ
* 100行以内推奨
* validation担当
* Response DTOを返す

## Application Service / UseCase

* 単一ユースケース単位
* fat service禁止
* transaction管理

## Domain

* business rule中心
* ValueObject積極利用
* 不変条件を保持

## Repository

* DBアクセスを集約
* SQL/JPAはここだけ

---

# DTO Rules

* EntityをAPIへ直接返さない
* Request/Response DTOを分離
* DTO変換は mapper または assembler を利用

---

# DI Rules

* constructor injection only
* field injection禁止

禁止:

```java
@Autowired
private UserService service;
```

許可:

```java
private final UserService service;

public UserController(UserService service) {
    this.service = service;
}
```

---

# Transaction Rules

transaction boundary は application layer に置く。

例:

```java
@Transactional
public void createUser(CreateUserCommand command)
```

禁止:

* Controllerでtransaction
* Repositoryで業務transaction管理

---

# Naming Rules

UseCase:

```text
CreateUserUseCase
UpdateOrderUseCase
```

Repository:

```text
UserRepository
JpaUserRepository
```

DTO:

```text
CreateUserRequest
UserResponse
```

---

# Refactoring Rules

リファクタ時は以下を遵守:

1. まず package structure を整理
2. DBアクセスを repository へ分離
3. transaction を application へ移動
4. business rule を domain へ移動
5. DTO分離
6. fat service分割

---

# Important Principles

最重要なのは:

* 責務分離
* dependency direction
* feature isolation
* maintainability
* readability
* testability

フォルダ構成そのものより、
依存方向と責務分離を優先すること。

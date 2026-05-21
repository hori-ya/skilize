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
src/main/java/com/skilize

├── shared
│   ├── domain/exception/        ← AuthException, GoalIncompleteException
│   ├── infrastructure/          ← SecurityConfig, JwtUtil, JwtAuthenticationFilter, InitialPasswordFilter
│   └── presentation/            ← GlobalExceptionHandler, ErrorResponse, ValidationErrorResponse
│
├── auth
│   ├── presentation/
│   │   ├── AuthController.java
│   │   └── request/             ← LoginRequest, ChangePasswordRequest
│   └── application/
│       ├── AuthService.java
│       ├── command/             ← LoginCommand, ChangePasswordCommand
│       ├── query/               ← LoginQueryResult, MeQueryResult
│       └── mapper/              ← AuthApplicationMapper
│
├── user
│   ├── presentation/
│   │   ├── UserController.java
│   │   ├── request/             ← CreateUserRequest, UpdateUserRequest
│   │   └── response/            ← UserResponse, TeamMemberResponse, MemberInventorySummaryResponse, FiscalYearRef, ResetPasswordResponse
│   ├── domain/                  ← User, Role, UserRepository
│   └── infrastructure/          ← UserDetailsServiceImpl
│
├── inventory
│   ├── presentation/
│   │   ├── InventoryController.java
│   │   ├── request/             ← CreateInventoryRequest, ItSkillDetailsRequest, ItSkillDetailItem, QualificationDetailsRequest, QualificationDetailItem, SeminarDetailsRequest, SeminarDetailItem, RemarksPatchRequest, GoalsRequest, GoalItem, GoalReviewUpdateRequest, GoalReviewUpdateItem
│   │   └── response/            ← InventorySummaryResponse, InventoryDetailResponse, ItSkillDetailsResponse, ItSkillDetailResponse, QualificationDetailsResponse, QualificationDetailResponse, SeminarDetailsResponse, SeminarDetailResponse, RemarksPatchResponse, SubmitResponse, GoalsResponse, GoalResponse, GoalCompleteResponse, GoalReviewCompleteResponse, FiscalYearRef
│   ├── application/
│   │   ├── InventoryService.java
│   │   ├── command/             ← ItSkillDetailCommand, QualificationDetailCommand, SeminarDetailCommand, GoalCommand, GoalReviewUpdateCommand
│   │   ├── query/               ← ComparisonQueryResult, GoalReviewQueryResult
│   │   └── mapper/              ← InventoryApplicationMapper
│   └── domain/                  ← エンティティ・Repository・列挙型
│
├── master
│   ├── presentation/
│   │   ├── MasterController.java
│   │   ├── request/             ← SkillLevelRequest, ItSkillRequest, ItSkillCategoryRequest, ItSkillCategoryUpdateRequest, QualificationRequest, AdSeminarRequest, PromoteItSkillRequest, PromoteQualificationRequest, SimpleCategoryRequest
│   │   └── response/            ← SkillLevelResponse, ItSkillResponse, ItSkillCategoryResponse, QualificationResponse, QualificationCategoryResponse, AdSeminarResponse, AdSeminarCategoryResponse, SeminarCategoryResponse, CustomUnregisteredResponse
│   └── domain/                  ← マスタエンティティ・Repository
│
├── fiscalyear
│   ├── presentation/
│   │   ├── FiscalYearController.java
│   │   ├── request/             ← FiscalYearRequest, FiscalYearSettingsRequest
│   │   └── response/            ← FiscalYearResponse, FiscalYearSettingsResponse
│   └── domain/                  ← FiscalYear, FiscalYearSettings, Repository
│
├── dashboard
│   └── presentation/
│       ├── DashboardController.java
│       └── response/            ← DashboardResponse（nested UserInfo・FiscalYearRef・CurrentInventoryInfo）
│
├── charts
│   ├── presentation/
│   │   ├── ChartController.java
│   │   └── (response なし)      ← QueryResult を直接返す
│   └── application/
│       ├── ChartService.java
│       └── query/               ← RadarQueryResult, GrowthQueryResult, HeatmapQueryResult, TimelineQueryResult
│
├── expectation
│   ├── presentation/
│   │   ├── ExpectationController.java
│   │   └── request/             ← SaveExpectationRequest
│   ├── application/
│   │   ├── ExpectationService.java
│   │   └── query/               ← ExpectationQueryResult
│   └── domain/                  ← UserExpectation・UserExpectationRepository
│
├── interview
│   ├── presentation/
│   │   ├── InterviewController.java
│   │   ├── request/             ← SaveInterviewRequest, DetailNoteRequest
│   │   └── response/            ← InterviewResponse, DetailNoteResponse
│   ├── application/
│   │   ├── InterviewService.java
│   │   └── command/             ← DetailNoteCommand
│   └── domain/                  ← InventoryInterview・InterviewDetailNote・DetailType・Repository
│
└── ai
    ├── presentation/
    │   └── AiAnalysisController.java  ← QueryResult を直接返す
    ├── application/
    │   ├── AiAnalysisService.java
    │   ├── AiAnalysisEventListener.java
    │   └── query/               ← AiAnalysisQueryResult
    └── domain/                  ← AiCareerAnalysis・AiAnalysisStatus・AiCareerAnalysisRepository
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

## 配置ルール（責務別パッケージ分離）

`feature/dto/` は廃止。DTO は責務に応じて以下の4カテゴリに分類し、それぞれ専用パッケージへ配置する。

| 種別 | 配置先 | 役割 |
|---|---|---|
| `XxxRequest` | `presentation/request/` | HTTP 入力・バリデーション。Controller が受け取る |
| `XxxResponse` | `presentation/response/` | Controller が組み立てて返す HTTP 出力 |
| `XxxCommand` | `application/command/` | Service への Write オペレーション入力 |
| `XxxQueryResult` | `application/query/` | Service が組み立てて返すクエリ結果 |
| `XxxApplicationMapper` | `application/mapper/` | `XxxRequest` → `XxxCommand` 変換コンポーネント |

## ファイル構成例

```text
feature/
├── presentation/
│   ├── FeatureController.java
│   ├── request/
│   │   ├── XxxRequest.java        ← @RequestBody で受け取る入力
│   │   └── XxxItem.java           ← リスト内の入力要素
│   └── response/
│       ├── XxxResponse.java       ← Controller が構築して返す出力
│       └── XxxRef.java            ← ネスト用参照オブジェクト
└── application/
    ├── FeatureService.java
    ├── command/
    │   └── XxxCommand.java        ← Service に渡す Write 入力
    ├── query/
    │   └── XxxQueryResult.java    ← Service が構築して返すクエリ結果
    └── mapper/
        └── XxxApplicationMapper.java  ← Request → Command 変換
```

## どちらに置くかの判断基準

```text
「誰が組み立てるか」で決まる:

Controller が組み立てる  → presentation/response/
Service が組み立てる     → application/query/

「誰が受け取るか」で決まる:
HTTP から来る入力       → presentation/request/
Service への入力        → application/command/
```

具体的な判断例:
- `InventorySummaryResponse` → Controller 内で `Inventory.from()` を呼ぶ → `presentation/response/`
- `ComparisonQueryResult` → Service 内で計算・構築する → `application/query/`
- `ItSkillDetailsRequest` → `@RequestBody` で受け取る → `presentation/request/`
- `ItSkillDetailCommand` → Service の引数として渡す → `application/command/`

## 命名規則

```text
XxxRequest         ← HTTP リクエストボディ（@RequestBody）
XxxItem            ← リクエスト内のリスト要素
XxxResponse        ← HTTP レスポンスボディ（Controller 構築）
XxxRef             ← レスポンス内の参照オブジェクト（ネスト用）
XxxCommand         ← Service への Write 入力
XxxQueryResult     ← Service が返すクエリ結果
XxxApplicationMapper ← Request → Command 変換（@Component）
```

`XxxDto` という命名は使用しない（責務が曖昧なため廃止）。

## 実装ルール

* Java `record` を使用する
* バリデーションは `jakarta.validation` アノテーション + `@Valid`（Request クラスのみ）
* 特定クラス内でしか使わないネスト型は、親ファイル内に `record` としてまとめて定義してよい
* `presentation → application` の依存は禁止。Service は `Request` クラスを直接インポートしない
* Mapper（`XxxApplicationMapper`）は `@Component` で DI し、Controller でコンストラクタ注入して使う
* `static from(Entity)` ファクトリメソッドは Response / QueryResult に定義してよい（domain エンティティへの依存は許容）

## レイヤー依存の厳守

```text
【禁止】Service が presentation パッケージをインポートする
【禁止】Request クラスを Service メソッドの引数にする
【必須】Controller → Mapper → Command → Service の順で変換する
```

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

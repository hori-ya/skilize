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
│   ├── infrastructure/          ← SecurityConfig, JwtUtil, JwtAuthenticationFilter, InitialPasswordFilter, LoggingFilter
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
│   ├── domain/
│   │   ├── model/                ← User, Role（JPA/Springに依存しない純粋なドメインモデル）
│   │   └── repository/           ← UserRepository（Interfaceのみ）
│   └── infrastructure/
│       ├── security/             ← UserPrincipal（UserDetails実装。Userをラップ）, UserDetailsServiceImpl
│       └── persistence/
│           ├── entity/           ← UserEntity（JPA @Entity）
│           ├── repository/       ← UserJpaRepository（Spring Data JPA）、UserRepositoryImpl（domain.repositoryの実装）
│           └── mapper/           ← UserPersistenceMapper（Entity⇄Domain変換）
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
│   ├── domain/
│   │   ├── model/                ← Inventory, ItSkillDetail, QualificationDetail, SeminarDetail, InventoryGoal, InventoryStatus, GoalCategory, AchievementStatus（JPA/Springに依存しない純粋なドメインモデル）
│   │   └── repository/           ← 各モデルに対応するRepository（Interfaceのみ）
│   └── infrastructure/persistence/
│       ├── entity/               ← 各モデルに対応するJPA @Entity（例: InventoryEntity, ItSkillDetailEntity）
│       ├── repository/           ← 各XxxJpaRepository（Spring Data JPA）、XxxRepositoryImpl（domain.repositoryの実装）
│       └── mapper/               ← 各XxxPersistenceMapper（Entity⇄Domain変換。user/fiscalyear/masterの各Mapperに委譲）
│
├── master
│   ├── presentation/
│   │   ├── MasterController.java
│   │   ├── request/             ← SkillLevelRequest, ItSkillRequest, ItSkillCategoryRequest, ItSkillCategoryUpdateRequest, QualificationRequest, AdSeminarRequest, PromoteItSkillRequest, PromoteQualificationRequest, SimpleCategoryRequest
│   │   └── response/            ← SkillLevelResponse, ItSkillResponse, ItSkillCategoryResponse, QualificationResponse, QualificationCategoryResponse, AdSeminarResponse, AdSeminarCategoryResponse, SeminarCategoryResponse, CustomUnregisteredResponse
│   ├── domain/
│   │   ├── model/                ← SkillLevel, ItSkill, ItSkillCategory, Qualification, QualificationCategory, AdSeminar, AdSeminarCategory, SeminarCategory（JPA/Springに依存しない純粋なドメインモデル）
│   │   └── repository/           ← 各モデルに対応するRepository（Interfaceのみ）
│   └── infrastructure/
│       ├── excel/                ← ExcelStyleHelper・ExcelFormatException・各Exporter/Importer（既存のまま。importのみpure domainへ追従）
│       └── persistence/
│           ├── entity/           ← 各モデルに対応するJPA @Entity（例: ItSkillEntity, ItSkillCategoryEntity）
│           ├── repository/       ← 各XxxJpaRepository（Spring Data JPA）、XxxRepositoryImpl（domain.repositoryの実装）
│           └── mapper/           ← 各XxxPersistenceMapper（Entity⇄Domain変換。ItSkillCategoryは親カテゴリを再帰変換）
│
├── fiscalyear
│   ├── presentation/
│   │   ├── FiscalYearController.java
│   │   ├── request/             ← FiscalYearRequest, FiscalYearSettingsRequest
│   │   └── response/            ← FiscalYearResponse, FiscalYearSettingsResponse
│   ├── domain/
│   │   ├── model/                ← FiscalYear, FiscalYearSettings（JPA/Springに依存しない純粋なドメインモデル）
│   │   └── repository/           ← FiscalYearRepository, FiscalYearSettingsRepository（Interfaceのみ）
│   └── infrastructure/persistence/
│       ├── entity/               ← FiscalYearEntity, FiscalYearSettingsEntity（JPA @Entity）
│       ├── repository/           ← FiscalYearJpaRepository, FiscalYearSettingsJpaRepository（Spring Data JPA）、FiscalYearRepositoryImpl, FiscalYearSettingsRepositoryImpl（domain.repositoryの実装）
│       └── mapper/               ← FiscalYearPersistenceMapper, FiscalYearSettingsPersistenceMapper（Entity⇄Domain変換）
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
│   ├── domain/
│   │   ├── model/                ← UserExpectation（JPA/Springに依存しない純粋なドメインモデル）
│   │   └── repository/           ← UserExpectationRepository（Interfaceのみ）
│   └── infrastructure/persistence/
│       ├── entity/               ← UserExpectationEntity（JPA @Entity）
│       ├── repository/           ← UserExpectationJpaRepository（Spring Data JPA）、UserExpectationRepositoryImpl（domain.repositoryの実装）
│       └── mapper/               ← UserExpectationPersistenceMapper（Entity⇄Domain変換。userはUserPersistenceMapperに委譲）
│
├── interview
│   ├── presentation/
│   │   ├── InterviewController.java
│   │   ├── request/             ← SaveInterviewRequest, DetailNoteRequest
│   │   └── response/            ← InterviewResponse, DetailNoteResponse
│   ├── application/
│   │   ├── InterviewService.java
│   │   └── command/             ← DetailNoteCommand
│   ├── domain/
│   │   ├── model/                ← InventoryInterview・InterviewDetailNote・DetailType（JPA/Springに依存しない純粋なドメインモデル）
│   │   └── repository/           ← InventoryInterviewRepository・InterviewDetailNoteRepository（Interfaceのみ）
│   └── infrastructure/persistence/
│       ├── entity/               ← InventoryInterviewEntity・InterviewDetailNoteEntity（JPA @Entity）
│       ├── repository/           ← XxxJpaRepository（Spring Data JPA）、XxxRepositoryImpl（domain.repositoryの実装）
│       └── mapper/               ← XxxPersistenceMapper（Entity⇄Domain変換。interviewerはuser側のMapperに委譲）
│
└── ai
    ├── presentation/
    │   ├── AiAnalysisController.java  ← QueryResult を直接返す
    │   ├── AiChatController.java      ← POST /api/ai/chat
    │   └── request/                   ← AiChatRequest
    ├── application/
    │   ├── AiAnalysisService.java
    │   ├── AiChatService.java         ← Python FastAPI への同期転送
    │   ├── InventoryCompletedEventListener.java
    │   ├── command/             ← AiChatCommand
    │   ├── mapper/              ← AiChatApplicationMapper
    │   └── query/               ← AiAnalysisQueryResult・AiChatQueryResult
    ├── domain/
    │   ├── model/                ← AiCareerAnalysis・AiAnalysisStatus（JPA/Springに依存しない純粋なドメインモデル）
    │   └── repository/           ← AiCareerAnalysisRepository（Interfaceのみ）
    └── infrastructure/persistence/
        ├── entity/               ← AiCareerAnalysisEntity（JPA @Entity）
        ├── repository/           ← AiCareerAnalysisJpaRepository（Spring Data JPA）、AiCareerAnalysisRepositoryImpl（domain.repositoryの実装）
        └── mapper/               ← AiCareerAnalysisPersistenceMapper（Entity⇄Domain変換）
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

* domain は純粋に保つ（JPA/Spring Frameworkに一切依存しない。`jakarta.persistence.*` の import 禁止）
* Repository は Interface のみ配置する（`extends JpaRepository` は禁止。実装は infrastructure 側）

サブパッケージ構成（全feature共通）:

```text
domain/
├── model/       ← ドメインモデル本体（Entity相当・Enum・Value Object）。JPA/Springアノテーションなし
├── repository/  ← Repository interface のみ（JpaRepositoryを継承しない）
├── service/     ← Domain Service（Entity単体で表現しづらい業務ロジック。必要になるまで空でよい）
├── value/       ← Value Object（Enumで十分な場合は無理に作らない）
└── exception/   ← feature固有の業務例外（横断的な例外は shared/domain/exception/ に置く）
```

ドメインモデルの実装規則:

* ビジネス生成用の `static create(...)` ファクトリと、永続化復元専用の `static reconstruct(...)` ファクトリを分ける（`reconstruct` は infrastructure層のMapperからのみ呼び出す）
* フィールド変更はドメインメソッド経由のみ（`@Setter` 禁止）
* 他featureのモデルを直接内包してよいのは、そのfeatureの `domain.model` が既に純粋ドメイン化されている場合のみ。JPAエンティティ（infrastructure層）を持ち込まない
* 親エンティティへの逆参照（例: 明細が持つ棚卸ヘッダーへの参照）は、`.getId()` 以外の用途がなければ nested object ではなく `xxxId: Integer` のようにフラットなIDで保持する（不要なEntity展開・追加クエリを避けるため）

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

サブパッケージ構成（全feature共通）:

```text
infrastructure/
├── persistence/
│   ├── entity/      ← JPA @Entity（`XxxEntity`。JPA/Hibernateアノテーションはここにのみ記述する）
│   ├── repository/  ← `XxxJpaRepository`（Spring Data JPA。既存の@Query/JOIN FETCH戦略はそのまま）
│   │                   + `XxxRepositoryImpl`（domain.repository.XxxRepository の実装。Entity⇄Domain変換をMapper経由で行う）
│   └── mapper/      ← `XxxPersistenceMapper`（Entity→Domainの変換。他featureの関連は、そのfeatureの
│                       Mapperに委譲する。例: InventoryPersistenceMapper は UserPersistenceMapper /
│                       FiscalYearPersistenceMapper に委譲する）
├── security/        ← feature固有のSpring Security実装（例: UserPrincipal, UserDetailsServiceImpl）
├── external/        ← 外部システム連携（DB以外の技術的実装）
└── config/          ← feature固有のConfiguration
```

`XxxRepositoryImpl` の実装パターン:

* 新規保存（id が null）: `XxxEntity.create(...)` で新規Entityを組み立てる。関連エンティティは他featureの
  `XxxJpaRepository.getReferenceById(id)` で参照のみ取得する（`findById` と違い実クエリを発行しない軽量な参照）
* 更新（id が非null）: `jpaRepository.findById(id)` で既存Entityを取得し、ドメインメソッド相当の
  `entity.applyState(...)` 等でドメイン側が計算済みの値をそのまま反映する（タイムスタンプ等をEntity側で再計算しない）
* 変換後は必ず `mapper.toDomain(jpaRepository.save(entity))` でドメインモデルとして返す

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
- Logging（LoggingFilter: MDC requestId/userId セットアップ）
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

# Authentication Principal Rule

Controller で認証済みユーザー（ドメインモデル `User`）を取得する際は、必ず以下の形式を使用する。

```java
@GetMapping
public XxxResponse example(@AuthenticationPrincipal(expression = "user") User user) {
```

`user.infrastructure.security.UserPrincipal` が Spring Security の認証プリンシパル（`UserDetails`）であり、
ドメインモデル `User`（Spring非依存）をラップしている。`expression = "user"` の SpEL 式で `UserPrincipal.getUser()` を
直接取り出すことで、Controller 側は `User` 型のまま扱える。

禁止: `@AuthenticationPrincipal User user`（`expression` なし）は `UserPrincipal` と `User` の型不一致でエラーになる。

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

Domain / Infrastructure（永続化レイヤー）:

| 対象 | パッケージ | 命名パターン | 例 |
|---|---|---|---|
| ドメインモデル | `domain/model/` | `Xxx` | `User`, `Inventory` |
| Repository（インターフェース） | `domain/repository/` | `XxxRepository` | `UserRepository` |
| JPAエンティティ | `infrastructure/persistence/entity/` | `XxxEntity` | `UserEntity` |
| Spring Data JPA リポジトリ | `infrastructure/persistence/repository/` | `XxxJpaRepository` | `UserJpaRepository` |
| Repository実装 | `infrastructure/persistence/repository/` | `XxxRepositoryImpl` | `UserRepositoryImpl` |
| Entity⇄Domain変換Mapper | `infrastructure/persistence/mapper/` | `XxxPersistenceMapper` | `UserPersistenceMapper` |

> 詳細（サブパッケージ構成・実装パターン）は上記「Layer Responsibilities」の domain / infrastructure セクションを参照。

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

# Logging Rules

## ログ基盤

* SLF4J + Logback を使用する（`@Slf4j` by Lombok）
* フォーマット: プレーンテキスト（開発・本番共通）
* MDC: `LoggingFilter`（`shared/infrastructure/`）がリクエストごとに `requestId`（UUID）と `userId`（未認証時は `"-"`）をセット

## ログレベル

| レベル | 用途 |
|--------|------|
| ERROR | 5xx エラー・外部サービス障害 |
| WARN | 認証失敗・権限エラー（4xx）・バリデーションエラー |
| INFO | 正常な業務操作（マスタ更新・ユーザー管理・棚卸提出等） |
| DEBUG | 開発環境のみ。本番は INFO 以上のみ出力 |

## INFO ログを出力すべき操作

* マスタ管理の書き込み操作（POST/PUT/DELETE/PATCH）
* ユーザー管理（作成・更新・有効化・無効化・パスワードリセット）
* カスタムスキル昇格（ITスキル・資格）
* 棚卸提出（`POST /api/inventories/{id}/submit`）
* AI分析トリガー・完了・失敗

## ログ出力禁止事項

以下の情報はログに出力しない:

* パスワード・パスワードハッシュ（`password_hash`）
* JWT トークン（`Authorization` ヘッダの値）
* 氏名・メールアドレス等の個人情報

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

---

# Directory Responsibilities

## バックエンド（Spring Boot）

| ディレクトリ | 責務 |
|---|---|
| `com/skilize/shared/domain/exception/` | 共通例外（AuthException, GoalIncompleteException） |
| `com/skilize/shared/infrastructure/` | SecurityConfig・JwtUtil・JwtAuthenticationFilter・InitialPasswordFilter・LoggingFilter（MDC requestId/userId セットアップ） |
| `com/skilize/shared/presentation/` | GlobalExceptionHandler・ErrorResponse・ValidationErrorResponse |
| `com/skilize/auth/presentation/` | AuthController |
| `com/skilize/auth/presentation/request/` | LoginRequest・ChangePasswordRequest |
| `com/skilize/auth/application/` | AuthService（ログイン・JWT 発行・パスワード変更ロジック） |
| `com/skilize/auth/application/command/` | LoginCommand・ChangePasswordCommand |
| `com/skilize/auth/application/query/` | LoginQueryResult・MeQueryResult |
| `com/skilize/auth/application/mapper/` | AuthApplicationMapper（Request→Command 変換） |
| `com/skilize/user/presentation/` | UserController |
| `com/skilize/user/presentation/request/` | CreateUserRequest・UpdateUserRequest |
| `com/skilize/user/presentation/response/` | UserResponse・TeamMemberResponse・MemberInventorySummaryResponse・FiscalYearRef・ResetPasswordResponse |
| `com/skilize/user/domain/model/` | User・Role（JPA/Springに依存しない純粋なドメインモデル） |
| `com/skilize/user/domain/repository/` | UserRepository（Interfaceのみ） |
| `com/skilize/user/infrastructure/security/` | UserPrincipal（UserDetails実装。ドメインUserをラップしてSpring Securityの認証プリンシパルとして扱う）・UserDetailsServiceImpl |
| `com/skilize/user/infrastructure/persistence/entity/` | UserEntity（JPA @Entity） |
| `com/skilize/user/infrastructure/persistence/repository/` | UserJpaRepository（Spring Data JPA）、UserRepositoryImpl（domain.repositoryの実装） |
| `com/skilize/user/infrastructure/persistence/mapper/` | UserPersistenceMapper（Entity⇄Domain変換） |
| `com/skilize/inventory/presentation/` | InventoryController |
| `com/skilize/inventory/presentation/request/` | CreateInventoryRequest・ItSkillDetailsRequest・ItSkillDetailItem・QualificationDetailsRequest・SeminarDetailsRequest・RemarksPatchRequest・GoalsRequest・GoalItem・GoalReviewUpdateRequest・GoalReviewUpdateItem |
| `com/skilize/inventory/presentation/response/` | InventorySummaryResponse・InventoryDetailResponse・ItSkillDetailsResponse・QualificationDetailsResponse・SeminarDetailsResponse・SubmitResponse・GoalsResponse・GoalCompleteResponse・GoalReviewCompleteResponse |
| `com/skilize/inventory/application/` | InventoryService（棚卸ビジネスロジック） |
| `com/skilize/inventory/application/command/` | ItSkillDetailCommand・QualificationDetailCommand・SeminarDetailCommand・GoalCommand・GoalReviewUpdateCommand |
| `com/skilize/inventory/application/query/` | ComparisonQueryResult・GoalReviewQueryResult |
| `com/skilize/inventory/application/mapper/` | InventoryApplicationMapper（Request→Command 変換） |
| `com/skilize/inventory/domain/model/` | Inventory・ItSkillDetail・QualificationDetail・SeminarDetail・InventoryGoal・列挙型（JPA/Springに依存しない純粋なドメインモデル） |
| `com/skilize/inventory/domain/repository/` | 各モデルに対応するRepository（Interfaceのみ） |
| `com/skilize/inventory/infrastructure/persistence/entity/` | 各モデルに対応するJPA @Entity（例: InventoryEntity, ItSkillDetailEntity） |
| `com/skilize/inventory/infrastructure/persistence/repository/` | 各XxxJpaRepository（Spring Data JPA）、XxxRepositoryImpl（domain.repositoryの実装） |
| `com/skilize/inventory/infrastructure/persistence/mapper/` | 各XxxPersistenceMapper（Entity⇄Domain変換） |
| `com/skilize/master/presentation/` | MasterController・MasterExcelController |
| `com/skilize/master/presentation/request/` | SkillLevelRequest・ItSkillRequest・ItSkillCategoryRequest・QualificationRequest・AdSeminarRequest・PromoteItSkillRequest・SimpleCategoryRequest |
| `com/skilize/master/presentation/response/` | SkillLevelResponse・ItSkillResponse・QualificationResponse・AdSeminarResponse・CustomUnregisteredResponse・MasterImportResponse |
| `com/skilize/master/application/` | MasterService（マスタ CRUD）・MasterExcelService（Excel 出力・取込） |
| `com/skilize/master/application/query/` | MasterImportQueryResult・MasterImportErrorDetail |
| `com/skilize/master/domain/model/` | マスタドメインモデル（SkillLevel, ItSkill, ItSkillCategory, Qualification, QualificationCategory, AdSeminar, AdSeminarCategory, SeminarCategory。JPA/Springに依存しない） |
| `com/skilize/master/domain/repository/` | 各モデルに対応するRepository（Interfaceのみ） |
| `com/skilize/master/infrastructure/excel/` | ExcelStyleHelper・ExcelFormatException・各 Exporter/Importer（ItSkill・Qualification・AdSeminar） |
| `com/skilize/master/infrastructure/persistence/entity/` | 各モデルに対応するJPA @Entity（例: ItSkillEntity, ItSkillCategoryEntity） |
| `com/skilize/master/infrastructure/persistence/repository/` | 各XxxJpaRepository（Spring Data JPA）、XxxRepositoryImpl（domain.repositoryの実装） |
| `com/skilize/master/infrastructure/persistence/mapper/` | 各XxxPersistenceMapper（Entity⇄Domain変換） |
| `com/skilize/fiscalyear/presentation/` | FiscalYearController |
| `com/skilize/fiscalyear/presentation/request/` | FiscalYearRequest・FiscalYearSettingsRequest |
| `com/skilize/fiscalyear/presentation/response/` | FiscalYearResponse・FiscalYearSettingsResponse |
| `com/skilize/fiscalyear/domain/model/` | FiscalYear・FiscalYearSettings（JPA/Springに依存しない純粋なドメインモデル） |
| `com/skilize/fiscalyear/domain/repository/` | FiscalYearRepository・FiscalYearSettingsRepository（Interfaceのみ） |
| `com/skilize/fiscalyear/infrastructure/persistence/entity/` | FiscalYearEntity・FiscalYearSettingsEntity（JPA @Entity） |
| `com/skilize/fiscalyear/infrastructure/persistence/repository/` | FiscalYearJpaRepository・FiscalYearSettingsJpaRepository（Spring Data JPA）、FiscalYearRepositoryImpl・FiscalYearSettingsRepositoryImpl（domain.repositoryの実装） |
| `com/skilize/fiscalyear/infrastructure/persistence/mapper/` | FiscalYearPersistenceMapper・FiscalYearSettingsPersistenceMapper（Entity⇄Domain変換） |
| `com/skilize/dashboard/presentation/` | DashboardController |
| `com/skilize/dashboard/presentation/response/` | DashboardResponse（nested UserInfo・FiscalYearRef・CurrentInventoryInfo） |
| `com/skilize/charts/presentation/` | ChartController（QueryResult を直接返す） |
| `com/skilize/charts/application/` | ChartService（スキルバランス・成長推移・ヒートマップ・タイムライン集計） |
| `com/skilize/charts/application/query/` | RadarQueryResult・GrowthQueryResult・HeatmapQueryResult・TimelineQueryResult |
| `com/skilize/expectation/presentation/` | ExpectationController |
| `com/skilize/expectation/presentation/request/` | SaveExpectationRequest |
| `com/skilize/expectation/application/` | ExpectationService（期待コメント保存ロジック） |
| `com/skilize/expectation/application/query/` | ExpectationQueryResult |
| `com/skilize/expectation/domain/model/` | UserExpectation（JPA/Springに依存しない純粋なドメインモデル） |
| `com/skilize/expectation/domain/repository/` | UserExpectationRepository（Interfaceのみ） |
| `com/skilize/expectation/infrastructure/persistence/entity/` | UserExpectationEntity（JPA @Entity） |
| `com/skilize/expectation/infrastructure/persistence/repository/` | UserExpectationJpaRepository（Spring Data JPA）、UserExpectationRepositoryImpl（domain.repositoryの実装） |
| `com/skilize/expectation/infrastructure/persistence/mapper/` | UserExpectationPersistenceMapper（Entity⇄Domain変換） |
| `com/skilize/interview/presentation/` | InterviewController |
| `com/skilize/interview/presentation/request/` | SaveInterviewRequest・DetailNoteRequest |
| `com/skilize/interview/presentation/response/` | InterviewResponse・DetailNoteResponse |
| `com/skilize/interview/application/` | InterviewService（面談メモ保存ロジック） |
| `com/skilize/interview/application/command/` | DetailNoteCommand |
| `com/skilize/interview/domain/model/` | InventoryInterview・InterviewDetailNote・DetailType（JPA/Springに依存しない純粋なドメインモデル） |
| `com/skilize/interview/domain/repository/` | InventoryInterviewRepository・InterviewDetailNoteRepository（Interfaceのみ） |
| `com/skilize/interview/infrastructure/persistence/entity/` | InventoryInterviewEntity・InterviewDetailNoteEntity（JPA @Entity） |
| `com/skilize/interview/infrastructure/persistence/repository/` | 各XxxJpaRepository（Spring Data JPA）、XxxRepositoryImpl（domain.repositoryの実装） |
| `com/skilize/interview/infrastructure/persistence/mapper/` | 各XxxPersistenceMapper（Entity⇄Domain変換） |
| `com/skilize/ai/presentation/` | AiAnalysisController（QueryResult を直接返す）・AiChatController（POST /api/ai/chat） |
| `com/skilize/ai/presentation/request/` | AiChatRequest |
| `com/skilize/ai/application/` | AiAnalysisService（非同期AI分析）・AiChatService（Python同期転送）・InventoryCompletedEventListener |
| `com/skilize/ai/application/command/` | AiChatCommand |
| `com/skilize/ai/application/mapper/` | AiChatApplicationMapper（AiChatRequest→AiChatCommand 変換） |
| `com/skilize/ai/application/query/` | AiAnalysisQueryResult・AiChatQueryResult |
| `com/skilize/ai/domain/model/` | AiCareerAnalysis・AiAnalysisStatus（JPA/Springに依存しない純粋なドメインモデル） |
| `com/skilize/ai/domain/repository/` | AiCareerAnalysisRepository（Interfaceのみ） |
| `com/skilize/ai/infrastructure/persistence/entity/` | AiCareerAnalysisEntity（JPA @Entity） |
| `com/skilize/ai/infrastructure/persistence/repository/` | AiCareerAnalysisJpaRepository（Spring Data JPA）、AiCareerAnalysisRepositoryImpl（domain.repositoryの実装） |
| `com/skilize/ai/infrastructure/persistence/mapper/` | AiCareerAnalysisPersistenceMapper（Entity⇄Domain変換） |
| `com/skilize/report/presentation/` | ReportController（棚卸表 PDF ダウンロードエンドポイント） |
| `com/skilize/report/application/` | ReportService（JasperReports を使った棚卸表 PDF 生成） |
| `apps/backend/src/main/resources/reports/` | 帳票レイアウトファイル（`.jrxml`）の格納フォルダ |
| `apps/backend/src/main/resources/db/migration/` | Flyway マイグレーション（本番・CI 用） |
| `scripts/db/init.sql` | ローカル Docker DB 用の完全初期化スクリプト（DROP→CREATE→INSERT） |

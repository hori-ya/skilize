# コメント記載標準書

---

## 1. 目的

本標準書は、Skilize プロジェクトにおけるソースコードの可読性・保守性向上および属人化防止を目的として、コメント記載ルールを定めるものである。

本プロジェクトでは Claude Code を利用して開発を行うため、人間および AI が理解しやすいコメントを記載し、品質の高いソースコードを維持することを目的とする。

---

## 2. 適用範囲

本標準は以下のソースコードに適用する。

- React（TypeScript）
- Spring Boot（Java）
- FastAPI（Python）

テストファイル（`*Test.java` / `*.test.tsx` / `test_*.py`）はファイルヘッダ不要。ただしテストクラスには概要コメントを付ける。

---

## 3. 基本方針

### 3.1 コメントの目的

コメントは以下の内容を補足するために記載する。

- 業務上の意図
- 設計上の理由
- 処理の目的
- データ項目の意味
- 保守上の注意事項
- AI 処理における判断理由

### 3.2 コメントの優先順位

コメントは以下の順で記載する。

1. なぜその処理が必要なのか
2. 業務上どのような意味を持つのか
3. 何をしている処理なのか

### 3.3 記載不要なコメント

ソースコードを見れば理解できる内容は記載しない。

**悪い例**

```java
// countを加算
count++;
```

```typescript
// ユーザーを取得
const user = response.data;
```

```python
# ループ開始
for item in items:
```

---

## 4. ファイルヘッダ

### 対象

全ての本番ソースファイル（Java / TypeScript / Python）。テストファイルは除く。

### 記載項目

| 項目 | 内容 |
|---|---|
| 機能ID | 下記の機能 ID 一覧を参照 |
| 機能名 | 機能の名称（日本語） |
| 作成日 | YYYY/MM/DD 形式 |
| 作成者 | GitHub ユーザー名 |
| 機能概要 | ファイルの役割を 1〜3 行で説明 |
| 更新履歴 | 最新履歴を先頭に追記形式で管理 |
| Copyright | `Copyright (C) 2026 Skilize Project. All Rights Reserved.` |

### 機能 ID 一覧

| 機能 ID | 機能名 | Java パッケージ / TS feature |
|---|---|---|
| `AUTH` | 認証機能 | `com.skilize.auth` / `features/auth` |
| `USR` | ユーザー管理 | `com.skilize.user` / `features/user` |
| `INV` | 棚卸管理 | `com.skilize.inventory` / `features/inventory` |
| `MST` | マスタ管理 | `com.skilize.master` / `features/master` |
| `FY` | 年度管理 | `com.skilize.fiscalyear` / `features/fiscalyear` |
| `DSH` | ダッシュボード | `com.skilize.dashboard` / `features/dashboard` |
| `CHT` | グラフ・チャート | `com.skilize.charts` / `features/charts` |
| `RPT` | 帳票・レポート | `com.skilize.report` / `features/report` |
| `AI` | AI 機能 | `com.skilize.ai` / `features/ai` / `apps/ai` |
| `EXP` | 期待コメント | `com.skilize.expectation` / `features/expectation` |
| `IVW` | 面談メモ | `com.skilize.interview` / `features/interview` |
| `SHR` | 共通 | `com.skilize.shared` / `shared/` / `app/` |

### 記載ルール

- ファイル先頭に記載する
- Copyright 年は 2026 を起点として記載する
- 更新履歴は削除しない（追記形式）
- 最新履歴を先頭に追加する
- GitHub Issue がある場合は `#イシュー番号` を記載する（例: `#42`）
- 初版作成のみの場合は `#` 番号なしで「初版作成」と記載する
- 更新内容は第三者が見て理解できる具体的な内容とする
- 「修正」「変更」など曖昧な表現は禁止する

### Java 記載例

```java
/**************************************************************************************************************
 * 機能ID      ：AUTH
 * 機能名      ：認証機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ログイン・パスワード変更・JWT 発行を行う認証機能のビジネスロジック。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
```

### TypeScript 記載例

```typescript
/*******************************************************************************
 * 機能ID      ：AUTH
 * 機能名      ：認証機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * ログインページ。ユーザー ID・パスワードを入力して認証を行う。
 * 初回ログイン時はパスワード変更ページへリダイレクトする。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
```

### Python 記載例

```python
# ******************************************************************************
# 機能ID      ：AI
# 機能名      ：AI 機能
# 作成日      ：2026/06/08
# 作成者      ：hori-ya
# ------------------------------------------------------------------------------
# 機能概要：
# AI チャットサービス。モードに応じたシステムプロンプトで LLM に問い合わせる。
# ------------------------------------------------------------------------------
# 更新履歴：
# 2026/06/08 hori-ya 初版作成
# ------------------------------------------------------------------------------
# Copyright (C) 2026 Skilize Project. All Rights Reserved.
# ******************************************************************************
```

---

## 5. クラス・コンポーネントコメント

### 対象

- React Component（JSDoc 形式）
- Controller
- Service
- Entity
- DTO（Request / Response / Command / QueryResult）
- FastAPI Router / Service

### 記載内容

- 役割（1 行）
- 機能概要（2〜3 行）
- 設計上の注意・制約（必要な場合のみ）

### Java 記載例

```java
/**
 * 認証サービス。
 *
 * ログイン・パスワード変更・自情報取得のビジネスロジックを提供する。
 * ユーザー ID の存在有無を外部に漏らさないため、ユーザー不在とパスワード不一致で同一エラーを返す。
 */
@Service
public class AuthService {
```

### TypeScript 記載例

```typescript
/**
 * ログインページ。
 *
 * ユーザー ID・パスワードを入力して認証を行う。
 * 初回ログイン時はパスワード変更ページへリダイレクトする。
 */
export default function LoginPage() {
```

### Python 記載例

```python
class CareerAnalysisService:
    """
    AI キャリア分析サービス。

    ユーザーのスキル・目標データをもとに LLM でキャリア分析を生成する。
    """
```

---

## 6. 関数・メソッドコメント

### 対象

- public メソッド：必須
- protected メソッド：推奨
- private メソッド：複雑な処理のみ

### 記載内容

- 処理概要（1〜2 行）
- `@param`：引数の意味（型が自明な場合は省略可）
- `@return`：戻り値の意味
- `@throws`：例外が発生する条件（必要に応じて）

### Java

```java
/**
 * ログイン処理。ユーザー ID・パスワードを検証し、成功時に JWT を発行して返す。
 * ユーザー不在とパスワード不一致を同一エラーにすることで、ユーザー ID の存在有無を外部に漏らさない。
 *
 * @param command ログインコマンド（ユーザー ID・パスワード）
 * @return ログイン結果（JWT・ユーザー情報）
 * @throws AuthException 認証失敗時またはアカウント無効時
 */
public LoginQueryResult login(LoginCommand command) {
```

### TypeScript

```typescript
/**
 * ユーザー一覧取得。
 *
 * @param fiscalYearId 対象年度 ID
 * @returns ユーザー一覧
 */
const getUsers = async (fiscalYearId: number): Promise<User[]> => {
```

### Python

```python
def process_chat(message: str, mode: str, user_id: int, history: list[dict]) -> str:
    """
    チャット処理。モードに応じたシステムプロンプトで LLM に問い合わせ、応答テキストを返す。

    Args:
        message: ユーザーメッセージ
        mode: チャットモード（NORMAL / PROOFREADING / CAREER / HELP）
        user_id: ユーザーの内部 ID
        history: 会話履歴（role / content のリスト）

    Returns:
        LLM の応答テキスト
    """
```

---

## 7. 業務ロジックコメント

業務ルールが存在する箇所には必ずコメントを記載する。

### 記載例

```java
// 新規ユーザーは必ず初回パスワード変更が必要
u.initialPassword = true;
```

```java
// ユーザー列挙攻撃対策のため、ユーザー不在とパスワード不一致で同一エラーを返す
throw new AuthException("AUTH_FAILED", "");
```

```typescript
// isInitialPassword が true の場合はパスワード変更ページへリダイレクトする業務ルール
if (user.isInitialPassword) navigate('/change-password');
```

```python
# hallucination 抑制のためユーザーのスキル・目標データをプロンプトに付与する
return CAREER_SYSTEM_PROMPT.format(inventory_context=context)
```

---

## 8. データ項目コメント

### 対象

- Entity フィールド：**必須**
- Request：バリデーション制約が複雑な場合のみ
- Response / QueryResult：意味が不明瞭なフィールドのみ

### Java Entity 記載例

```java
/** パスワードハッシュ（BCrypt コスト 12・API レスポンスに含めない） */
@Column(name = "password_hash", nullable = false)
private String passwordHash;

/** 初回パスワードフラグ（true=パスワード変更強制。InitialPasswordFilter が参照する） */
@Column(name = "is_initial_password", nullable = false)
private boolean initialPassword;
```

### Java record（DTO）記載例

record はコンストラクタ引数にコメントを付けない。クラスコメントに主要項目の意味を記載する。

```java
/**
 * ログイン結果。
 *
 * token: JWT アクセストークン（ローカルストレージに保存して Bearer トークンとして送信する）
 * userInfo: ログインユーザー情報（フロントエンドの状態管理に使用する）
 */
public record LoginQueryResult(String token, UserInfo userInfo) {
```

---

## 9. AI 機能（FastAPI）特別ルール

再現性および保守性確保のため、AI 処理の判断理由を必ずコメントとして残す。

### コメント対象

| 項目 | コメント記載内容 |
|---|---|
| プロンプト | 設計意図・制約事項 |
| モデル選定 | 選定理由（コスト・品質バランス等） |
| Temperature 設定 | 設定値と理由 |
| 会話履歴上限 | 上限数と理由（コスト・コンテキスト長のバランス） |
| フォールバック処理 | なぜフォールバックが必要か |

### 記載例

```python
# 会話履歴の最大件数（古いものから切り捨て）
# LLM のコンテキスト長制限と API コスト削減のバランスを考慮して 20 件を上限とする
MAX_HISTORY = 20
```

```python
# DB 接続失敗時はデータなしプロンプトにフォールバックする
# ユーザーへのエラー表示を避け、データなしでも AI 応答を返す設計
return CAREER_SYSTEM_PROMPT_NO_DATA
```

---

## 10. React 固有ルール

### コンポーネント

全てのコンポーネントにクラスコメント（JSDoc 形式）を記載する。

```typescript
/**
 * 認証ガード。未認証ユーザーをログインページへリダイレクトする。
 */
export default function PrivateRoute({ children }: { children: React.ReactNode }) {
```

### State 変数

意味が自明でない場合のみコメントを記載する。

```typescript
// 選択中のユーザー ID（メンバー詳細表示の制御に使用する）
const [selectedUserId, setSelectedUserId] = useState<number>();
```

### useEffect

処理理由を必ず記載する。

```typescript
// 初期表示時に年度一覧とユーザー一覧を取得する
useEffect(() => {
  fetchFiscalYears();
  fetchUsers();
}, []);
```

---

## 11. Spring 固有ルール

### Service

業務ルールがある箇所には必ずコメントを記載する。

```java
// SecurityContext のユーザーは JPA 管理外の可能性があるため、
// ID で再フェッチしてトランザクション内で更新する
User user = userRepository.findById(currentUser.getId()).orElseThrow();
```

### Filter

処理フローに関するコメントを記載する。

```java
// フィルターチェーン: JwtAuthenticationFilter → InitialPasswordFilter → コントローラー
```

### Repository

原則コメント不要とする。複雑な JPQL クエリのみコメントを記載する。

---

## 12. FastAPI 固有ルール

### API エンドポイント

全ての公開エンドポイントに Docstring を記載する。

```python
@router.post("/analyze", status_code=202)
async def analyze(request: AnalyzeRequest, background_tasks: BackgroundTasks):
    """
    AI キャリア分析トリガー。

    バックグラウンドタスクで分析を起動し、即座に 202 を返す。
    分析完了後は DB（ai_career_analyses）にステータスを更新する。
    """
```

### AI 関連処理

推論結果に影響を与えるパラメータは理由を記載する。

---

## 13. Claude Code 利用ルール

Claude Code によるコード生成・修正時は以下を必須とする。

### 必須項目

- ファイルヘッダ（新規ファイル作成時）
- クラス / コンポーネントコメント
- public メソッドコメント
- 業務ロジックコメント
- Entity フィールドコメント
- AI 処理理由コメント

### コメント言語

**コメントは日本語で統一する。** 例外なし。

### 命名規則との組み合わせ

| 対象 | 言語 |
|---|---|
| クラス名・関数名・変数名 | 英語 |
| コメント | 日本語 |

### 良い例

```java
// 締日を跨ぐデータは翌月売上として集計する
```

```python
# 応答速度と回答品質のバランスを考慮して上位 5 件を採用
```

### 悪い例

```java
// ループ開始
for (...) {
```

```typescript
// API 呼び出し
await api.get(...)
```

---

## 14. 更新履歴管理ルール

更新履歴は保守担当者による障害調査および変更履歴確認を目的として管理する。

### 記載形式

```
YYYY/MM/DD 担当者名 #GitHubIssue番号（任意） 変更内容
```

### 記載例

```
2026/07/02 hori-ya #42 CSV 出力機能追加
2026/06/15 hori-ya #38 検索条件追加
2026/06/08 hori-ya 初版作成
```

### 運用ルール

- 最新履歴を先頭に追加する
- 過去履歴は削除しない
- 内容は具体的に記載する
- 「修正」「変更」など曖昧な表現は禁止する
- GitHub Issue がある場合はイシュー番号を記載する
- 障害対応時は障害番号またはイシュー番号を記載する

---

## 15. レビュー観点

レビュー時は以下を確認する。

- [ ] ファイルヘッダが記載されている（本番ソースファイルのみ）
- [ ] Copyright が記載されている
- [ ] 更新履歴が最新に更新されている
- [ ] クラス / コンポーネントコメントが存在する
- [ ] public メソッドコメントが存在する
- [ ] 業務ルールがコメント化されている
- [ ] AI 関連設定の理由が記載されている
- [ ] Entity フィールドに論理名が記載されている
- [ ] コメントと実装内容が一致している
- [ ] 不要なコメントが記載されていない

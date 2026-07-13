---
paths:
  - "**/*.ts"
  - "**/*.tsx"
---

# Frontend Code Style Rules

React 等のコンポーネント指向フロントエンドにおける設計・命名規約。

---

# ディレクトリ構成の原則

- package by feature（機能単位でディレクトリを分割する）を基本方針とする
- feature 内部は責務ごとに分離する（例: `api/` `types/` `components/` `pages/`）
- 複数 feature で共有するものだけを `shared/` に置く（3回以上重複した場合のみ共有化を検討する）

---

# 依存方向

```
app → features → shared
```

禁止:
- `shared` が `features` をインポートする
- feature 間の密結合（型参照は許容するが、ロジックの直接依存はしない）

---

# コンポーネント設計

| ディレクトリ | 責務 | 禁止事項 |
|---|---|---|
| components | UI 描画・props 表示・プレゼンテーションロジック | API 呼び出し、複雑な状態管理 |
| api | バックエンド通信（HTTP クライアント呼び出し・DTO 変換） | UI ロジック・コンポーネント状態 |
| pages | ルーティング単位の画面構成・feature の組み合わせ | 重厚なビジネスロジック・直接的な API 実装 |

---

# 状態管理

- サーバー状態: 標準の非同期処理 + ローカル state で管理する（専用データフェッチライブラリを導入する場合はプロジェクト全体で統一する）
- UI状態: コンポーネントローカルな state を基本とする。複雑な場合のみ reducer を使う
- グローバル状態: 認証状態など、真にアプリ全体で共有が必要なものに限定する
- 禁止: API レスポンスをそのままグローバルストアへ全保存すること

---

# 命名規則

| 対象 | 規則 | 例 |
|---|---|---|
| コンポーネントファイル | PascalCase | `LoginPage.tsx`, `NavBar.tsx` |
| API ファイル | camelCase + 用途を表す接尾辞 | `orderApi.ts` |
| 変数・関数 | camelCase | `getUsers`, `userId` |
| 型・interface | PascalCase | `UserDetail` |
| CSSクラス | BEM ライク | `.card__title`, `.btn--primary` |

---

# 型安全性

- `any` 型を使用しない。型が不明な場合は `unknown` を使い、絞り込みを行う
- API レスポンスの型は必ず定義し、型なしで扱わない

---

# スタイリング

- 一貫した命名規則（BEM ライク等）でクラスを命名する
- inline style は例外的な微調整のみに限定する
- feature ごとに個別の CSS ファイルを乱立させない

---

# フォーム

- 標準のローカル state でフォーム状態を管理する
- 外部フォームライブラリを使う場合はプロジェクト全体で統一し、混在させない

---

# 国際化・文言管理

- UI に表示する文言をコンポーネント内に直接ハードコードしない（`className` 値等を除く）
- 文言は翻訳リソースファイル（i18n）で一元管理する
- キー命名はネスト階層2〜3段・camelCase を基本とし、意味が自明な名前にする

---

# リファクタリング方針

1. feature 単位で整理する
2. 巨大コンポーネントを分割する
3. API 呼び出しをコンポーネントから分離する
4. state を局所化する
5. shared を最小化する

大規模な一括変更は避け、段階的に進める。

---

# 短縮記法の制限

誰が読んでも処理の流れを追いやすいことを優先し、以下の短縮記法を禁止する。React の Hooks・JSX の基本文法（アロー関数・分割代入・スプレッド構文・テンプレートリテラル・JSX の `&&` 短絡評価）はフレームワークの前提であるため対象外とする。

## 禁止

| 記法 | 禁止例 |
|---|---|
| 配列コールバックメソッド | `.map(`, `.filter(`, `.find(`, `.some(`, `.every(`, `.sort(`, `.forEach(`, `.reduce(` |
| 三項演算子（`? :`） | `const label = active ? '有効' : '無効';`、JSX内の `{loading ? <A /> : <B />}` |
| Optional chaining（`?.`） | `user?.name`, `res?.data?.list` |
| Nullish coalescing（`??`） | `const remarks = d.remarks ?? '';` |

## 代替手段

| 用途 | 禁止（短縮記法） | 代替 |
|---|---|---|
| 配列の絞り込み・変換 | `list.filter(x => x.active).map(x => x.name)` | `for...of` で明示的にループし、結果を新しい配列に詰める |
| 配列から1件検索 | `list.find(x => x.id === id)` | `for...of` でループし、見つかった時点で変数へ代入して `break` する |
| 条件による値の切り替え（変数） | `const label = active ? '有効' : '無効';` | `if`/`else` で変数へ代入する |
| 条件による JSX の出し分け | `{loading ? <Spinner /> : <Content />}` | `if`/`else` でレンダリングする JSX 要素を変数に代入してから使う、または早期 `return` でコンポーネントを分岐する |
| Optional chaining | `user?.name` | `if (user != null) { ... user.name ... }` のように明示的に分岐する |
| Nullish coalescing | `d.remarks ?? ''` | `let remarks = ''; if (d.remarks != null) { remarks = d.remarks; }` のように明示的に分岐する |

## 例外（禁止対象外）

以下は React の基本文法・慣用パターンであり、禁止すると Hooks やコンポーネント設計そのものが成り立たなくなるため許可する。

- アロー関数（JSX イベントハンドラ・`useEffect`/`useState` のコールバック等）
- 分割代入（`const [x, setX] = useState(...)`、`const { a, b } = props`）
- スプレッド構文（`...`）
- テンプレートリテラル（`` `${}` ``）
- JSX の `&&` による短絡評価の条件描画（`{cond && <X />}`）
- async/await

## 理由

- `.map`/`.filter` 等の配列メソッドチェーンはコールバック内の処理を1行に凝縮しやすく、ネストした条件やインデックス操作が絡むと可読性が下がる
- 三項演算子・Optional chaining・Nullish coalescing は1行に判定と処理を凝縮するため、慣れていないと見落としやすい

---
paths:
  - "**/*.java"
  - "**/*.ts"
  - "**/*.tsx"
  - "**/*.py"
---

# Naming Conventions

言語・レイヤーを問わず適用する命名の原則。各レイヤー固有の詳細は [backend/code-style.md](backend/code-style.md)・[frontend/code-style.md](frontend/code-style.md) を参照。

---

# 基本原則

- 意図が読み取れる名前にする（不要な省略語・連番・汎用すぎる名前を避ける）
- 1つの概念には1つの用語を使い、同義語を混在させない（例: `get` と `fetch` を混ぜない）
- boolean は `is` / `has` / `can` 等の接頭辞を付ける（例: `isActive`, `hasPermission`）
- 否定形の boolean 名を避ける（`isNotValid` ではなく `isInvalid` や `!isValid`）

---

# ケース対応表

| 対象 | 規則 |
|---|---|
| クラス・型・コンポーネント | PascalCase |
| メソッド・関数・変数 | camelCase |
| 定数 | SCREAMING_SNAKE_CASE |
| DB テーブル・カラム | snake_case |
| URL パス | kebab-case（小文字） |
| ファイル名（クラス・コンポーネント） | 対応するクラス/コンポーネント名と一致させる |

---

# 入出力データ型の命名パターン

レイヤー間を流れるデータの役割を名前で明示する（詳細は [backend/code-style.md](backend/code-style.md) 参照）。

| 種別 | パターン | 役割 |
|---|---|---|
| 外部入力 | `XxxRequest` | 境界（HTTP等）から受け取る入力 |
| 外部出力 | `XxxResponse` | 境界へ返す出力 |
| 内部入力 | `XxxCommand` | ユースケースへの書き込み系入力 |
| 内部出力 | `XxxQueryResult` | ユースケースが返す参照系結果 |

責務が曖昧になる万能な `XxxDto` のような名称は避け、役割が伝わる名前にする。

---

# 禁止パターン

| 禁止 | 理由 |
|---|---|
| 汎用的すぎる共通クラス名（`Util` / `Common` / `Helper` を無限定に使う） | 何をするクラスか分からず、何でも詰め込まれがちになる |
| 意味のない連番（`data1`, `data2`） | 意図が読み取れない |
| 型を名前に含める（ハンガリアン記法） | 型は言語機能・IDEで分かるため冗長 |
| 3回以上重複していない段階での早すぎる共通化・命名 | 抽象化の方向を誤り、後から命名し直すコストが増える |

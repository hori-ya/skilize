# Rules Index

このフォルダには、プロジェクト固有の情報（`.claude/context/`）から命名・規約のみを抽出し、他プロジェクトでも再利用できる形に一般化した規約集を配置する。

---

# 横断的な規約

| ファイル | 内容 |
|---|---|
| [security.md](security.md) | 認証情報・シークレット管理・認可・CORS・入力検証 |
| [comments.md](comments.md) | コメント記載標準（目的・優先順位・ファイルヘッダー・業務ロジックコメント） |
| [naming-conventions.md](naming-conventions.md) | 命名の基本原則・ケース対応表・入出力データ型の命名パターン |
| [logging.md](logging.md) | ログレベルの使い分け・出力禁止事項 |
| [error-handling.md](error-handling.md) | 例外設計・エラーコード・グローバルハンドリング |

---

# レイヤー別の規約

| レイヤー | 言語 / フレームワーク | code-style | comments | api-design | testing |
|---|---|---|---|---|---|
| backend | Java（Spring Boot） | [backend/code-style.md](backend/code-style.md) | [backend/comments.md](backend/comments.md) | [backend/api-design.md](backend/api-design.md) | [backend/testing.md](backend/testing.md) |
| frontend | TypeScript（React） | [frontend/code-style.md](frontend/code-style.md) | [frontend/comments.md](frontend/comments.md) | [frontend/api-design.md](frontend/api-design.md) | [frontend/testing.md](frontend/testing.md) |
| ai | Python（FastAPI） | [ai/code-style.md](ai/code-style.md) | [ai/comments.md](ai/comments.md) | [ai/api-design.md](ai/api-design.md) | [ai/testing.md](ai/testing.md) |

---

# 位置づけ

- `.claude/context/` : 本プロジェクト（Skilize）固有のアーキテクチャ・技術スタック・API一覧などの解説資料
- `.claude/rules/` : 特定プロジェクトに依存しない、汎用的な規約のみを抽出したルール集（他プロジェクトへ流用可能）

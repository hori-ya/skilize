# API リファレンス

---

# API 設計ルール

- ベースパス: `/api`
- 認証不要: `POST /api/auth/login`、`GET /api/health`
- 全エンドポイントに `Authorization: Bearer <JWT>` 必須（上記以外）
- レスポンス形式: JSON
- エラーレスポンス: `{ "code": "ERROR_CODE", "message": "..." }`
- バリデーションエラー: `{ "errors": [{ "field": "...", "message": "..." }] }`

---

# 認証・セキュリティ

- パスワードハッシュ: BCrypt コストファクター 12
- JWT 有効期限: `JWT_EXPIRATION_MS`（デフォルト 28800000ms = 8時間）
- `is_initial_password = true` の場合、`/api/auth/change-password` 以外は 403 を返す（`InitialPasswordFilter`）
- CORS 許可オリジン: `FRONTEND_ORIGIN` 環境変数（デフォルト `http://localhost:5173,http://localhost:8081`）
- ロール制御: `@PreAuthorize` / `@EnableMethodSecurity` で実装

---

# エンドポイント一覧

```
# 認証
POST   /api/auth/login
GET    /api/auth/me
POST   /api/auth/change-password
POST   /api/auth/logout

# ダッシュボード
GET    /api/dashboard

# グラフ
GET    /api/charts/radar
GET    /api/charts/growth
GET    /api/charts/heatmap
GET    /api/charts/timeline

# 棚卸
GET    /api/inventories/mine
POST   /api/inventories
GET    /api/inventories/{id}
PUT    /api/inventories/{id}/it-skill-details
PUT    /api/inventories/{id}/qualification-details
PUT    /api/inventories/{id}/seminar-details
PATCH  /api/inventories/{id}/it-skill-details/{detailId}
POST   /api/inventories/{id}/submit
GET    /api/inventories/{id}/comparison
GET    /api/inventories/{id}/goal-review
PUT    /api/inventories/{id}/goal-review
POST   /api/inventories/{id}/goal-review/complete
GET    /api/inventories/{id}/goals
PUT    /api/inventories/{id}/goals
POST   /api/inventories/{id}/goals/complete

# 帳票出力
GET    /api/inventories/{id}/report

# AI チャット
POST   /api/ai/chat

# AI 分析
GET    /api/users/me/ai-analyses
GET    /api/users/{userId}/ai-analyses        (TL/ADMIN)

# ユーザー管理（ADMIN）
GET    /api/users
POST   /api/users
PUT    /api/users/{id}
PATCH  /api/users/{id}/deactivate
PATCH  /api/users/{id}/activate
POST   /api/users/{id}/reset-password

# チーム照会（TL/ADMIN）
GET    /api/users/me/team-members
GET    /api/users/{id}/inventories

# 期待コメント（TL/ADMIN）
GET    /api/users/{userId}/expectations
PUT    /api/users/{userId}/expectations/tl
PUT    /api/users/{userId}/expectations/company

# 面談（TL/ADMIN）
GET    /api/interviews/inventory/{inventoryId}
PUT    /api/interviews/inventory/{inventoryId}
GET    /api/interviews/inventory/{inventoryId}/prev-year

# マスタ Excel 出力・取込（ADMIN）
GET    /api/master-excel/it-skills/download
POST   /api/master-excel/it-skills/upload
GET    /api/master-excel/qualifications/download
POST   /api/master-excel/qualifications/upload
GET    /api/master-excel/ad-seminars/download
POST   /api/master-excel/ad-seminars/upload

# マスタ（TL/ADMIN）
GET    /api/it-skills
POST   /api/it-skills
PUT    /api/it-skills/{id}
DELETE /api/it-skills/{id}
PATCH  /api/it-skills/{id}/restore
GET    /api/it-skills/custom-unregistered
POST   /api/it-skills/promote
GET    /api/qualifications
POST   /api/qualifications
PUT    /api/qualifications/{id}
DELETE /api/qualifications/{id}
PATCH  /api/qualifications/{id}/restore
GET    /api/qualifications/custom-unregistered
POST   /api/qualifications/promote
GET    /api/ad-seminars
POST   /api/ad-seminars
PUT    /api/ad-seminars/{id}
DELETE /api/ad-seminars/{id}
PATCH  /api/ad-seminars/{id}/restore
GET    /api/it-skill-categories
POST   /api/it-skill-categories
PUT    /api/it-skill-categories/{id}
DELETE /api/it-skill-categories/{id}
GET    /api/qualification-categories
POST   /api/qualification-categories
PUT    /api/qualification-categories/{id}
DELETE /api/qualification-categories/{id}
GET    /api/ad-seminar-categories
POST   /api/ad-seminar-categories
PUT    /api/ad-seminar-categories/{id}
DELETE /api/ad-seminar-categories/{id}
GET    /api/seminar-categories
POST   /api/seminar-categories
PUT    /api/seminar-categories/{id}
DELETE /api/seminar-categories/{id}
GET    /api/skill-levels
POST   /api/skill-levels
PUT    /api/skill-levels/{id}
DELETE /api/skill-levels/{id}

# 年度（ADMIN）
GET    /api/fiscal-years
GET    /api/fiscal-years/current
POST   /api/fiscal-years
PUT    /api/fiscal-years/{id}
GET    /api/fiscal-year-settings
PUT    /api/fiscal-year-settings

# ヘルスチェック
GET    /api/health
```

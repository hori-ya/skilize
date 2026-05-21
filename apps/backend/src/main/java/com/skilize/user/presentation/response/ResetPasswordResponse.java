package com.skilize.user.presentation.response;

/**
 * パスワードリセットレスポンス。POST /api/users/{id}/reset-password のレスポンスに使用する（ADMIN のみ実行可）。
 * 一時パスワードを平文で返す。ユーザーは次回ログイン時に変更を求められる。
 *
 * @param temporaryPassword 自動生成された一時パスワード（平文）
 */
public record ResetPasswordResponse(String temporaryPassword) {}

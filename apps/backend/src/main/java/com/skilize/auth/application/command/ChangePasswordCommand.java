package com.skilize.auth.application.command;

/**
 * パスワード変更操作のコマンド。AuthService.changePassword() に渡す変更情報を保持する。
 *
 * @param currentPassword 現在のパスワード（本人確認用）
 * @param newPassword     新しいパスワード（最低 8 文字。Service 層で BCrypt ハッシュ化して保存）
 */
public record ChangePasswordCommand(String currentPassword, String newPassword) {}

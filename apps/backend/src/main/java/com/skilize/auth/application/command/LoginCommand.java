package com.skilize.auth.application.command;

/**
 * ログイン操作のコマンド。AuthService.login() に渡す認証情報を保持する。
 *
 * @param userId   ログインID（例: "user01"）。内部 PK ではなく文字列識別子
 * @param password 平文パスワード。Service 層で BCrypt ハッシュと照合する
 */
public record LoginCommand(String userId, String password) {}

package com.skilize.user.presentation.request;

import jakarta.validation.constraints.NotBlank;

/**
 * ユーザー新規登録リクエスト。POST /api/users のリクエストボディ（ADMIN のみ実行可）。
 * 初期パスワードはサーバー側で自動生成し、レスポンスで返す。
 *
 * @param userId    ログインID（必須・一意）
 * @param name      氏名（必須）
 * @param email     メールアドレス（任意）
 * @param role      ロール（GENERAL / TL / ADMIN）（必須）
 * @param tlUserId  所属TLのユーザー内部PK（GENERAL の場合に設定）
 */
public record CreateUserRequest(
        @NotBlank String userId,
        @NotBlank String name,
        String email,
        @NotBlank String role,
        Integer tlUserId
) {}

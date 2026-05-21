package com.skilize.auth.presentation.request;

import jakarta.validation.constraints.NotBlank;

/**
 * ログインリクエスト。POST /api/auth/login のリクエストボディ。
 *
 * @param userId   ログインID（例: "user01"）。内部 PK ではなく文字列識別子
 * @param password 平文パスワード
 */
public record LoginRequest(
        @NotBlank String userId,
        @NotBlank String password
) {}

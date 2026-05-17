package com.skilize.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * ログインリクエスト。userId は文字列形式のユーザーID（例: "user01"）。内部 PK ではない。
 */
public record LoginRequest(
        @NotBlank String userId,
        @NotBlank String password
) {}

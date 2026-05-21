package com.skilize.auth.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * パスワード変更リクエスト。POST /api/auth/change-password のリクエストボディ。
 * 現在のパスワードで本人確認を行ってから新しいパスワードに変更する。
 *
 * @param currentPassword 現在のパスワード（本人確認用）
 * @param newPassword     新しいパスワード（最低 8 文字）
 */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, message = "8 文字以上で入力してください") String newPassword
) {}

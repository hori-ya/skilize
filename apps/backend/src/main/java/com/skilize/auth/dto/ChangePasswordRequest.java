package com.skilize.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * パスワード変更リクエスト。現在のパスワードで本人確認を行ってから新しいパスワードに変更する。
 * newPassword は最低 8 文字を強制する（@Size バリデーション）。
 */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, message = "8 文字以上で入力してください") String newPassword
) {}

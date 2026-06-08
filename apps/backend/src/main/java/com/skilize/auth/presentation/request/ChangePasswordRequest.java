/**************************************************************************************************************
 * 機能ID      ：AUTH
 * 機能名      ：認証機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * POST /api/auth/change-password のリクエストボディを受け取る record クラス。
 * 現在のパスワードで本人確認を行った上で新しいパスワードに変更する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
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
        @NotBlank @Size(min = 8) String newPassword
) {}

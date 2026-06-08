/**************************************************************************************************************
 * 機能ID      ：USR
 * 機能名      ：ユーザー管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ユーザー情報更新APIのリクエストボディ（ADMIN 専用）。
 * ロール文字列は GENERAL / TL / ADMIN のいずれかのみ受け付ける。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.user.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * ユーザー情報更新リクエスト。PUT /api/users/{id} のリクエストボディ（ADMIN のみ実行可）。
 *
 * @param name     氏名（必須）
 * @param email    メールアドレス（任意）
 * @param role     ロール（GENERAL / TL / ADMIN）（必須・列挙値のみ許可）
 * @param tlUserId 所属TLのユーザー内部PK（GENERAL の場合に設定）
 * @param active   有効フラグ（false で論理無効化）
 */
public record UpdateUserRequest(
        @NotBlank String name,
        String email,
        @NotBlank @Pattern(regexp = "GENERAL|TL|ADMIN") String role,
        Integer tlUserId,
        Boolean active
) {}

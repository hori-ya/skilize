/**************************************************************************************************************
 * 機能ID      ：USR
 * 機能名      ：ユーザー管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ユーザー1件情報のレスポンス（ADMIN 向け管理画面用）。
 * User エンティティと所属TL名マップから生成する静的ファクトリメソッドを提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.user.presentation.response;

import com.skilize.user.domain.User;

import java.util.Map;

/**
 * ユーザー1件のレスポンス。GET /api/users などのレスポンスに使用する（ADMIN 向け管理画面）。
 *
 * @param id                ユーザー内部PK
 * @param userId            ログインID
 * @param name              氏名
 * @param email             メールアドレス
 * @param role              ロール（GENERAL / TL / ADMIN）
 * @param tlUserId          所属TLのユーザー内部PK（null の場合は未設定）
 * @param tlName            所属TL氏名（null の場合は未設定）
 * @param isInitialPassword 初期パスワードフラグ（true の場合、次回ログイン時にパスワード変更を強制）
 * @param isActive          有効フラグ
 * @param createdAt         作成日時（ISO-8601 形式）
 */
public record UserResponse(int id, String userId, String name, String email, String role,
                            Integer tlUserId, String tlName, boolean isInitialPassword,
                            boolean isActive, String createdAt) {

    /**
     * User エンティティと所属TL名マップからレスポンスを生成する。
     *
     * @param u        変換元のユーザーエンティティ
     * @param nameById ユーザー内部PK → 氏名のマップ（TL名の解決に使用）
     * @return ユーザーレスポンス
     */
    public static UserResponse from(User u, Map<Integer, String> nameById) {
        return new UserResponse(
                u.getId(), u.getUserId(), u.getName(), u.getEmail(),
                u.getRole().name(),
                u.getTlUserId(),
                u.getTlUserId() != null ? nameById.get(u.getTlUserId()) : null,
                u.isInitialPassword(), u.isActive(),
                u.getCreatedAt() != null ? u.getCreatedAt().toString() : null
        );
    }
}

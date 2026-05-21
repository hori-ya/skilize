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

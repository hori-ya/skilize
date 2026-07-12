/**************************************************************************************************************
 * 機能ID      ：USR
 * 機能名      ：ユーザー管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ユーザーJPAエンティティ。users テーブルとのマッピングを担う。
 * Spring Security への対応は user.infrastructure.security.UserPrincipal が本エンティティ由来の
 * ドメインモデル（User）をラップして行うため、本エンティティ自体は UserDetails を実装しない。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.User から分離（JPAアノテーションはこちらにのみ残す）
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.user.infrastructure.persistence.entity;

import com.skilize.user.domain.model.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * ユーザーJPAエンティティ。
 * フィールドは @Setter を付けず、ドメインメソッド（create/update/changePassword/resetPassword）でのみ変更する。
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class UserEntity {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ユーザーID
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    // 氏名
    @Column(nullable = false)
    private String name;

    // メールアドレス（任意項目）
    private String email;

    // パスワードハッシュ（BCrypt コスト12・APIレスポンス除外）
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // ロール
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // TLユーザーID（null は上長なし）
    @Column(name = "tl_user_id")
    private Integer tlUserId;

    // 初回パスワードフラグ（true=変更強制）
    @Column(name = "is_initial_password", nullable = false)
    private boolean initialPassword;

    // 有効フラグ（false=無効化済み）
    @Column(name = "is_active", nullable = false)
    private boolean active;

    // 作成日時
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static UserEntity create(String userId, String name, String email, Role role,
                                    Integer tlUserId, String passwordHash) {
        UserEntity u = new UserEntity();
        u.userId = userId;
        u.name = name;
        u.email = email;
        u.passwordHash = passwordHash;
        u.role = role;
        u.tlUserId = tlUserId;
        u.initialPassword = true; // 新規ユーザーは必ず初回パスワード変更が必要
        u.active = true;
        return u;
    }

    public void update(String name, String email, Role role, Integer tlUserId, boolean active) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.tlUserId = tlUserId;
        this.active = active;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.initialPassword = false;
    }

    public void resetPassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.initialPassword = true;
    }
}

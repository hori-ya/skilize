/**************************************************************************************************************
 * 機能ID      ：USR
 * 機能名      ：ユーザー管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ユーザードメインモデル。
 * フィールドはドメインメソッドでのみ変更し、@Setter は使用しない。
 * Spring Security の認証プリンシパルとしては user/infrastructure/security/UserPrincipal がこのモデルをラップして扱う
 * （本モデル自体は Spring Security に依存しない）。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.UserEntity から分離。
 *                     UserDetails実装はuser.infrastructure.security.UserPrincipalへ切り出し、本モデルはSpringに非依存化
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.user.domain.model;

import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * ユーザー。JPA/Springに依存しない純粋なドメインモデル。
 * フィールドは @Setter を付けず、ドメインメソッド（create/update/changePassword/resetPassword）でのみ変更する。
 *
 * 項目（論理名）:
 *   ユーザーID          - ログインID（英数字・記号使用可、重複不可）
 *   氏名                - 表示名
 *   メールアドレス       - 任意項目。一意性制約なし
 *   パスワードハッシュ   - BCrypt コスト12 でハッシュ化済み。API レスポンスに含めない
 *   ロール              - GENERAL=一般 / TL=チームリーダー / ADMIN=管理者
 *   TLユーザーID        - 上長の内部ID（自己参照。NULLは上長なし）
 *   初回パスワードフラグ - true=パスワード変更強制。InitialPasswordFilter が参照する
 *   有効フラグ          - false=無効化済み。JwtAuthenticationFilter が認証時に確認する
 */
@Getter
public class User {

    private Integer id;
    private String userId;
    private String name;
    private String email;
    private String passwordHash;
    private Role role;
    private Integer tlUserId;
    private boolean initialPassword;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * ユーザーを新規作成する。is_initial_password=true、is_active=true で初期化する。
     *
     * @param userId       ログインID
     * @param name         氏名
     * @param email        メールアドレス（null 可）
     * @param role         ロール
     * @param tlUserId     所属TLのユーザー内部PK（null で上長なし）
     * @param passwordHash BCrypt でハッシュ化済みのパスワード
     * @return 新規作成されたユーザー
     */
    public static User create(String userId, String name, String email, Role role,
                               Integer tlUserId, String passwordHash) {
        User u = new User();
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

    /**
     * 永続化済みの状態からユーザーを復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static User reconstruct(Integer id, String userId, String name, String email, String passwordHash,
                                   Role role, Integer tlUserId, boolean initialPassword, boolean active,
                                   OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        User u = new User();
        u.id = id;
        u.userId = userId;
        u.name = name;
        u.email = email;
        u.passwordHash = passwordHash;
        u.role = role;
        u.tlUserId = tlUserId;
        u.initialPassword = initialPassword;
        u.active = active;
        u.createdAt = createdAt;
        u.updatedAt = updatedAt;
        return u;
    }

    /**
     * ユーザー情報（氏名・メール・ロール・上長・有効フラグ）を更新する。
     *
     * @param name     氏名
     * @param email    メールアドレス（null 可）
     * @param role     ロール
     * @param tlUserId 所属TLのユーザー内部PK（null で上長なし）
     * @param active   有効フラグ（false で論理無効化）
     */
    public void update(String name, String email, Role role, Integer tlUserId, boolean active) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.tlUserId = tlUserId;
        this.active = active;
    }

    /**
     * パスワードを変更し、初回パスワードフラグを解除する。
     *
     * @param newPasswordHash BCrypt でハッシュ化済みの新パスワード
     */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.initialPassword = false; // 変更完了で初回PW強制を解除する
    }

    /**
     * パスワードをリセットし、初回パスワードフラグを再設定する。
     * リセット後は次回ログイン時にパスワード変更が強制される。
     *
     * @param passwordHash BCrypt でハッシュ化済みの仮パスワード
     */
    public void resetPassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.initialPassword = true; // リセット後は再度初回PW変更を強制する
    }
}

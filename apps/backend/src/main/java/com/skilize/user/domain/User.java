package com.skilize.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

/**
 * ユーザー。Spring Security の UserDetails を実装し、認証プリンシパルとしても機能する。
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
@Entity
@Table(name = "users")
@Getter
@lombok.NoArgsConstructor
public class User implements UserDetails {

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

    public void update(String name, String email, Role role, Integer tlUserId, boolean active) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.tlUserId = tlUserId;
        this.active = active;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.initialPassword = false; // 変更完了で初回PW強制を解除する
    }

    public void resetPassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.initialPassword = true; // リセット後は再度初回PW変更を強制する
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return userId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}

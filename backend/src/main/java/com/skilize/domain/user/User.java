package com.skilize.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@lombok.NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(nullable = false)
    private String name;

    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "tl_user_id")
    private Integer tlUserId;

    @Column(name = "is_initial_password", nullable = false)
    private boolean initialPassword;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

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
        u.initialPassword = true;
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

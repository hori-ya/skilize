package com.skilize.auth.application;

import com.skilize.auth.application.command.ChangePasswordCommand;
import com.skilize.auth.application.command.LoginCommand;
import com.skilize.auth.application.query.LoginQueryResult;
import com.skilize.auth.application.query.MeQueryResult;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.shared.infrastructure.JwtUtil;
import com.skilize.user.domain.Role;
import com.skilize.user.domain.User;
import com.skilize.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService の単体テスト。Repository・PasswordEncoder・JwtUtil をすべてモック化して検証する。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks AuthService authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.create("user01", "テストユーザー", "user@example.com",
                Role.GENERAL, null, "$2a$12$hashedPassword");
        ReflectionTestUtils.setField(activeUser, "id", 1);
    }

    // ═══════════════════════════════════════════════════════════
    //  login
    // ═══════════════════════════════════════════════════════════

    @Nested
    class Login {

        @Test
        void 正常系_JWTとユーザー情報を返す() {
            when(userRepository.findByUserId("user01")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("password", activeUser.getPasswordHash())).thenReturn(true);
            when(jwtUtil.generateToken(activeUser)).thenReturn("jwt-token");

            LoginQueryResult result = authService.login(new LoginCommand("user01", "password"));

            assertThat(result.token()).isEqualTo("jwt-token");
            assertThat(result.user().name()).isEqualTo("テストユーザー");
            assertThat(result.user().role()).isEqualTo("GENERAL");
            assertThat(result.user().isInitialPassword()).isTrue();
        }

        @Test
        void 異常系_ユーザー不在_AUTH_FAILEDをスロー() {
            when(userRepository.findByUserId("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginCommand("unknown", "pw")))
                    .isInstanceOf(AuthException.class)
                    .hasFieldOrPropertyWithValue("code", "AUTH_FAILED");
        }

        @Test
        void 異常系_パスワード不一致_AUTH_FAILEDをスロー() {
            when(userRepository.findByUserId("user01")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("wrong", activeUser.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginCommand("user01", "wrong")))
                    .isInstanceOf(AuthException.class)
                    .hasFieldOrPropertyWithValue("code", "AUTH_FAILED");
        }

        @Test
        void 異常系_無効化アカウント_FORBIDDENをスロー() {
            User inactiveUser = User.create("user01", "名前", null, Role.GENERAL, null, "hash");
            inactiveUser.update("名前", null, Role.GENERAL, null, false);
            when(userRepository.findByUserId("user01")).thenReturn(Optional.of(inactiveUser));

            assertThatThrownBy(() -> authService.login(new LoginCommand("user01", "pw")))
                    .isInstanceOf(AuthException.class)
                    .hasFieldOrPropertyWithValue("code", "FORBIDDEN");
        }

        @Test
        void 異常系_ユーザー不在とパスワード不一致は同一エラー_ユーザー列挙攻撃対策() {
            when(userRepository.findByUserId("unknown")).thenReturn(Optional.empty());
            when(userRepository.findByUserId("user01")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(any(), any())).thenReturn(false);

            AuthException notFound = catchThrowableOfType(
                    () -> authService.login(new LoginCommand("unknown", "pw")), AuthException.class);
            AuthException badPassword = catchThrowableOfType(
                    () -> authService.login(new LoginCommand("user01", "pw")), AuthException.class);

            assertThat(notFound.getCode()).isEqualTo(badPassword.getCode());
            assertThat(notFound.getMessage()).isEqualTo(badPassword.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  changePassword
    // ═══════════════════════════════════════════════════════════

    @Nested
    class ChangePassword {

        @Test
        void 正常系_パスワードが更新されisInitialPasswordがfalseになる() {
            when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("oldpass", activeUser.getPasswordHash())).thenReturn(true);
            when(passwordEncoder.encode("newpass12")).thenReturn("$2a$12$newHash");

            authService.changePassword(new ChangePasswordCommand("oldpass", "newpass12"), activeUser);

            assertThat(activeUser.isInitialPassword()).isFalse();
            assertThat(activeUser.getPasswordHash()).isEqualTo("$2a$12$newHash");
        }

        @Test
        void 異常系_adminアカウント_FORBIDDENをスロー() {
            User adminUser = User.create("admin", "Admin", null, Role.ADMIN, null, "hash");

            assertThatThrownBy(() -> authService.changePassword(
                    new ChangePasswordCommand("old", "newpass12"), adminUser))
                    .isInstanceOf(AuthException.class)
                    .hasFieldOrPropertyWithValue("code", "FORBIDDEN");

            // admin チェックで早期リターンするため findById を呼ばない
            verify(userRepository, never()).findById(any());
        }

        @Test
        void 異常系_現在のパスワード不一致_AUTH_FAILEDをスロー() {
            when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("wrong", activeUser.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(
                    new ChangePasswordCommand("wrong", "newpass12"), activeUser))
                    .isInstanceOf(AuthException.class)
                    .hasFieldOrPropertyWithValue("code", "AUTH_FAILED");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  getMe
    // ═══════════════════════════════════════════════════════════

    @Nested
    class GetMe {

        @Test
        void 正常系_ユーザー情報を返す() {
            MeQueryResult result = authService.getMe(activeUser);

            assertThat(result.userId()).isEqualTo("user01");
            assertThat(result.name()).isEqualTo("テストユーザー");
            assertThat(result.role()).isEqualTo("GENERAL");
            assertThat(result.isInitialPassword()).isTrue();
            assertThat(result.isActive()).isTrue();
        }

        @Test
        void 正常系_TLユーザー設定あり_TL情報を返す() {
            User tlUser = User.create("tl01", "TLユーザー", null, Role.TL, null, "hash");
            ReflectionTestUtils.setField(tlUser, "id", 2);
            User userWithTl = User.create("user01", "一般ユーザー", null, Role.GENERAL, 2, "hash");
            when(userRepository.findById(2)).thenReturn(Optional.of(tlUser));

            MeQueryResult result = authService.getMe(userWithTl);

            assertThat(result.tlUser()).isNotNull();
            assertThat(result.tlUser().id()).isEqualTo(2);
            assertThat(result.tlUser().name()).isEqualTo("TLユーザー");
        }

        @Test
        void 正常系_TLユーザー設定なし_tlUserがnull() {
            MeQueryResult result = authService.getMe(activeUser);

            assertThat(result.tlUser()).isNull();
        }
    }
}

package com.skilize.auth.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skilize.auth.application.AuthService;
import com.skilize.auth.application.command.ChangePasswordCommand;
import com.skilize.auth.application.command.LoginCommand;
import com.skilize.auth.application.mapper.AuthApplicationMapper;
import com.skilize.auth.application.query.LoginQueryResult;
import com.skilize.auth.application.query.MeQueryResult;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.shared.infrastructure.InitialPasswordFilter;
import com.skilize.shared.infrastructure.JwtAuthenticationFilter;
import com.skilize.user.domain.Role;
import com.skilize.user.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController の Web レイヤーテスト。
 * DB 接続不要。JwtAuthenticationFilter と InitialPasswordFilter はモック化して、
 * 素通りするよう設定する。認証付きテストは SecurityMockMvcRequestPostProcessors.user() を使用する。
 */
@WebMvcTest(AuthController.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AuthService authService;
    @MockitoBean AuthApplicationMapper authApplicationMapper;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean InitialPasswordFilter initialPasswordFilter;

    private User generalUser;

    @BeforeEach
    void setUpFilters() throws Exception {
        // モックフィルターを素通り設定（doFilterInternal が呼ばれたらチェーンを次に進める）
        lenient().doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2))
                    .doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter)
                .doFilterInternal(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        lenient().doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2))
                    .doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(initialPasswordFilter)
                .doFilterInternal(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        generalUser = User.create("user01", "テストユーザー", null, Role.GENERAL, null, "hash");
    }

    // ═══════════════════════════════════════════════════════════
    //  POST /api/auth/login
    // ═══════════════════════════════════════════════════════════

    @Nested
    class Login {

        @Test
        void 正常系_200とJWTを返す() throws Exception {
            var command = new LoginCommand("user01", "password");
            var result = new LoginQueryResult(
                    "jwt-token",
                    new LoginQueryResult.UserInfo(1, "テストユーザー", "GENERAL", false, null));
            when(authApplicationMapper.toCommand(any())).thenReturn(command);
            when(authService.login(command)).thenReturn(result);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("userId", "user01", "password", "password"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.user.role").value("GENERAL"))
                    .andExpect(jsonPath("$.user.name").value("テストユーザー"));
        }

        @Test
        void 異常系_userIdが空_400バリデーションエラー() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("userId", "", "password", "password"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void 異常系_passwordが空_400バリデーションエラー() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("userId", "user01", "password", ""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void 異常系_認証失敗_401を返す() throws Exception {
            when(authApplicationMapper.toCommand(any())).thenReturn(new LoginCommand("x", "x"));
            when(authService.login(any()))
                    .thenThrow(new AuthException("AUTH_FAILED", "ユーザーIDまたはパスワードが違います"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("userId", "x", "password", "x"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
        }

        @Test
        void 異常系_アカウント無効化_403を返す() throws Exception {
            when(authApplicationMapper.toCommand(any())).thenReturn(new LoginCommand("x", "x"));
            when(authService.login(any()))
                    .thenThrow(new AuthException("FORBIDDEN", "このアカウントは無効化されています"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("userId", "x", "password", "x"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/auth/me
    // ═══════════════════════════════════════════════════════════

    @Nested
    class GetMe {

        @Test
        void 正常系_200とユーザー情報を返す() throws Exception {
            var result = new MeQueryResult(1, "user01", "テストユーザー", null, "GENERAL", false, null, true);
            when(authService.getMe(any())).thenReturn(result);

            mockMvc.perform(get("/api/auth/me").with(user(generalUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("user01"))
                    .andExpect(jsonPath("$.role").value("GENERAL"));
        }

        @Test
        void 異常系_認証なし_401を返す() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  POST /api/auth/change-password
    // ═══════════════════════════════════════════════════════════

    @Nested
    class ChangePassword {

        @Test
        void 正常系_204を返す() throws Exception {
            when(authApplicationMapper.toCommand(any(com.skilize.auth.presentation.request.ChangePasswordRequest.class)))
                    .thenReturn(new ChangePasswordCommand("oldpass1", "newpass12"));
            doNothing().when(authService).changePassword(any(), any());

            mockMvc.perform(post("/api/auth/change-password")
                            .with(user(generalUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("currentPassword", "oldpass1", "newPassword", "newpass12"))))
                    .andExpect(status().isNoContent());
        }

        @Test
        void 異常系_認証なし_401を返す() throws Exception {
            mockMvc.perform(post("/api/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("currentPassword", "old", "newPassword", "newpass12"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void 異常系_newPasswordが8文字未満_400バリデーションエラー() throws Exception {
            mockMvc.perform(post("/api/auth/change-password")
                            .with(user(generalUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("currentPassword", "oldpass1", "newPassword", "short"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void 異常系_パスワード変更禁止アカウント_403を返す() throws Exception {
            when(authApplicationMapper.toCommand(any(com.skilize.auth.presentation.request.ChangePasswordRequest.class)))
                    .thenReturn(new ChangePasswordCommand("old", "newpass12"));
            doThrow(new AuthException("FORBIDDEN", "このアカウントはパスワードを変更できません"))
                    .when(authService).changePassword(any(), any());

            mockMvc.perform(post("/api/auth/change-password")
                            .with(user(generalUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("currentPassword", "old", "newPassword", "newpass12"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  POST /api/auth/logout
    // ═══════════════════════════════════════════════════════════

    @Test
    void logout_正常系_204を返す() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(user(generalUser)))
                .andExpect(status().isNoContent());
    }
}

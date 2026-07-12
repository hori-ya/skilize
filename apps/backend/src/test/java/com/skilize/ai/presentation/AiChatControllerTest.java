package com.skilize.ai.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skilize.ai.application.AiChatService;
import com.skilize.ai.application.mapper.AiChatApplicationMapper;
import com.skilize.ai.application.query.AiChatQueryResult;
import com.skilize.shared.presentation.GlobalExceptionHandler;
import com.skilize.user.domain.model.Role;
import com.skilize.user.domain.model.User;
import com.skilize.user.infrastructure.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AiChatController の Web レイヤーテスト。
 * DB・Python AI サービス接続不要。standaloneSetup によりコントローラーロジックを検証する。
 * SecurityContextHolder を直接設定して @AuthenticationPrincipal を解決する。
 */
@ExtendWith(MockitoExtension.class)
class AiChatControllerTest {

    @Mock AiChatService aiChatService;
    @Mock AiChatApplicationMapper mapper;
    @InjectMocks AiChatController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();
    private User generalUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        generalUser = User.create("user01", "テストユーザー", null, Role.GENERAL, null, "hash");
        ReflectionTestUtils.setField(generalUser, "id", 1);
        // SecurityContextHolder にテストユーザーの認証情報を設定する
        // （スタンドアローン MockMvc は同一スレッドで実行されるためスレッドローカルが有効）
        UserPrincipal principal = new UserPrincipal(generalUser);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ═══════════════════════════════════════════════════════════
    //  POST /api/ai/chat
    // ═══════════════════════════════════════════════════════════

    @Nested
    class Chat {

        @Test
        void 正常系_通常モードでAI応答を返す() throws Exception {
            when(mapper.toCommand(any(), anyInt())).thenReturn(null);
            when(aiChatService.chat(any())).thenReturn(new AiChatQueryResult("こんにちは！", "NORMAL"));

            mockMvc.perform(post("/api/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "message", "こんにちは",
                                    "mode", "NORMAL",
                                    "history", List.of()
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response").value("こんにちは！"))
                    .andExpect(jsonPath("$.mode").value("NORMAL"));
        }

        @Test
        void 正常系_会話履歴付きで送信できる() throws Exception {
            when(mapper.toCommand(any(), anyInt())).thenReturn(null);
            when(aiChatService.chat(any())).thenReturn(new AiChatQueryResult("了解しました。", "CAREER"));

            List<Map<String, String>> history = List.of(
                    Map.of("role", "user", "content", "キャリアについて相談したい"),
                    Map.of("role", "assistant", "content", "もちろんです。どのような点が気になりますか？")
            );

            mockMvc.perform(post("/api/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "message", "スキルアップの方向性を教えてください",
                                    "mode", "CAREER",
                                    "history", history
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response").value("了解しました。"));
        }

        @Test
        void 異常系_メッセージ空はバリデーションエラー() throws Exception {
            mockMvc.perform(post("/api/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "message", "",
                                    "mode", "NORMAL"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void 異常系_不正なモードはバリデーションエラー() throws Exception {
            mockMvc.perform(post("/api/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "message", "テスト",
                                    "mode", "INVALID_MODE"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void 異常系_AIサービス障害時は503を返す() throws Exception {
            when(mapper.toCommand(any(), anyInt())).thenReturn(null);
            when(aiChatService.chat(any()))
                    .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE"));

            mockMvc.perform(post("/api/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "message", "テスト",
                                    "mode", "NORMAL"
                            ))))
                    .andExpect(status().isServiceUnavailable());
        }
    }
}

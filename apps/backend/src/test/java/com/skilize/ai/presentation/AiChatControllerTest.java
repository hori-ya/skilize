package com.skilize.ai.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skilize.ai.application.AiChatService;
import com.skilize.ai.application.mapper.AiChatApplicationMapper;
import com.skilize.ai.application.query.AiChatQueryResult;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AiChatController の Web レイヤーテスト。
 * DB・Python AI サービス接続不要。フィルターはモック化して素通り設定。
 */
@WebMvcTest(AiChatController.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class AiChatControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AiChatService aiChatService;
    @MockitoBean AiChatApplicationMapper mapper;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean InitialPasswordFilter initialPasswordFilter;

    private User generalUser;

    @BeforeEach
    void setUpFilters() throws Exception {
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
        ReflectionTestUtils.setField(generalUser, "id", 1);
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
                            .with(user(generalUser))
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
                            .with(user(generalUser))
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
        void 異常系_未認証はアクセス不可() throws Exception {
            mockMvc.perform(post("/api/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "message", "test",
                                    "mode", "NORMAL"
                            ))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void 異常系_メッセージ空はバリデーションエラー() throws Exception {
            mockMvc.perform(post("/api/ai/chat")
                            .with(user(generalUser))
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
                            .with(user(generalUser))
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
                    .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AIサービスが一時的に利用できません"));

            mockMvc.perform(post("/api/ai/chat")
                            .with(user(generalUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "message", "テスト",
                                    "mode", "NORMAL"
                            ))))
                    .andExpect(status().isServiceUnavailable());
        }
    }
}

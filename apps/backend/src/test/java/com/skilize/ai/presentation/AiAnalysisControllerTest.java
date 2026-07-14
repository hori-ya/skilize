package com.skilize.ai.presentation;

import com.skilize.ai.application.AiAnalysisService;
import com.skilize.ai.application.query.AiAnalysisQueryResult;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.shared.presentation.GlobalExceptionHandler;
import com.skilize.user.application.UserService;
import com.skilize.user.domain.model.Role;
import com.skilize.user.domain.model.User;
import com.skilize.user.infrastructure.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AiAnalysisController の Web レイヤーテスト。DB 接続不要。
 * standaloneSetup では SecurityContextPersistenceFilter が存在しないため SecurityContextHolder へ直接設定する。
 */
@ExtendWith(MockitoExtension.class)
class AiAnalysisControllerTest {

    @Mock AiAnalysisService aiAnalysisService;
    @Mock UserService userService;
    @InjectMocks AiAnalysisController controller;

    MockMvc mockMvc;
    private User generalUser;
    private User tlUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        generalUser = User.create("user01", "テストユーザー", null, Role.GENERAL, null, "hash");
        ReflectionTestUtils.setField(generalUser, "id", 1);
        tlUser = User.create("tl01", "TL", null, Role.TL, null, "hash");
        ReflectionTestUtils.setField(tlUser, "id", 2);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getMyAnalyses_正常系_200と自分の分析結果一覧を返す() throws Exception {
        authenticateAs(generalUser);
        AiAnalysisQueryResult result = new AiAnalysisQueryResult(1, 2, "COMPLETED", null, null,
                OffsetDateTime.now(), OffsetDateTime.now());
        when(aiAnalysisService.findByUserId(1)).thenReturn(List.of(result));

        mockMvc.perform(get("/api/users/me/ai-analyses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void getMemberAnalyses_正常系_担当TL_200を返す() throws Exception {
        authenticateAs(tlUser);
        User target = User.create("user01", "対象ユーザー", null, Role.GENERAL, 2, "hash");
        ReflectionTestUtils.setField(target, "id", 5);
        when(userService.findById(5)).thenReturn(Optional.of(target));
        when(aiAnalysisService.findByUserId(5)).thenReturn(List.of());

        mockMvc.perform(get("/api/users/5/ai-analyses"))
                .andExpect(status().isOk());
    }

    @Test
    void getMemberAnalyses_異常系_担当外TL_403を返す() throws Exception {
        authenticateAs(tlUser);
        User otherTeamMember = User.create("user02", "他チームユーザー", null, Role.GENERAL, 99, "hash");
        ReflectionTestUtils.setField(otherTeamMember, "id", 6);
        when(userService.findById(6)).thenReturn(Optional.of(otherTeamMember));

        mockMvc.perform(get("/api/users/6/ai-analyses"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMemberAnalyses_異常系_対象ユーザー不在_404を返す() throws Exception {
        authenticateAs(tlUser);
        when(userService.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/99/ai-analyses"))
                .andExpect(status().isNotFound());
    }
}

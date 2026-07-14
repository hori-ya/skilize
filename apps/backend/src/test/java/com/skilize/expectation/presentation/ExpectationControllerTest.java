package com.skilize.expectation.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skilize.expectation.application.ExpectationService;
import com.skilize.expectation.application.query.ExpectationQueryResult;
import com.skilize.shared.presentation.GlobalExceptionHandler;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ExpectationController の Web レイヤーテスト。DB 接続不要・@PreAuthorize は standaloneSetup では
 * 評価されないため、Service 層の例外レスポンス整形（GlobalExceptionHandler経由）を中心に検証する。
 */
@ExtendWith(MockitoExtension.class)
class ExpectationControllerTest {

    @Mock ExpectationService expectationService;
    @InjectMocks ExpectationController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();
    private User tlUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        tlUser = User.create("tl01", "TL", null, Role.TL, null, "hash");
        ReflectionTestUtils.setField(tlUser, "id", 2);
        authenticateAs(tlUser);
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
    void get_正常系_200と期待情報を返す() throws Exception {
        when(expectationService.getForUser(eq(5), any())).thenReturn(new ExpectationQueryResult("TL期待", "会社期待"));

        mockMvc.perform(get("/api/users/5/expectations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tlExpectation").value("TL期待"))
                .andExpect(jsonPath("$.companyExpectation").value("会社期待"));
    }

    @Test
    void get_異常系_担当外TL_403を返す() throws Exception {
        when(expectationService.getForUser(eq(5), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "EXPECTATION_TEAM_MEMBER_ONLY"));

        mockMvc.perform(get("/api/users/5/expectations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveTl_正常系_200で保存結果を返す() throws Exception {
        when(expectationService.saveTlExpectation(eq(5), any(), eq("新しい期待")))
                .thenReturn(new ExpectationQueryResult("新しい期待", null));

        mockMvc.perform(put("/api/users/5/expectations/tl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("expectation", "新しい期待"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tlExpectation").value("新しい期待"));
    }

    @Test
    void saveTl_異常系_担当外TL_403を返す() throws Exception {
        when(expectationService.saveTlExpectation(eq(5), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "EXPECTATION_ASSIGNED_TL_ONLY"));

        mockMvc.perform(put("/api/users/5/expectations/tl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("expectation", "コメント"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveCompany_正常系_200で保存結果を返す() throws Exception {
        when(expectationService.saveCompanyExpectation(eq(5), any(), eq("会社期待")))
                .thenReturn(new ExpectationQueryResult(null, "会社期待"));

        mockMvc.perform(put("/api/users/5/expectations/company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("expectation", "会社期待"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyExpectation").value("会社期待"));
    }

    @Test
    void saveCompany_異常系_ADMIN以外_403を返す() throws Exception {
        when(expectationService.saveCompanyExpectation(eq(5), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "EXPECTATION_ADMIN_ONLY"));

        mockMvc.perform(put("/api/users/5/expectations/company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("expectation", "コメント"))))
                .andExpect(status().isForbidden());
    }
}

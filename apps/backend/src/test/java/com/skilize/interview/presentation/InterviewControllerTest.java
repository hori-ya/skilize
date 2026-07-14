package com.skilize.interview.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skilize.interview.application.InterviewService;
import com.skilize.interview.domain.model.DetailType;
import com.skilize.interview.domain.model.InterviewDetailNote;
import com.skilize.interview.domain.model.InventoryInterview;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * InterviewController の Web レイヤーテスト。DB 接続不要。
 * standaloneSetup では SecurityContextPersistenceFilter が存在しないため SecurityContextHolder へ直接設定する。
 */
@ExtendWith(MockitoExtension.class)
class InterviewControllerTest {

    @Mock InterviewService interviewService;
    @InjectMocks InterviewController controller;

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
    void getMine_正常系_200と面談メモを返す() throws Exception {
        InventoryInterview interview = InventoryInterview.create(10, tlUser, "全体メモ");
        ReflectionTestUtils.setField(interview, "id", 100);
        when(interviewService.findMine(10, tlUser)).thenReturn(Optional.of(interview));
        when(interviewService.findDetailNotes(100)).thenReturn(List.of());

        mockMvc.perform(get("/api/interviews/inventory/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generalNote").value("全体メモ"))
                .andExpect(jsonPath("$.interviewerId").value(2));
    }

    @Test
    void getMine_異常系_未作成_404を返す() throws Exception {
        when(interviewService.findMine(10, tlUser)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/interviews/inventory/10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void save_正常系_200で保存した面談メモを返す() throws Exception {
        InventoryInterview saved = InventoryInterview.create(10, tlUser, "新しいメモ");
        ReflectionTestUtils.setField(saved, "id", 100);
        when(interviewService.save(any(Integer.class), any(), any(), any())).thenReturn(saved);
        InterviewDetailNote note = InterviewDetailNote.create(saved, DetailType.IT_SKILL, 1, "コメント");
        ReflectionTestUtils.setField(note, "id", 1000);
        when(interviewService.findDetailNotes(100)).thenReturn(List.of(note));

        mockMvc.perform(put("/api/interviews/inventory/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "generalNote", "新しいメモ",
                                "detailNotes", List.of(Map.of(
                                        "detailType", "IT_SKILL", "detailId", 1, "note", "コメント"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generalNote").value("新しいメモ"))
                .andExpect(jsonPath("$.detailNotes[0].detailType").value("IT_SKILL"));
    }

    @Test
    void save_異常系_detailNotesがnull_400バリデーションエラー() throws Exception {
        mockMvc.perform(put("/api/interviews/inventory/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("generalNote", "メモ"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPrevYear_正常系_200と前年度面談メモを返す() throws Exception {
        InventoryInterview prevInterview = InventoryInterview.create(9, tlUser, "前年度メモ");
        ReflectionTestUtils.setField(prevInterview, "id", 90);
        when(interviewService.findPrevYear(10, tlUser)).thenReturn(Optional.of(prevInterview));
        when(interviewService.findDetailNotes(90)).thenReturn(List.of());

        mockMvc.perform(get("/api/interviews/inventory/10/prev-year"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generalNote").value("前年度メモ"));
    }

    @Test
    void getPrevYear_異常系_前年度なし_404を返す() throws Exception {
        when(interviewService.findPrevYear(10, tlUser)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/interviews/inventory/10/prev-year"))
                .andExpect(status().isNotFound());
    }
}

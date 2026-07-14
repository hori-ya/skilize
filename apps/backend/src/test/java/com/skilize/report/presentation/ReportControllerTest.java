package com.skilize.report.presentation;

import com.skilize.report.application.ReportService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ReportController の Web レイヤーテスト。DB 接続不要。standaloneSetup では
 * SecurityContextPersistenceFilter が存在しないため SecurityContextHolder へ直接設定する。
 */
@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock ReportService reportService;
    @InjectMocks ReportController controller;

    MockMvc mockMvc;
    private User generalUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        generalUser = User.create("user01", "テストユーザー", null, Role.GENERAL, null, "hash");
        ReflectionTestUtils.setField(generalUser, "id", 1);
        authenticateAs(generalUser);
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
    void downloadInventoryReport_正常系_200とPDFを返す() throws Exception {
        byte[] fakePdf = new byte[] {'%', 'P', 'D', 'F'};
        when(reportService.generateInventoryReport(10L, generalUser)).thenReturn(fakePdf);

        mockMvc.perform(get("/api/inventories/10/report"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"inventory_report_10.pdf\""));
    }
}

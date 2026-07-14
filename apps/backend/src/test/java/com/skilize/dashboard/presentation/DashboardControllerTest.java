package com.skilize.dashboard.presentation;

import com.skilize.dashboard.application.DashboardService;
import com.skilize.dashboard.application.query.DashboardQueryResult;
import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.inventory.domain.model.Inventory;
import com.skilize.inventory.domain.model.InventoryStatus;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DashboardController の Web レイヤーテスト。DB 接続不要。
 * standaloneSetup では SecurityContextPersistenceFilter が存在せず .with(user(..)) が
 * SecurityContextHolder に反映されないため、SecurityContextHolder へ直接認証情報を設定する。
 */
@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock DashboardService dashboardService;
    @InjectMocks DashboardController controller;

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
    void getDashboard_正常系_年度なし_年度と棚卸がnullで返る() throws Exception {
        when(dashboardService.getDashboard(anyInt())).thenReturn(new DashboardQueryResult(null, null, 0, 0, 0));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.currentFiscalYear").doesNotExist())
                .andExpect(jsonPath("$.currentInventory").doesNotExist());
    }

    @Test
    void getDashboard_正常系_年度ありだが棚卸未作成_棚卸がnullで返る() throws Exception {
        FiscalYear fy = FiscalYear.create("2025年度", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31),
                LocalDate.of(2025, 4, 1), LocalDate.of(2025, 5, 31));
        ReflectionTestUtils.setField(fy, "id", 2);
        when(dashboardService.getDashboard(anyInt())).thenReturn(new DashboardQueryResult(fy, null, 0, 0, 0));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentFiscalYear.name").value("2025年度"))
                .andExpect(jsonPath("$.currentFiscalYear.inputStartDate").value("2025-04-01"))
                .andExpect(jsonPath("$.currentInventory").doesNotExist());
    }

    @Test
    void getDashboard_正常系_棚卸あり_ステータスと明細件数を返す() throws Exception {
        FiscalYear fy = FiscalYear.create("2025年度", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), null, null);
        ReflectionTestUtils.setField(fy, "id", 2);
        Inventory inv = Inventory.create(generalUser, fy);
        ReflectionTestUtils.setField(inv, "id", 10);
        ReflectionTestUtils.setField(inv, "status", InventoryStatus.PENDING_GOAL);

        when(dashboardService.getDashboard(anyInt())).thenReturn(new DashboardQueryResult(fy, inv, 3, 1, 2));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentInventory.id").value(10))
                .andExpect(jsonPath("$.currentInventory.status").value("PENDING_GOAL"))
                .andExpect(jsonPath("$.currentInventory.itSkillCount").value(3))
                .andExpect(jsonPath("$.currentInventory.qualificationCount").value(1))
                .andExpect(jsonPath("$.currentInventory.seminarCount").value(2))
                .andExpect(jsonPath("$.currentInventory.submittedAt").doesNotExist());
    }
}

package com.skilize.charts.presentation;

import com.skilize.charts.application.ChartService;
import com.skilize.charts.application.query.GrowthQueryResult;
import com.skilize.charts.application.query.HeatmapQueryResult;
import com.skilize.charts.application.query.RadarQueryResult;
import com.skilize.charts.application.query.TimelineQueryResult;
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

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChartController の Web レイヤーテスト。DB 接続不要。standaloneSetup により
 * Spring Security フィルターチェーンなしでルーティング・レスポンス整形を検証する。
 * standaloneSetup では SecurityContextPersistenceFilter が存在せず .with(user(..)) が
 * SecurityContextHolder に反映されないため、SecurityContextHolder へ直接認証情報を設定する。
 */
@ExtendWith(MockitoExtension.class)
class ChartControllerTest {

    @Mock ChartService chartService;
    @InjectMocks ChartController controller;

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
    void getRadar_正常系_200とレーダーデータを返す() throws Exception {
        RadarQueryResult result = new RadarQueryResult("2025年度", "2024年度", true, 40, List.of());
        when(chartService.getRadar(generalUser)).thenReturn(result);

        mockMvc.perform(get("/api/charts/radar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentFiscalYear").value("2025年度"))
                .andExpect(jsonPath("$.hasCurrentYearData").value(true));
    }

    @Test
    void getGrowth_正常系_200と成長推移データを返す() throws Exception {
        GrowthQueryResult result = new GrowthQueryResult(List.of("2024年度", "2025年度"), List.of());
        when(chartService.getGrowth(generalUser)).thenReturn(result);

        mockMvc.perform(get("/api/charts/growth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiscalYears[0]").value("2024年度"));
    }

    @Test
    void getHeatmap_正常系_200とヒートマップデータを返す() throws Exception {
        HeatmapQueryResult result = new HeatmapQueryResult("2025年度", true, 5, List.of());
        when(chartService.getHeatmap(generalUser)).thenReturn(result);

        mockMvc.perform(get("/api/charts/heatmap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxLevelValue").value(5));
    }

    @Test
    void getTimeline_正常系_200とタイムラインデータを返す() throws Exception {
        TimelineQueryResult result = new TimelineQueryResult(List.of());
        when(chartService.getTimeline(generalUser)).thenReturn(result);

        mockMvc.perform(get("/api/charts/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isArray());
    }
}

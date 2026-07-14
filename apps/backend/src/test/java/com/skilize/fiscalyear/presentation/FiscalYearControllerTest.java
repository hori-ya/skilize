package com.skilize.fiscalyear.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skilize.fiscalyear.application.FiscalYearService;
import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.fiscalyear.domain.model.FiscalYearSettings;
import com.skilize.shared.presentation.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FiscalYearController の Web レイヤーテスト。DB 接続不要。standaloneSetup により
 * @PreAuthorize は評価されないため、正常系のレスポンス整形・バリデーション・404分岐を中心に検証する。
 */
@ExtendWith(MockitoExtension.class)
class FiscalYearControllerTest {

    @Mock FiscalYearService fiscalYearService;
    @InjectMocks FiscalYearController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    class List_ {

        @Test
        void 正常系_200と年度一覧を返す() throws Exception {
            FiscalYear fy = FiscalYear.create("2025年度", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), null, null);
            ReflectionTestUtils.setField(fy, "id", 1);
            when(fiscalYearService.findAllOrderByStartDateDesc()).thenReturn(List.of(fy));

            mockMvc.perform(get("/api/fiscal-years"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("2025年度"));
        }
    }

    @Nested
    class Current {

        @Test
        void 正常系_現在年度あり_200を返す() throws Exception {
            FiscalYear fy = FiscalYear.create("2025年度", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), null, null);
            ReflectionTestUtils.setField(fy, "id", 1);
            when(fiscalYearService.findCurrent(any(LocalDate.class))).thenReturn(Optional.of(fy));

            mockMvc.perform(get("/api/fiscal-years/current"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("2025年度"));
        }

        @Test
        void 異常系_現在年度なし_404を返す() throws Exception {
            when(fiscalYearService.findCurrent(any(LocalDate.class))).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/fiscal-years/current"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Create {

        @Test
        void 正常系_201で作成した年度を返す() throws Exception {
            FiscalYear saved = FiscalYear.create("2026年度", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31), null, null);
            ReflectionTestUtils.setField(saved, "id", 3);
            when(fiscalYearService.createFiscalYear(any(), any(), any(), any(), any())).thenReturn(saved);

            mockMvc.perform(post("/api/fiscal-years")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "name", "2026年度", "startDate", "2026-04-01", "endDate", "2027-03-31"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("2026年度"));
        }

        @Test
        void 異常系_name空_400バリデーションエラー() throws Exception {
            mockMvc.perform(post("/api/fiscal-years")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "name", "", "startDate", "2026-04-01", "endDate", "2027-03-31"))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Update {

        @Test
        void 正常系_200で更新後の年度を返す() throws Exception {
            FiscalYear updated = FiscalYear.create("2024年度改", LocalDate.of(2024, 4, 1), LocalDate.of(2025, 3, 31), null, null);
            ReflectionTestUtils.setField(updated, "id", 1);
            when(fiscalYearService.updateFiscalYear(any(Integer.class), any(), any(), any(), any(), any(), any()))
                    .thenReturn(updated);

            mockMvc.perform(put("/api/fiscal-years/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "name", "2024年度改", "startDate", "2024-04-01", "endDate", "2025-03-31"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("2024年度改"));
        }
    }

    @Nested
    class Settings {

        @Test
        void getSettings_正常系_200と年度開始月を返す() throws Exception {
            FiscalYearSettings settings = FiscalYearSettings.reconstruct((short) 1, (short) 4, null);
            when(fiscalYearService.getSettings()).thenReturn(settings);

            mockMvc.perform(get("/api/fiscal-year-settings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fiscalYearStartMonth").value(4));
        }

        @Test
        void updateSettings_正常系_200で更新後の月を返す() throws Exception {
            FiscalYearSettings settings = FiscalYearSettings.reconstruct((short) 1, (short) 7, null);
            when(fiscalYearService.updateSettings((short) 7)).thenReturn(settings);

            mockMvc.perform(put("/api/fiscal-year-settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("fiscalYearStartMonth", 7))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fiscalYearStartMonth").value(7));
        }

        @Test
        void updateSettings_異常系_範囲外の月_400バリデーションエラー() throws Exception {
            mockMvc.perform(put("/api/fiscal-year-settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("fiscalYearStartMonth", 13))))
                    .andExpect(status().isBadRequest());
        }
    }
}

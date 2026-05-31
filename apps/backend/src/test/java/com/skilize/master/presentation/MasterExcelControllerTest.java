package com.skilize.master.presentation;

import com.skilize.master.application.MasterExcelService;
import com.skilize.master.application.query.MasterImportErrorDetail;
import com.skilize.master.application.query.MasterImportQueryResult;
import com.skilize.master.infrastructure.excel.ExcelFormatException;
import com.skilize.shared.infrastructure.InitialPasswordFilter;
import com.skilize.shared.infrastructure.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MasterExcelController の Web レイヤーテスト。
 * DB 接続不要。JwtAuthenticationFilter と InitialPasswordFilter はモック化する。
 */
@WebMvcTest(MasterExcelController.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class MasterExcelControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean MasterExcelService masterExcelService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean InitialPasswordFilter initialPasswordFilter;

    @BeforeEach
    void setUpFilters() throws Exception {
        lenient().doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2))
                    .doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
        lenient().doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2))
                    .doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(initialPasswordFilter).doFilter(any(), any(), any());
    }

    @Nested
    class 出力 {

        @Test
        void ITスキルマスタをダウンロードできる() throws Exception {
            when(masterExcelService.exportItSkillExcel()).thenReturn(new byte[]{1, 2, 3});

            mockMvc.perform(get("/api/master-excel/it-skills/download")
                            .with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition",
                            org.hamcrest.Matchers.containsString("attachment")))
                    .andExpect(content().contentTypeCompatibleWith(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        @Test
        void ADMIN以外は403が返る() throws Exception {
            mockMvc.perform(get("/api/master-excel/it-skills/download")
                            .with(user("user").roles("GENERAL")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void 参考資格マスタをダウンロードできる() throws Exception {
            when(masterExcelService.exportQualificationExcel()).thenReturn(new byte[]{1, 2, 3});

            mockMvc.perform(get("/api/master-excel/qualifications/download")
                            .with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk());
        }

        @Test
        void ADマスタをダウンロードできる() throws Exception {
            when(masterExcelService.exportAdSeminarExcel()).thenReturn(new byte[]{1, 2, 3});

            mockMvc.perform(get("/api/master-excel/ad-seminars/download")
                            .with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class 取込 {

        @Test
        void ITスキルマスタを取込できる() throws Exception {
            when(masterExcelService.importItSkillExcel(any()))
                    .thenReturn(MasterImportQueryResult.ofSuccess(3, 5, 1));

            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                    MediaType.MULTIPART_FORM_DATA_VALUE, new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/master-excel/it-skills/upload")
                            .file(file)
                            .with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.created").value(3))
                    .andExpect(jsonPath("$.updated").value(5))
                    .andExpect(jsonPath("$.deleted").value(1));
        }

        @Test
        void バリデーションエラーがある場合は400が返る() throws Exception {
            List<MasterImportErrorDetail> errors = List.of(
                    new MasterImportErrorDetail("ITスキル", 3, "F", "スキル名は必須です"));
            when(masterExcelService.importItSkillExcel(any()))
                    .thenReturn(MasterImportQueryResult.ofErrors(errors));

            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                    MediaType.MULTIPART_FORM_DATA_VALUE, new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/master-excel/it-skills/upload")
                            .file(file)
                            .with(user("admin").roles("ADMIN")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("EXCEL_IMPORT_ERROR"))
                    .andExpect(jsonPath("$.errors[0].sheet").value("ITスキル"))
                    .andExpect(jsonPath("$.errors[0].row").value(3))
                    .andExpect(jsonPath("$.errors[0].column").value("F"))
                    .andExpect(jsonPath("$.errors[0].message").value("スキル名は必須です"));
        }

        @Test
        void ファイル形式が不正な場合は400が返る() throws Exception {
            when(masterExcelService.importItSkillExcel(any()))
                    .thenThrow(new ExcelFormatException("シート「IT分類」が見つかりません"));

            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                    MediaType.MULTIPART_FORM_DATA_VALUE, new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/master-excel/it-skills/upload")
                            .file(file)
                            .with(user("admin").roles("ADMIN")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("EXCEL_FORMAT_ERROR"));
        }

        @Test
        void 参考資格マスタを取込できる() throws Exception {
            when(masterExcelService.importQualificationExcel(any()))
                    .thenReturn(MasterImportQueryResult.ofSuccess(2, 10, 0));

            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                    MediaType.MULTIPART_FORM_DATA_VALUE, new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/master-excel/qualifications/upload")
                            .file(file)
                            .with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.created").value(2));
        }

        @Test
        void ADマスタを取込できる() throws Exception {
            when(masterExcelService.importAdSeminarExcel(any()))
                    .thenReturn(MasterImportQueryResult.ofSuccess(0, 5, 2));

            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                    MediaType.MULTIPART_FORM_DATA_VALUE, new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/master-excel/ad-seminars/upload")
                            .file(file)
                            .with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deleted").value(2));
        }
    }
}

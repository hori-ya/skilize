package com.skilize.master.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skilize.master.application.MasterService;
import com.skilize.master.domain.model.*;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MasterController の Web レイヤーテスト。DB 接続不要。standaloneSetup では @PreAuthorize は
 * 評価されないため、レスポンス整形（特に ITスキルの表示用大分類/中分類解決）とバリデーションを検証する。
 */
@ExtendWith(MockitoExtension.class)
class MasterControllerTest {

    @Mock MasterService masterService;
    @InjectMocks MasterController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    class SkillLevels {

        @Test
        void getSkillLevels_正常系_200と一覧を返す() throws Exception {
            SkillLevel level = SkillLevel.create((short) 3, "上級", 30);
            ReflectionTestUtils.setField(level, "id", 1);
            when(masterService.getSkillLevels(null)).thenReturn(List.of(level));

            mockMvc.perform(get("/api/skill-levels"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].description").value("上級"));
        }

        @Test
        void createSkillLevel_異常系_levelValue未指定_400バリデーションエラー() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("description", "上級");
            body.put("scoreWeight", 30);

            mockMvc.perform(post("/api/skill-levels")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void createSkillLevel_正常系_201で作成結果を返す() throws Exception {
            SkillLevel saved = SkillLevel.create((short) 3, "上級", 30);
            ReflectionTestUtils.setField(saved, "id", 1);
            when(masterService.createSkillLevel(eq((short) 3), eq("上級"), eq(30))).thenReturn(saved);

            Map<String, Object> body = new HashMap<>();
            body.put("levelValue", 3);
            body.put("description", "上級");
            body.put("scoreWeight", 30);

            mockMvc.perform(post("/api/skill-levels")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1));
        }
    }

    @Nested
    class ItSkills {

        @Test
        void getItSkills_正常系_レベル3スキル_大分類と中分類を解決する() throws Exception {
            ItSkillCategory cat1 = ItSkillCategory.create(null, (short) 1, "大分類", 1);
            ReflectionTestUtils.setField(cat1, "id", 1);
            ItSkillCategory cat2 = ItSkillCategory.create(1, (short) 2, "中分類", 1);
            ReflectionTestUtils.setField(cat2, "id", 2);
            ItSkillCategory cat3 = ItSkillCategory.create(2, (short) 3, "小分類", 1);
            ReflectionTestUtils.setField(cat3, "id", 3);
            ItSkill skill = ItSkill.create(cat3, "Java", null, 1);
            ReflectionTestUtils.setField(skill, "id", 100);

            when(masterService.getItSkills(null)).thenReturn(List.of(skill));
            when(masterService.findItSkillCategoryById(2)).thenReturn(Optional.of(cat2));
            when(masterService.findItSkillCategoryById(1)).thenReturn(Optional.of(cat1));

            mockMvc.perform(get("/api/it-skills"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].category1Name").value("大分類"))
                    .andExpect(jsonPath("$[0].category2Name").value("中分類"))
                    .andExpect(jsonPath("$[0].category3Name").value("小分類"));
        }

        @Test
        void getItSkills_正常系_レベル1スキル_大分類はそのまま返す() throws Exception {
            ItSkillCategory cat1 = ItSkillCategory.create(null, (short) 1, "大分類", 1);
            ReflectionTestUtils.setField(cat1, "id", 1);
            ItSkill skill = ItSkill.create(cat1, "マネジメント", null, 1);
            ReflectionTestUtils.setField(skill, "id", 101);

            when(masterService.getItSkills(true)).thenReturn(List.of(skill));

            mockMvc.perform(get("/api/it-skills").param("isActive", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].category1Name").value("大分類"))
                    .andExpect(jsonPath("$[0].category2Name").doesNotExist());
        }

        @Test
        void createItSkill_異常系_categoryId未指定_400バリデーションエラー() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("name", "Java");

            mockMvc.perform(post("/api/it-skills")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void getCustomUnregisteredItSkills_正常系_使用件数付きで返す() throws Exception {
            when(masterService.getCustomUnregisteredItSkills())
                    .thenReturn(List.<Object[]>of(new Object[] {"自作スキル", 3L}));

            mockMvc.perform(get("/api/it-skills/custom-unregistered"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].customName").value("自作スキル"))
                    .andExpect(jsonPath("$[0].usageCount").value(3));
        }

        @Test
        void promoteItSkill_正常系_201で昇格結果を返す() throws Exception {
            ItSkillCategory cat1 = ItSkillCategory.create(null, (short) 1, "大分類", 1);
            ReflectionTestUtils.setField(cat1, "id", 1);
            ItSkill saved = ItSkill.create(cat1, "自作スキル", null, 0);
            ReflectionTestUtils.setField(saved, "id", 200);
            when(masterService.promoteItSkill(eq("自作スキル"), eq(1), eq("自作スキル"), any(), any()))
                    .thenReturn(saved);

            Map<String, Object> body = new HashMap<>();
            body.put("customName", "自作スキル");
            body.put("categoryId", 1);
            body.put("name", "自作スキル");

            mockMvc.perform(post("/api/it-skills/promote")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(200));
        }
    }

    @Nested
    class Qualifications {

        @Test
        void getQualifications_正常系_200と一覧を返す() throws Exception {
            QualificationCategory cat = QualificationCategory.create("分類A", 1);
            ReflectionTestUtils.setField(cat, "id", 1);
            Qualification q = Qualification.create(cat, "基本情報技術者", null, 1);
            ReflectionTestUtils.setField(q, "id", 10);
            when(masterService.getQualifications(null)).thenReturn(List.of(q));

            mockMvc.perform(get("/api/qualifications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].categoryName").value("分類A"));
        }

        @Test
        void updateQualification_正常系_200で更新結果を返す() throws Exception {
            Qualification updated = Qualification.create(null, "応用情報技術者", null, 1);
            ReflectionTestUtils.setField(updated, "id", 10);
            when(masterService.updateQualification(eq(10), any(), eq("応用情報技術者"), any(), any(), any()))
                    .thenReturn(updated);

            Map<String, Object> body = new HashMap<>();
            body.put("name", "応用情報技術者");

            mockMvc.perform(put("/api/qualifications/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("応用情報技術者"));
        }

        @Test
        void getCustomUnregisteredQualifications_正常系_使用件数付きで返す() throws Exception {
            when(masterService.getCustomUnregisteredQualifications())
                    .thenReturn(List.<Object[]>of(new Object[] {"自作資格", 2L}));

            mockMvc.perform(get("/api/qualifications/custom-unregistered"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].usageCount").value(2));
        }
    }

    @Nested
    class QualificationCategories {

        @Test
        void createQualificationCategory_正常系_201で作成結果を返す() throws Exception {
            QualificationCategory saved = QualificationCategory.create("分類B", 0);
            ReflectionTestUtils.setField(saved, "id", 2);
            when(masterService.createQualificationCategory(eq("分類B"), any())).thenReturn(saved);

            Map<String, Object> body = new HashMap<>();
            body.put("name", "分類B");

            mockMvc.perform(post("/api/qualification-categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("分類B"));
        }
    }

    @Nested
    class AdSeminars {

        @Test
        void getAdSeminars_正常系_200と一覧を返す() throws Exception {
            AdSeminar a = AdSeminar.create(null, "AWS研修", null, 1);
            ReflectionTestUtils.setField(a, "id", 5);
            when(masterService.getAdSeminars(null)).thenReturn(List.of(a));

            mockMvc.perform(get("/api/ad-seminars"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("AWS研修"));
        }

        @Test
        void createAdSeminar_正常系_201で作成結果を返す() throws Exception {
            AdSeminar saved = AdSeminar.create(null, "GCP研修", null, 0);
            ReflectionTestUtils.setField(saved, "id", 6);
            when(masterService.createAdSeminar(any(), eq("GCP研修"), any(), any())).thenReturn(saved);

            Map<String, Object> body = new HashMap<>();
            body.put("name", "GCP研修");

            mockMvc.perform(post("/api/ad-seminars")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("GCP研修"));
        }
    }

    @Nested
    class SeminarCategories {

        @Test
        void getSeminarCategories_正常系_200と一覧を返す() throws Exception {
            SeminarCategory cat = SeminarCategory.reconstruct(1, "技術研修", 1, true, null, null);
            when(masterService.getSeminarCategories()).thenReturn(List.of(cat));

            mockMvc.perform(get("/api/seminar-categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("技術研修"));
        }
    }

    @Nested
    class ItSkillCategories {

        @Test
        void getItSkillCategories_正常系_200と一覧を返す() throws Exception {
            ItSkillCategory cat = ItSkillCategory.create(null, (short) 1, "大分類", 1);
            ReflectionTestUtils.setField(cat, "id", 1);
            when(masterService.getItSkillCategories(null)).thenReturn(List.of(cat));

            mockMvc.perform(get("/api/it-skill-categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].level").value(1));
        }

        @Test
        void createItSkillCategory_異常系_name空_400バリデーションエラー() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("name", "");

            mockMvc.perform(post("/api/it-skill-categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void updateItSkillCategory_正常系_200で更新結果を返す() throws Exception {
            ItSkillCategory updated = ItSkillCategory.create(null, (short) 1, "大分類改", 0);
            ReflectionTestUtils.setField(updated, "id", 1);
            when(masterService.updateItSkillCategory(eq(1), eq("大分類改"), any(), any())).thenReturn(updated);

            Map<String, Object> body = new HashMap<>();
            body.put("name", "大分類改");

            mockMvc.perform(put("/api/it-skill-categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("大分類改"));
        }
    }

    @Nested
    class AdSeminarCategories {

        @Test
        void getAdSeminarCategories_正常系_200と一覧を返す() throws Exception {
            AdSeminarCategory cat = AdSeminarCategory.create("研修", 1);
            ReflectionTestUtils.setField(cat, "id", 1);
            when(masterService.getAdSeminarCategories(null)).thenReturn(List.of(cat));

            mockMvc.perform(get("/api/ad-seminar-categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("研修"));
        }
    }
}

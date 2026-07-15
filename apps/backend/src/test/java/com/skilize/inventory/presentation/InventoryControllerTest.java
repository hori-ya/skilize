package com.skilize.inventory.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.inventory.application.InventoryService;
import com.skilize.inventory.application.mapper.InventoryApplicationMapper;
import com.skilize.inventory.application.query.ComparisonQueryResult;
import com.skilize.inventory.application.query.GoalReviewQueryResult;
import com.skilize.inventory.domain.model.*;
import com.skilize.master.domain.model.ItSkill;
import com.skilize.master.domain.model.SkillLevel;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.shared.domain.exception.GoalIncompleteException;
import com.skilize.shared.presentation.GlobalExceptionHandler;
import com.skilize.user.domain.model.Role;
import com.skilize.user.domain.model.User;
import com.skilize.user.infrastructure.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * InventoryController の Web レイヤーテスト。DB 接続不要。
 * standaloneSetup では SecurityContextPersistenceFilter が存在しないため SecurityContextHolder へ直接設定する。
 * Mapper はモック化し、Command 変換自体は InventoryApplicationMapperTest 相当の責務外として扱う。
 */
@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock InventoryService inventoryService;
    @Mock InventoryApplicationMapper mapper;
    @InjectMocks InventoryController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();
    private User generalUser;
    private FiscalYear fy;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        generalUser = User.create("user01", "テストユーザー", null, Role.GENERAL, null, "hash");
        ReflectionTestUtils.setField(generalUser, "id", 1);
        authenticateAs(generalUser);

        fy = FiscalYear.create("2025年度", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), null, null);
        ReflectionTestUtils.setField(fy, "id", 2);
        inventory = Inventory.create(generalUser, fy);
        ReflectionTestUtils.setField(inventory, "id", 10);
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
    void mine_正常系_200と棚卸一覧を返す() throws Exception {
        when(inventoryService.findMine(1)).thenReturn(List.of(inventory));

        mockMvc.perform(get("/api/inventories/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Nested
    class Create {

        @Test
        void 正常系_201で作成した棚卸を返す() throws Exception {
            when(inventoryService.create(any(User.class), eq(2))).thenReturn(inventory);

            mockMvc.perform(post("/api/inventories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("fiscalYearId", 2))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10));
        }

        @Test
        void 異常系_同年度既存棚卸あり_409を返す() throws Exception {
            when(inventoryService.create(any(User.class), eq(2)))
                    .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "INVENTORY_ALREADY_EXISTS"));

            mockMvc.perform(post("/api/inventories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("fiscalYearId", 2))))
                    .andExpect(status().isConflict());
        }
    }

    @Test
    void getById_正常系_200と棚卸詳細を返す() throws Exception {
        when(inventoryService.findById(10, generalUser)).thenReturn(inventory);

        mockMvc.perform(get("/api/inventories/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void getById_異常系_他人の棚卸_403を返す() throws Exception {
        when(inventoryService.findById(10, generalUser)).thenThrow(new AuthException("FORBIDDEN", ""));

        mockMvc.perform(get("/api/inventories/10"))
                .andExpect(status().isForbidden());
    }

    @Nested
    class ItSkillDetails {

        @Test
        void get_正常系_200と明細一覧を返す() throws Exception {
            when(inventoryService.findItSkillDetails(10, generalUser)).thenReturn(List.of());

            mockMvc.perform(get("/api/inventories/10/it-skill-details"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }

        @Test
        void save_正常系_200で保存結果を返す() throws Exception {
            when(mapper.toCommands(any())).thenReturn(List.of());
            when(inventoryService.saveItSkillDetails(eq(10), any(User.class), any())).thenReturn(List.of());

            mockMvc.perform(put("/api/inventories/10/it-skill-details")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("items", List.of()))))
                    .andExpect(status().isOk());
        }

        @Test
        void patchRemarks_正常系_200で更新結果を返す() throws Exception {
            ItSkill skill = ItSkill.create(null, "Java", null, 1);
            SkillLevel level = SkillLevel.create((short) 3, "上級", 30);
            ItSkillDetail detail = ItSkillDetail.create(inventory, skill, null, level, "新備考");
            ReflectionTestUtils.setField(detail, "id", 50);
            when(inventoryService.updateItSkillDetailRemarks(10, 50, generalUser, "新備考")).thenReturn(detail);

            mockMvc.perform(patch("/api/inventories/10/it-skill-details/50")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("remarks", "新備考"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.remarks").value("新備考"));
        }
    }

    @Nested
    class QualificationDetails {

        @Test
        void get_正常系_200と明細一覧を返す() throws Exception {
            when(inventoryService.findQualificationDetails(10, generalUser)).thenReturn(List.of());

            mockMvc.perform(get("/api/inventories/10/qualification-details"))
                    .andExpect(status().isOk());
        }

        @Test
        void save_正常系_200で保存結果を返す() throws Exception {
            when(mapper.toQualificationCommands(any())).thenReturn(List.of());
            when(inventoryService.saveQualificationDetails(eq(10), any(User.class), any())).thenReturn(List.of());

            mockMvc.perform(put("/api/inventories/10/qualification-details")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("items", List.of()))))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class SeminarDetails {

        @Test
        void get_正常系_200と明細一覧を返す() throws Exception {
            when(inventoryService.findSeminarDetails(10, generalUser)).thenReturn(List.of());

            mockMvc.perform(get("/api/inventories/10/seminar-details"))
                    .andExpect(status().isOk());
        }

        @Test
        void save_正常系_200で保存結果を返す() throws Exception {
            when(mapper.toSeminarCommands(any())).thenReturn(List.of());
            when(inventoryService.saveSeminarDetails(eq(10), any(User.class), any())).thenReturn(List.of());

            mockMvc.perform(put("/api/inventories/10/seminar-details")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("items", List.of()))))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void submit_正常系_200で提出後のステータスを返す() throws Exception {
        ReflectionTestUtils.setField(inventory, "status", InventoryStatus.PENDING_GOAL);
        when(inventoryService.submit(10, generalUser)).thenReturn(inventory);

        mockMvc.perform(post("/api/inventories/10/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_GOAL"));
    }

    @Test
    void getComparison_正常系_200と比較結果を返す() throws Exception {
        ComparisonQueryResult result = new ComparisonQueryResult(10, "2025年度", null, false, List.of());
        when(inventoryService.getComparison(10, generalUser)).thenReturn(result);

        mockMvc.perform(get("/api/inventories/10/comparison"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPrevYear").value(false));
    }

    @Nested
    class GoalReview {

        @Test
        void get_正常系_200と振り返りデータを返す() throws Exception {
            GoalReviewQueryResult result = new GoalReviewQueryResult(null, false, List.of());
            when(inventoryService.getGoalReview(10, generalUser)).thenReturn(result);

            mockMvc.perform(get("/api/inventories/10/goal-review"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasPrevGoals").value(false));
        }

        @Test
        void save_正常系_200で更新結果を返す() throws Exception {
            when(mapper.toGoalReviewUpdateCommands(any())).thenReturn(List.of());
            GoalReviewQueryResult result = new GoalReviewQueryResult("2024年度", true, List.of());
            when(inventoryService.saveGoalReview(eq(10), any(User.class), any())).thenReturn(result);

            mockMvc.perform(put("/api/inventories/10/goal-review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("items", List.of()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasPrevGoals").value(true));
        }

        @Test
        void complete_正常系_200で完了日時を返す() throws Exception {
            ReflectionTestUtils.setField(inventory, "goalReviewCompletedAt", java.time.OffsetDateTime.now());
            when(inventoryService.completeGoalReview(10, generalUser)).thenReturn(inventory);

            mockMvc.perform(post("/api/inventories/10/goal-review/complete"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.goalReviewCompletedAt").exists());
        }
    }

    @Nested
    class Goals {

        @Test
        void get_正常系_200と目標一覧を返す() throws Exception {
            when(inventoryService.findGoals(10, generalUser)).thenReturn(List.of());

            mockMvc.perform(get("/api/inventories/10/goals"))
                    .andExpect(status().isOk());
        }

        @Test
        void save_正常系_200で保存結果を返す() throws Exception {
            when(mapper.toGoalCommands(any())).thenReturn(List.of());
            when(inventoryService.saveGoals(eq(10), any(User.class), any())).thenReturn(List.of());

            mockMvc.perform(put("/api/inventories/10/goals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("items", List.of()))))
                    .andExpect(status().isOk());
        }

        @Test
        void complete_正常系_200で完了ステータスを返す() throws Exception {
            ReflectionTestUtils.setField(inventory, "status", InventoryStatus.COMPLETED);
            when(inventoryService.completeGoal(10, generalUser)).thenReturn(inventory);

            mockMvc.perform(post("/api/inventories/10/goals/complete"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        void complete_異常系_件数不足_422を返す() throws Exception {
            List<GoalIncompleteException.GoalValidationError> errors = List.of(
                    new GoalIncompleteException.GoalValidationError("ad", "ADの目標を2件すべて入力してください"));
            when(inventoryService.completeGoal(10, generalUser)).thenThrow(new GoalIncompleteException(errors));

            mockMvc.perform(post("/api/inventories/10/goals/complete"))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("GOAL_INCOMPLETE"));
        }
    }
}

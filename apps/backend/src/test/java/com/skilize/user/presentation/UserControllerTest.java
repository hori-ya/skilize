package com.skilize.user.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.inventory.domain.model.Inventory;
import com.skilize.inventory.domain.model.InventoryStatus;
import com.skilize.shared.presentation.GlobalExceptionHandler;
import com.skilize.user.application.UserService;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController の Web レイヤーテスト。DB 接続不要。standaloneSetup では @PreAuthorize が
 * 評価されないため、正常系のレスポンス整形・バリデーション・アクセス制御分岐を中心に検証する。
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock UserService userService;
    @InjectMocks UserController controller;

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

    @Nested
    class List_ {

        @Test
        void 正常系_200とユーザー一覧を返す_TL名を解決する() throws Exception {
            User member = User.create("user01", "一般ユーザー", null, Role.GENERAL, 2, "hash");
            ReflectionTestUtils.setField(member, "id", 5);
            when(userService.findAllOrdered()).thenReturn(List.of(tlUser, member));

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[1].tlName").value("TL"));
        }
    }

    @Nested
    class Create {

        @Test
        void 正常系_201で作成したユーザーを返す() throws Exception {
            User saved = User.create("user03", "新規ユーザー", null, Role.GENERAL, null, "hash");
            ReflectionTestUtils.setField(saved, "id", 10);
            when(userService.create("user03", "新規ユーザー", null, Role.GENERAL, null)).thenReturn(saved);
            when(userService.findAllOrdered()).thenReturn(List.of(saved));

            Map<String, Object> body = new HashMap<>();
            body.put("userId", "user03");
            body.put("name", "新規ユーザー");
            body.put("role", "GENERAL");

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").value("user03"));
        }

        @Test
        void 異常系_不正なロール文字列_400を返す() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", "user03");
            body.put("name", "新規ユーザー");
            body.put("role", "SUPER_ADMIN");

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_ROLE"));
        }

        @Test
        void 異常系_userIdが空_400バリデーションエラー() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", "");
            body.put("name", "新規ユーザー");
            body.put("role", "GENERAL");

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    class Update {

        @Test
        void 正常系_200で更新後のユーザーを返す() throws Exception {
            User updated = User.create("user01", "更新後", null, Role.GENERAL, null, "hash");
            ReflectionTestUtils.setField(updated, "id", 5);
            when(userService.update(5, "更新後", null, Role.GENERAL, null, true)).thenReturn(updated);
            when(userService.findAllOrdered()).thenReturn(List.of(updated));

            Map<String, Object> body = new HashMap<>();
            body.put("name", "更新後");
            body.put("role", "GENERAL");

            mockMvc.perform(put("/api/users/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("更新後"));
        }

        @Test
        void 異常系_不正なロール文字列パターン_400バリデーションエラー() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("name", "更新後");
            body.put("role", "SUPER_ADMIN");

            mockMvc.perform(put("/api/users/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void resetPassword_正常系_200で仮パスワードを返す() throws Exception {
        when(userService.resetPassword(5)).thenReturn("user01");

        mockMvc.perform(post("/api/users/5/reset-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").value("user01"));
    }

    @Nested
    class GetTeamMembers {

        @Test
        void 正常系_今年度棚卸ありのメンバーを返す() throws Exception {
            User member = User.create("user01", "担当ユーザー", null, Role.GENERAL, 2, "hash");
            ReflectionTestUtils.setField(member, "id", 5);
            FiscalYear fy = FiscalYear.create("2025年度", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), null, null);
            ReflectionTestUtils.setField(fy, "id", 3);
            Inventory inv = Inventory.create(member, fy);
            ReflectionTestUtils.setField(inv, "id", 20);
            ReflectionTestUtils.setField(inv, "status", InventoryStatus.PENDING_GOAL);

            when(userService.findActiveMembersFor(tlUser)).thenReturn(List.of(member));
            when(userService.findCurrentFiscalYear()).thenReturn(Optional.of(fy));
            when(userService.findCurrentInventory(5, 3)).thenReturn(Optional.of(inv));
            when(userService.findAllOrdered()).thenReturn(List.of(tlUser, member));

            mockMvc.perform(get("/api/users/me/team-members"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].currentInventory.status").value("PENDING_GOAL"));
        }

        @Test
        void 正常系_今年度なし_棚卸情報がnullで返る() throws Exception {
            User member = User.create("user01", "担当ユーザー", null, Role.GENERAL, 2, "hash");
            ReflectionTestUtils.setField(member, "id", 5);

            when(userService.findActiveMembersFor(tlUser)).thenReturn(List.of(member));
            when(userService.findCurrentFiscalYear()).thenReturn(Optional.empty());
            when(userService.findAllOrdered()).thenReturn(List.of(tlUser, member));

            mockMvc.perform(get("/api/users/me/team-members"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].currentInventory").doesNotExist());
        }
    }

    @Nested
    class GetUserInventories {

        @Test
        void 正常系_TL_担当ユーザーの棚卸一覧を返す() throws Exception {
            User target = User.create("user01", "担当ユーザー", null, Role.GENERAL, 2, "hash");
            ReflectionTestUtils.setField(target, "id", 5);
            FiscalYear fy = FiscalYear.create("2025年度", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), null, null);
            ReflectionTestUtils.setField(fy, "id", 3);
            Inventory inv = Inventory.create(target, fy);
            ReflectionTestUtils.setField(inv, "id", 20);

            when(userService.findById(5)).thenReturn(Optional.of(target));
            when(userService.findInventoriesByUserId(5)).thenReturn(List.of(inv));

            mockMvc.perform(get("/api/users/5/inventories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(20));
        }

        @Test
        void 異常系_担当外TL_403を返す() throws Exception {
            User otherTeamMember = User.create("user02", "他チームユーザー", null, Role.GENERAL, 99, "hash");
            ReflectionTestUtils.setField(otherTeamMember, "id", 6);
            when(userService.findById(6)).thenReturn(Optional.of(otherTeamMember));

            mockMvc.perform(get("/api/users/6/inventories"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void 異常系_対象ユーザー不在_404を返す() throws Exception {
            when(userService.findById(99)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/users/99/inventories"))
                    .andExpect(status().isNotFound());
        }
    }
}

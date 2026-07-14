package com.skilize.user.application;

import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.fiscalyear.domain.repository.FiscalYearRepository;
import com.skilize.inventory.domain.model.Inventory;
import com.skilize.inventory.domain.repository.InventoryRepository;
import com.skilize.user.domain.model.Role;
import com.skilize.user.domain.model.User;
import com.skilize.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * UserService の単体テスト。ユーザーID重複チェック・パスワードリセット・ロール別チーム照会を検証する。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock FiscalYearRepository fiscalYearRepository;
    @Mock InventoryRepository inventoryRepository;

    @InjectMocks UserService userService;

    private User adminUser;
    private User tlUser;

    @BeforeEach
    void setUp() {
        adminUser = User.create("admin", "管理者", null, Role.ADMIN, null, "hash");
        ReflectionTestUtils.setField(adminUser, "id", 1);
        tlUser = User.create("tl01", "TL", null, Role.TL, null, "hash");
        ReflectionTestUtils.setField(tlUser, "id", 2);
    }

    @Nested
    class Create {

        @Test
        void 正常系_新規ユーザーを作成する() {
            when(userRepository.findByUserId("user03")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("user03")).thenReturn("$2a$12$hashed");
            when(userRepository.save(any(User.class))).thenAnswer(new org.mockito.stubbing.Answer<User>() {
                @Override
                public User answer(org.mockito.invocation.InvocationOnMock invocation) {
                    return invocation.getArgument(0);
                }
            });

            User result = userService.create("user03", "新規ユーザー", null, Role.GENERAL, 2);

            assertThat(result.getUserId()).isEqualTo("user03");
            assertThat(result.getPasswordHash()).isEqualTo("$2a$12$hashed");
        }

        @Test
        void 異常系_ユーザーID重複_409をスロー() {
            when(userRepository.findByUserId("user01")).thenReturn(Optional.of(tlUser));

            try {
                userService.create("user01", "重複ユーザー", null, Role.GENERAL, null);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(409);
                assertThat(e.getBody().getDetail()).isEqualTo("USER_ID_ALREADY_EXISTS");
            }
        }
    }

    @Nested
    class Update {

        @Test
        void 正常系_ユーザー情報を更新する() {
            when(userRepository.findById(2)).thenReturn(Optional.of(tlUser));
            when(userRepository.save(tlUser)).thenReturn(tlUser);

            User result = userService.update(2, "TL改", "tl@example.com", Role.TL, null, false);

            assertThat(result.getName()).isEqualTo("TL改");
            assertThat(result.isActive()).isFalse();
        }

        @Test
        void 異常系_対象ユーザー不在_404をスロー() {
            when(userRepository.findById(99)).thenReturn(Optional.empty());

            try {
                userService.update(99, "名前", null, Role.GENERAL, null, true);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }

    @Nested
    class ResetPassword {

        @Test
        void 正常系_ユーザーID同一の仮パスワードを返す() {
            when(userRepository.findById(2)).thenReturn(Optional.of(tlUser));
            when(passwordEncoder.encode("tl01")).thenReturn("$2a$12$reset");

            String tempPassword = userService.resetPassword(2);

            assertThat(tempPassword).isEqualTo("tl01");
            assertThat(tlUser.isInitialPassword()).isTrue();
        }

        @Test
        void 異常系_対象ユーザー不在_404をスロー() {
            when(userRepository.findById(99)).thenReturn(Optional.empty());

            try {
                userService.resetPassword(99);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }

    @Nested
    class FindActiveMembersFor {

        @Test
        void 正常系_ADMIN_全有効ユーザーを返す() {
            when(userRepository.findByActiveTrue()).thenReturn(List.of(tlUser));

            List<User> result = userService.findActiveMembersFor(adminUser);

            assertThat(result).containsExactly(tlUser);
        }

        @Test
        void 正常系_TL_自分の担当ユーザーのみ返す() {
            User member = User.create("user01", "担当ユーザー", null, Role.GENERAL, 2, "hash");
            when(userRepository.findByTlUserIdAndActiveTrue(2)).thenReturn(List.of(member));

            List<User> result = userService.findActiveMembersFor(tlUser);

            assertThat(result).containsExactly(member);
        }
    }

    @Nested
    class FindCurrentInventory {

        @Test
        void 正常系_指定年度の棚卸を返す() {
            FiscalYear fy = FiscalYear.create("2025年度", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), null, null);
            ReflectionTestUtils.setField(fy, "id", 2);
            User member = User.create("user01", "ユーザー", null, Role.GENERAL, null, "hash");
            Inventory inv = Inventory.create(member, fy);
            when(inventoryRepository.findByUserIdAndFiscalYearId(5, 2)).thenReturn(Optional.of(inv));

            Optional<Inventory> result = userService.findCurrentInventory(5, 2);

            assertThat(result).contains(inv);
        }
    }
}

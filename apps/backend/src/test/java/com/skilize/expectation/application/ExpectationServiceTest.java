package com.skilize.expectation.application;

import com.skilize.expectation.application.query.ExpectationQueryResult;
import com.skilize.expectation.domain.model.UserExpectation;
import com.skilize.expectation.domain.repository.UserExpectationRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

/**
 * ExpectationService の単体テスト。TL/ADMIN/GENERAL のアクセス制御分岐と upsert 挙動を検証する。
 */
@ExtendWith(MockitoExtension.class)
class ExpectationServiceTest {

    @Mock UserExpectationRepository expectationRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ExpectationService expectationService;

    private User adminUser;
    private User tlUser;
    private User otherTlUser;
    private User generalUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        adminUser = User.create("admin", "管理者", null, Role.ADMIN, null, "hash");
        ReflectionTestUtils.setField(adminUser, "id", 1);

        tlUser = User.create("tl01", "TL", null, Role.TL, null, "hash");
        ReflectionTestUtils.setField(tlUser, "id", 2);

        otherTlUser = User.create("tl02", "他TL", null, Role.TL, null, "hash");
        ReflectionTestUtils.setField(otherTlUser, "id", 3);

        generalUser = User.create("user01", "一般ユーザー", null, Role.GENERAL, null, "hash");
        ReflectionTestUtils.setField(generalUser, "id", 4);

        targetUser = User.create("user02", "対象ユーザー", null, Role.GENERAL, 2, "hash");
        ReflectionTestUtils.setField(targetUser, "id", 5);
    }

    @Nested
    class GetForUser {

        @Test
        void 正常系_ADMIN_全ユーザーの期待情報を取得できる() {
            UserExpectation entity = UserExpectation.create(targetUser);
            entity.updateTlExpectation("頑張ってください");
            when(expectationRepository.findByUserId(5)).thenReturn(Optional.of(entity));

            ExpectationQueryResult result = expectationService.getForUser(5, adminUser);

            assertThat(result.tlExpectation()).isEqualTo("頑張ってください");
        }

        @Test
        void 正常系_担当TL_チームメンバーの期待情報を取得できる() {
            when(userRepository.findById(5)).thenReturn(Optional.of(targetUser));
            when(expectationRepository.findByUserId(5)).thenReturn(Optional.empty());

            ExpectationQueryResult result = expectationService.getForUser(5, tlUser);

            assertThat(result.tlExpectation()).isNull();
            assertThat(result.companyExpectation()).isNull();
        }

        @Test
        void 異常系_担当外TL_403をスロー() {
            when(userRepository.findById(5)).thenReturn(Optional.of(targetUser));

            try {
                expectationService.getForUser(5, otherTlUser);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(403);
                assertThat(e.getReason()).isEqualTo("EXPECTATION_TEAM_MEMBER_ONLY");
            }
        }

        @Test
        void 異常系_GENERAL_403をスロー() {
            try {
                expectationService.getForUser(5, generalUser);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(403);
                assertThat(e.getReason()).isEqualTo("FORBIDDEN");
            }
        }

        @Test
        void 異常系_対象ユーザー不在_404をスロー() {
            when(userRepository.findById(99)).thenReturn(Optional.empty());

            try {
                expectationService.getForUser(99, tlUser);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }

    @Nested
    class SaveTlExpectation {

        @Test
        void 正常系_担当TL_新規レコードを作成して保存する() {
            when(userRepository.findById(5)).thenReturn(Optional.of(targetUser));
            when(expectationRepository.findByUserId(5)).thenReturn(Optional.empty());
            when(expectationRepository.save(org.mockito.ArgumentMatchers.any(UserExpectation.class)))
                    .thenAnswer(new org.mockito.stubbing.Answer<UserExpectation>() {
                        @Override
                        public UserExpectation answer(org.mockito.invocation.InvocationOnMock invocation) {
                            return invocation.getArgument(0);
                        }
                    });

            ExpectationQueryResult result = expectationService.saveTlExpectation(5, tlUser, "期待コメント");

            assertThat(result.tlExpectation()).isEqualTo("期待コメント");
        }

        @Test
        void 正常系_既存レコードあり_更新して保存する() {
            UserExpectation existing = UserExpectation.create(targetUser);
            when(userRepository.findById(5)).thenReturn(Optional.of(targetUser));
            when(expectationRepository.findByUserId(5)).thenReturn(Optional.of(existing));
            when(expectationRepository.save(existing)).thenReturn(existing);

            ExpectationQueryResult result = expectationService.saveTlExpectation(5, tlUser, "更新後コメント");

            assertThat(result.tlExpectation()).isEqualTo("更新後コメント");
        }

        @Test
        void 異常系_TL以外のロール_403をスロー() {
            try {
                expectationService.saveTlExpectation(5, generalUser, "コメント");
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getReason()).isEqualTo("EXPECTATION_TL_ONLY");
            }
        }

        @Test
        void 異常系_担当外TL_403をスロー() {
            when(userRepository.findById(5)).thenReturn(Optional.of(targetUser));

            try {
                expectationService.saveTlExpectation(5, otherTlUser, "コメント");
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getReason()).isEqualTo("EXPECTATION_ASSIGNED_TL_ONLY");
            }
        }
    }

    @Nested
    class SaveCompanyExpectation {

        @Test
        void 正常系_ADMIN_新規レコードを作成して保存する() {
            when(userRepository.findById(5)).thenReturn(Optional.of(targetUser));
            when(expectationRepository.findByUserId(5)).thenReturn(Optional.empty());
            when(expectationRepository.save(org.mockito.ArgumentMatchers.any(UserExpectation.class)))
                    .thenAnswer(new org.mockito.stubbing.Answer<UserExpectation>() {
                        @Override
                        public UserExpectation answer(org.mockito.invocation.InvocationOnMock invocation) {
                            return invocation.getArgument(0);
                        }
                    });

            ExpectationQueryResult result = expectationService.saveCompanyExpectation(5, adminUser, "会社期待コメント");

            assertThat(result.companyExpectation()).isEqualTo("会社期待コメント");
        }

        @Test
        void 異常系_ADMIN以外のロール_403をスロー() {
            try {
                expectationService.saveCompanyExpectation(5, tlUser, "コメント");
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getReason()).isEqualTo("EXPECTATION_ADMIN_ONLY");
            }
        }
    }
}

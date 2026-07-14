package com.skilize.inventory.application;

import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.fiscalyear.domain.repository.FiscalYearRepository;
import com.skilize.inventory.application.command.*;
import com.skilize.inventory.application.query.GoalReviewQueryResult;
import com.skilize.inventory.domain.model.*;
import com.skilize.inventory.domain.repository.*;
import com.skilize.master.domain.model.*;
import com.skilize.master.domain.repository.*;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.shared.domain.exception.GoalIncompleteException;
import com.skilize.user.domain.model.Role;
import com.skilize.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * InventoryService の単体テスト（getComparison 以外の全メソッド）。
 * 所有権チェック（本人/TL/ADMIN/他人）・全件洗い替え・目標完了件数条件・目標振り返り分岐を検証する。
 * getComparison は InventoryServiceComparisonTest で別途検証済み。
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock ItSkillDetailRepository itSkillDetailRepository;
    @Mock QualificationDetailRepository qualificationDetailRepository;
    @Mock SeminarDetailRepository seminarDetailRepository;
    @Mock InventoryGoalRepository inventoryGoalRepository;
    @Mock FiscalYearRepository fiscalYearRepository;
    @Mock SkillLevelRepository skillLevelRepository;
    @Mock ItSkillRepository itSkillRepository;
    @Mock QualificationRepository qualificationRepository;
    @Mock AdSeminarRepository adSeminarRepository;
    @Mock SeminarCategoryRepository seminarCategoryRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks InventoryService inventoryService;

    private User generalUser;
    private User tlUser;
    private User otherUser;
    private FiscalYear currentFy;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        generalUser = User.create("user01", "テストユーザー", null, Role.GENERAL, null, "hash");
        ReflectionTestUtils.setField(generalUser, "id", 1);
        tlUser = User.create("tl01", "TL", null, Role.TL, null, "hash");
        ReflectionTestUtils.setField(tlUser, "id", 2);
        otherUser = User.create("user02", "他人", null, Role.GENERAL, null, "hash");
        ReflectionTestUtils.setField(otherUser, "id", 3);

        currentFy = FiscalYear.create("2025年度", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), null, null);
        ReflectionTestUtils.setField(currentFy, "id", 2);
        inventory = Inventory.create(generalUser, currentFy);
        ReflectionTestUtils.setField(inventory, "id", 10);
    }

    @Nested
    class FindById {

        @Test
        void 正常系_本人アクセス_取得できる() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));

            Inventory result = inventoryService.findById(10, generalUser);

            assertThat(result).isEqualTo(inventory);
        }

        @Test
        void 正常系_TLアクセス_取得できる() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));

            Inventory result = inventoryService.findById(10, tlUser);

            assertThat(result).isEqualTo(inventory);
        }

        @Test
        void 異常系_他人GENERALアクセス_FORBIDDENをスロー() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));

            try {
                inventoryService.findById(10, otherUser);
                fail("AuthException が発生する想定");
            } catch (AuthException e) {
                assertThat(e.getCode()).isEqualTo("FORBIDDEN");
            }
        }

        @Test
        void 異常系_棚卸不在_404をスロー() {
            when(inventoryRepository.findByIdWithAssociations(99)).thenReturn(Optional.empty());

            try {
                inventoryService.findById(99, generalUser);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }

    @Nested
    class Create {

        @Test
        void 正常系_新規棚卸を作成する() {
            when(fiscalYearRepository.findById(2)).thenReturn(Optional.of(currentFy));
            when(inventoryRepository.findByUserIdAndFiscalYearId(1, 2)).thenReturn(Optional.empty());
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

            Inventory result = inventoryService.create(generalUser, 2);

            assertThat(result).isEqualTo(inventory);
        }

        @Test
        void 異常系_年度不在_404をスロー() {
            when(fiscalYearRepository.findById(99)).thenReturn(Optional.empty());

            try {
                inventoryService.create(generalUser, 99);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }

        @Test
        void 異常系_同年度に既存棚卸あり_409をスロー() {
            when(fiscalYearRepository.findById(2)).thenReturn(Optional.of(currentFy));
            when(inventoryRepository.findByUserIdAndFiscalYearId(1, 2)).thenReturn(Optional.of(inventory));

            try {
                inventoryService.create(generalUser, 2);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(409);
            }
        }
    }

    @Nested
    class ItSkillDetails {

        @Test
        void saveItSkillDetails_正常系_全件洗い替えで保存する() {
            SkillLevel level = SkillLevel.create((short) 3, "上級", 30);
            ReflectionTestUtils.setField(level, "id", 1);
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(skillLevelRepository.findById(1)).thenReturn(Optional.of(level));
            when(itSkillDetailRepository.findByInventoryId(10)).thenReturn(List.of());

            List<ItSkillDetailCommand> commands = List.of(
                    new ItSkillDetailCommand(null, null, "自作スキル", 1, "備考"));

            inventoryService.saveItSkillDetails(10, generalUser, commands);

            verify(itSkillDetailRepository).deleteByInventoryId(10);
            verify(itSkillDetailRepository).saveAll(any());
        }

        @Test
        void saveItSkillDetails_異常系_ITスキル不在_404をスロー() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(itSkillRepository.findById(99)).thenReturn(Optional.empty());

            List<ItSkillDetailCommand> commands = List.of(
                    new ItSkillDetailCommand(null, 99, null, 1, null));

            try {
                inventoryService.saveItSkillDetails(10, generalUser, commands);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getBody().getDetail()).isEqualTo("IT_SKILL_NOT_FOUND");
            }
        }

        @Test
        void saveItSkillDetails_異常系_スキルレベル不在_404をスロー() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(skillLevelRepository.findById(99)).thenReturn(Optional.empty());

            List<ItSkillDetailCommand> commands = List.of(
                    new ItSkillDetailCommand(null, null, "自作スキル", 99, null));

            try {
                inventoryService.saveItSkillDetails(10, generalUser, commands);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getBody().getDetail()).isEqualTo("SKILL_LEVEL_NOT_FOUND");
            }
        }

        @Test
        void updateItSkillDetailRemarks_正常系_備考を更新する() {
            ItSkill skill = ItSkill.create(null, "Java", null, 1);
            SkillLevel level = SkillLevel.create((short) 3, "上級", 30);
            ItSkillDetail detail = ItSkillDetail.create(inventory, skill, null, level, "旧備考");
            ReflectionTestUtils.setField(detail, "id", 50);
            ReflectionTestUtils.setField(detail, "inventoryId", 10);

            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(itSkillDetailRepository.findById(50)).thenReturn(Optional.of(detail));
            when(itSkillDetailRepository.save(detail)).thenReturn(detail);

            ItSkillDetail result = inventoryService.updateItSkillDetailRemarks(10, 50, generalUser, "新備考");

            assertThat(result.getRemarks()).isEqualTo("新備考");
        }

        @Test
        void updateItSkillDetailRemarks_異常系_別棚卸の明細_403をスロー() {
            ItSkill skill = ItSkill.create(null, "Java", null, 1);
            SkillLevel level = SkillLevel.create((short) 3, "上級", 30);
            ItSkillDetail detail = ItSkillDetail.create(inventory, skill, null, level, null);
            ReflectionTestUtils.setField(detail, "id", 50);
            ReflectionTestUtils.setField(detail, "inventoryId", 999);

            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(itSkillDetailRepository.findById(50)).thenReturn(Optional.of(detail));

            try {
                inventoryService.updateItSkillDetailRemarks(10, 50, generalUser, "新備考");
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(403);
            }
        }

        @Test
        void updateItSkillDetailRemarks_異常系_明細不在_404をスロー() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(itSkillDetailRepository.findById(99)).thenReturn(Optional.empty());

            try {
                inventoryService.updateItSkillDetailRemarks(10, 99, generalUser, "新備考");
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }

    @Nested
    class QualificationDetails {

        @Test
        void saveQualificationDetails_正常系_日付文字列をLocalDateへ変換して保存する() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(qualificationDetailRepository.findByInventoryId(10)).thenReturn(List.of());

            List<QualificationDetailCommand> commands = List.of(
                    new QualificationDetailCommand(null, null, "自作資格", "2020-05-01", null));

            inventoryService.saveQualificationDetails(10, generalUser, commands);

            verify(qualificationDetailRepository).deleteByInventoryId(10);
            verify(qualificationDetailRepository).saveAll(any());
        }

        @Test
        void saveQualificationDetails_異常系_資格不在_404をスロー() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(qualificationRepository.findById(99)).thenReturn(Optional.empty());

            List<QualificationDetailCommand> commands = List.of(
                    new QualificationDetailCommand(null, 99, null, null, null));

            try {
                inventoryService.saveQualificationDetails(10, generalUser, commands);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getBody().getDetail()).isEqualTo("QUALIFICATION_NOT_FOUND");
            }
        }

        @Test
        void findQualificationDetails_正常系_一覧を返す() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(qualificationDetailRepository.findByInventoryId(10)).thenReturn(List.of());

            List<QualificationDetail> result = inventoryService.findQualificationDetails(10, generalUser);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class SeminarDetails {

        @Test
        void saveSeminarDetails_正常系_ADセミナー指定時はカテゴリを無視する() {
            AdSeminar ad = AdSeminar.create(null, "AWS研修", null, 1);
            ReflectionTestUtils.setField(ad, "id", 1);
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(adSeminarRepository.findById(1)).thenReturn(Optional.of(ad));
            when(seminarDetailRepository.findByInventoryId(10)).thenReturn(List.of());

            List<SeminarDetailCommand> commands = List.of(
                    new SeminarDetailCommand(null, 1, null, 5, "2025-06-01", null));

            inventoryService.saveSeminarDetails(10, generalUser, commands);

            verify(seminarCategoryRepository, org.mockito.Mockito.never()).findById(any());
            verify(seminarDetailRepository).saveAll(any());
        }

        @Test
        void saveSeminarDetails_正常系_自由入力セミナー_カテゴリを解決する() {
            SeminarCategory cat = SeminarCategory.reconstruct(5, "技術研修", 1, true, null, null);
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(seminarCategoryRepository.findById(5)).thenReturn(Optional.of(cat));
            when(seminarDetailRepository.findByInventoryId(10)).thenReturn(List.of());

            List<SeminarDetailCommand> commands = List.of(
                    new SeminarDetailCommand(null, null, "社外勉強会", 5, "2025-06-01", null));

            inventoryService.saveSeminarDetails(10, generalUser, commands);

            verify(seminarCategoryRepository).findById(5);
            verify(seminarDetailRepository).saveAll(any());
        }

        @Test
        void saveSeminarDetails_異常系_ADセミナー不在_404をスロー() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(adSeminarRepository.findById(99)).thenReturn(Optional.empty());

            List<SeminarDetailCommand> commands = List.of(
                    new SeminarDetailCommand(null, 99, null, null, null, null));

            try {
                inventoryService.saveSeminarDetails(10, generalUser, commands);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getBody().getDetail()).isEqualTo("AD_SEMINAR_NOT_FOUND");
            }
        }
    }

    @Nested
    class Submit {

        @Test
        void 正常系_ステータスがPENDING_GOALに遷移する() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(inventory)).thenReturn(inventory);

            Inventory result = inventoryService.submit(10, generalUser);

            assertThat(result.getStatus()).isEqualTo(InventoryStatus.PENDING_GOAL);
            assertThat(result.getSubmittedAt()).isNotNull();
        }
    }

    @Nested
    class GoalReview {

        @Test
        void getGoalReview_正常系_前年度なし_空レスポンスを返す() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(inventory));

            GoalReviewQueryResult result = inventoryService.getGoalReview(10, generalUser);

            assertThat(result.hasPrevGoals()).isFalse();
            assertThat(result.items()).isEmpty();
        }

        @Test
        void getGoalReview_正常系_前年度目標あり_名称と達成状況を返す() {
            FiscalYear prevFy = FiscalYear.create("2024年度", LocalDate.of(2024, 4, 1), LocalDate.of(2025, 3, 31), null, null);
            ReflectionTestUtils.setField(prevFy, "id", 1);
            Inventory prevInv = Inventory.create(generalUser, prevFy);
            ReflectionTestUtils.setField(prevInv, "id", 9);

            ItSkill skill = ItSkill.create(null, "Java", null, 1);
            InventoryGoal goal = InventoryGoal.create(prevInv, GoalCategory.IT_SKILL, skill, null, null, null,
                    LocalDate.of(2025, 6, 1), "理由");
            ReflectionTestUtils.setField(goal, "id", 100);
            ReflectionTestUtils.setField(goal, "achievementStatus", AchievementStatus.ACHIEVED);

            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(inventory, prevInv));
            when(inventoryGoalRepository.findByInventoryId(9)).thenReturn(List.of(goal));

            GoalReviewQueryResult result = inventoryService.getGoalReview(10, generalUser);

            assertThat(result.hasPrevGoals()).isTrue();
            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).goalName()).isEqualTo("Java");
            assertThat(result.items().get(0).achievementStatus()).isEqualTo("ACHIEVED");
        }

        @Test
        void saveGoalReview_正常系_達成状況を更新する() {
            FiscalYear prevFy = FiscalYear.create("2024年度", LocalDate.of(2024, 4, 1), LocalDate.of(2025, 3, 31), null, null);
            ReflectionTestUtils.setField(prevFy, "id", 1);
            Inventory prevInv = Inventory.create(generalUser, prevFy);
            ReflectionTestUtils.setField(prevInv, "id", 9);
            ItSkill skill = ItSkill.create(null, "Java", null, 1);
            InventoryGoal goal = InventoryGoal.create(prevInv, GoalCategory.IT_SKILL, skill, null, null, null,
                    LocalDate.of(2025, 6, 1), "理由");
            ReflectionTestUtils.setField(goal, "id", 100);

            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(inventoryGoalRepository.findById(100)).thenReturn(Optional.of(goal));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(inventory, prevInv));
            when(inventoryGoalRepository.findByInventoryId(9)).thenReturn(List.of(goal));

            List<GoalReviewUpdateCommand> commands = List.of(
                    new GoalReviewUpdateCommand(100, "PARTIAL", "半分だけ達成"));

            inventoryService.saveGoalReview(10, generalUser, commands);

            assertThat(goal.getAchievementStatus()).isEqualTo(AchievementStatus.PARTIAL);
            assertThat(goal.getReviewNote()).isEqualTo("半分だけ達成");
            verify(inventoryGoalRepository).save(goal);
        }

        @Test
        void saveGoalReview_異常系_目標不在_404をスロー() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(inventoryGoalRepository.findById(999)).thenReturn(Optional.empty());

            List<GoalReviewUpdateCommand> commands = List.of(
                    new GoalReviewUpdateCommand(999, "ACHIEVED", null));

            try {
                inventoryService.saveGoalReview(10, generalUser, commands);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }

        @Test
        void completeGoalReview_正常系_完了日時が設定される() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(inventory)).thenReturn(inventory);

            Inventory result = inventoryService.completeGoalReview(10, generalUser);

            assertThat(result.getGoalReviewCompletedAt()).isNotNull();
        }
    }

    @Nested
    class Goals {

        @Test
        void saveGoals_正常系_目標期間の文字列をLocalDateへ変換して保存する() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(inventoryGoalRepository.findByInventoryId(10)).thenReturn(List.of());

            List<GoalCommand> commands = List.of(
                    new GoalCommand(null, "IT_SKILL", null, null, null, "カスタム目標", "2026-01-01", "理由"));

            inventoryService.saveGoals(10, generalUser, commands);

            verify(inventoryGoalRepository).deleteByInventoryId(10);
            verify(inventoryGoalRepository).saveAll(any());
        }

        @Test
        void completeGoal_正常系_件数条件を満たす_ステータスがCOMPLETEDに遷移しイベント発行される() {
            List<InventoryGoal> goals = List.of(
                    InventoryGoal.create(inventory, GoalCategory.IT_SKILL, null, null, null, "目標1", LocalDate.now(), null),
                    InventoryGoal.create(inventory, GoalCategory.AD, null, null, null, "目標2", LocalDate.now(), null),
                    InventoryGoal.create(inventory, GoalCategory.AD, null, null, null, "目標3", LocalDate.now(), null));

            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(inventoryGoalRepository.findByInventoryId(10)).thenReturn(goals);
            when(inventoryRepository.save(inventory)).thenReturn(inventory);

            Inventory result = inventoryService.completeGoal(10, generalUser);

            assertThat(result.getStatus()).isEqualTo(InventoryStatus.COMPLETED);
            verify(eventPublisher).publishEvent(any(InventoryCompletedEvent.class));
        }

        @Test
        void completeGoal_異常系_件数不足_GoalIncompleteExceptionをスロー() {
            List<InventoryGoal> goals = List.of(
                    InventoryGoal.create(inventory, GoalCategory.AD, null, null, null, "目標1", LocalDate.now(), null));

            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(inventoryGoalRepository.findByInventoryId(10)).thenReturn(goals);

            try {
                inventoryService.completeGoal(10, generalUser);
                fail("GoalIncompleteException が発生する想定");
            } catch (GoalIncompleteException e) {
                assertThat(e.getErrors()).hasSize(2);
            }
        }

        @Test
        void findGoals_正常系_一覧を返す() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(inventoryGoalRepository.findByInventoryId(10)).thenReturn(List.of());

            List<InventoryGoal> result = inventoryService.findGoals(10, generalUser);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindMine {

        @Test
        void 正常系_ユーザーの棚卸一覧を返す() {
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(inventory));

            List<Inventory> result = inventoryService.findMine(1);

            assertThat(result).containsExactly(inventory);
        }
    }
}

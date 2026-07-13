package com.skilize.inventory.application;

import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.fiscalyear.domain.repository.FiscalYearRepository;
import com.skilize.inventory.application.query.ComparisonQueryResult;
import com.skilize.inventory.application.query.ComparisonQueryResult.ComparisonItem;
import com.skilize.inventory.domain.model.*;
import com.skilize.inventory.domain.repository.*;
import com.skilize.master.domain.model.*;
import com.skilize.master.domain.repository.*;
import com.skilize.shared.domain.exception.AuthException;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * InventoryService#getComparison の単体テスト。
 * カスタムスキルの差分計算・前年度なし分岐・アクセス制御を検証する。
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceComparisonTest {

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
    private FiscalYear currentFy;
    private FiscalYear prevFy;
    private Inventory currentInv;
    private Inventory prevInv;
    private ItSkill itSkillA;
    private SkillLevel level2;
    private SkillLevel level3;

    @BeforeEach
    void setUp() {
        generalUser = User.create("user01", "テストユーザー", null, Role.GENERAL, null, "hash");
        ReflectionTestUtils.setField(generalUser, "id", 1);

        currentFy = FiscalYear.create("2025年度",
                LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), null, null);
        ReflectionTestUtils.setField(currentFy, "id", 2);

        prevFy = FiscalYear.create("2024年度",
                LocalDate.of(2024, 4, 1), LocalDate.of(2025, 3, 31), null, null);
        ReflectionTestUtils.setField(prevFy, "id", 1);

        currentInv = Inventory.create(generalUser, currentFy);
        ReflectionTestUtils.setField(currentInv, "id", 10);

        prevInv = Inventory.create(generalUser, prevFy);
        ReflectionTestUtils.setField(prevInv, "id", 9);

        itSkillA = ItSkill.create(null, "Java", "Javaスキル", 1);
        ReflectionTestUtils.setField(itSkillA, "id", 100);

        level2 = SkillLevel.create((short) 2, "中級", 2);
        level3 = SkillLevel.create((short) 3, "上級", 3);
    }

    // ═══════════════════════════════════════════════════════════
    //  getComparison
    // ═══════════════════════════════════════════════════════════

    @Nested
    class GetComparison {

        @Test
        void 正常系_前年度なし_hasPrevYearがfalseで空リストを返す() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(currentInv));
            when(itSkillDetailRepository.findByInventoryId(10)).thenReturn(List.of());
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(currentInv));

            ComparisonQueryResult result = inventoryService.getComparison(10, generalUser);

            assertThat(result.hasPrevYear()).isFalse();
            assertThat(result.items()).isEmpty();
            assertThat(result.prevFiscalYear()).isNull();
            assertThat(result.currentFiscalYear()).isEqualTo("2025年度");
        }

        @Test
        void 正常系_マスタスキルのみ_レベル差分が正しく計算される() {
            ItSkillDetail currentDetail = ItSkillDetail.create(currentInv, itSkillA, null, level3, "備考A");
            ReflectionTestUtils.setField(currentDetail, "id", 101);

            ItSkillDetail prevDetail = ItSkillDetail.create(prevInv, itSkillA, null, level2, null);
            ReflectionTestUtils.setField(prevDetail, "id", 90);

            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(currentInv));
            when(itSkillDetailRepository.findByInventoryId(10)).thenReturn(List.of(currentDetail));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(currentInv, prevInv));
            when(itSkillDetailRepository.findByInventoryId(9)).thenReturn(List.of(prevDetail));

            ComparisonQueryResult result = inventoryService.getComparison(10, generalUser);

            assertThat(result.hasPrevYear()).isTrue();
            assertThat(result.currentFiscalYear()).isEqualTo("2025年度");
            assertThat(result.prevFiscalYear()).isEqualTo("2024年度");
            assertThat(result.items()).hasSize(1);

            ComparisonItem item = result.items().get(0);
            assertThat(item.itSkillId()).isEqualTo(100);
            assertThat(item.skillName()).isEqualTo("Java");
            assertThat(item.currentDetailId()).isEqualTo(101);
            assertThat(item.currentLevelValue()).isEqualTo(3);
            assertThat(item.prevLevelValue()).isEqualTo(2);
            assertThat(item.diff()).isEqualTo(1);
            assertThat(item.currentRemarks()).isEqualTo("備考A");
        }

        @Test
        void 正常系_前年度にないスキル_prevLevelValueとdiffがnull() {
            ItSkillDetail currentDetail = ItSkillDetail.create(currentInv, itSkillA, null, level3, null);
            ReflectionTestUtils.setField(currentDetail, "id", 101);

            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(currentInv));
            when(itSkillDetailRepository.findByInventoryId(10)).thenReturn(List.of(currentDetail));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(currentInv, prevInv));
            when(itSkillDetailRepository.findByInventoryId(9)).thenReturn(List.of());

            ComparisonQueryResult result = inventoryService.getComparison(10, generalUser);

            assertThat(result.hasPrevYear()).isTrue();
            assertThat(result.items()).hasSize(1);

            ComparisonItem item = result.items().get(0);
            assertThat(item.itSkillId()).isEqualTo(100);
            assertThat(item.currentLevelValue()).isEqualTo(3);
            assertThat(item.prevLevelValue()).isNull();
            assertThat(item.diff()).isNull();
        }

        @Test
        void 正常系_カスタムスキルを含む_currentLevelValueとdiffがnull() {
            ItSkillDetail masterDetail = ItSkillDetail.create(currentInv, itSkillA, null, level3, null);
            ReflectionTestUtils.setField(masterDetail, "id", 101);

            ItSkillDetail customDetail = ItSkillDetail.create(currentInv, null, "自作フレームワーク", level2, "独自開発");
            ReflectionTestUtils.setField(customDetail, "id", 102);

            ItSkillDetail prevMasterDetail = ItSkillDetail.create(prevInv, itSkillA, null, level2, null);
            ReflectionTestUtils.setField(prevMasterDetail, "id", 90);

            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(currentInv));
            when(itSkillDetailRepository.findByInventoryId(10)).thenReturn(List.of(masterDetail, customDetail));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(currentInv, prevInv));
            when(itSkillDetailRepository.findByInventoryId(9)).thenReturn(List.of(prevMasterDetail));

            ComparisonQueryResult result = inventoryService.getComparison(10, generalUser);

            assertThat(result.hasPrevYear()).isTrue();
            assertThat(result.items()).hasSize(2);

            ComparisonItem masterItem = null;
            ComparisonItem customItem = null;
            for (ComparisonItem i : result.items()) {
                if (i.itSkillId() != null) {
                    masterItem = i;
                } else {
                    customItem = i;
                }
            }
            assertThat(masterItem).isNotNull();
            assertThat(customItem).isNotNull();

            assertThat(masterItem.skillName()).isEqualTo("Java");
            assertThat(masterItem.currentLevelValue()).isEqualTo(3);
            assertThat(masterItem.prevLevelValue()).isEqualTo(2);
            assertThat(masterItem.diff()).isEqualTo(1);

            assertThat(customItem.skillName()).isEqualTo("自作フレームワーク");
            assertThat(customItem.currentLevelValue()).isNull();
            assertThat(customItem.prevLevelValue()).isNull();
            assertThat(customItem.diff()).isNull();
            assertThat(customItem.currentRemarks()).isEqualTo("独自開発");
        }

        @Test
        void 異常系_他ユーザーの棚卸へGENERALアクセス_FORBIDDENをスロー() {
            User otherUser = User.create("user02", "別ユーザー", null, Role.GENERAL, null, "hash");
            ReflectionTestUtils.setField(otherUser, "id", 2);

            Inventory otherInv = Inventory.create(otherUser, currentFy);
            ReflectionTestUtils.setField(otherInv, "id", 10);

            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(otherInv));

            try {
                inventoryService.getComparison(10, generalUser);
                fail("AuthException が発生する想定");
            } catch (AuthException e) {
                assertThat(e.getCode()).isEqualTo("FORBIDDEN");
            }
        }
    }
}

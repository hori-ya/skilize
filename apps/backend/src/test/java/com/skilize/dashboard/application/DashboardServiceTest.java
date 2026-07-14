package com.skilize.dashboard.application;

import com.skilize.dashboard.application.query.DashboardQueryResult;
import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.fiscalyear.domain.repository.FiscalYearRepository;
import com.skilize.inventory.domain.model.Inventory;
import com.skilize.inventory.domain.model.ItSkillDetail;
import com.skilize.inventory.domain.model.QualificationDetail;
import com.skilize.inventory.domain.model.SeminarDetail;
import com.skilize.inventory.domain.repository.InventoryRepository;
import com.skilize.inventory.domain.repository.ItSkillDetailRepository;
import com.skilize.inventory.domain.repository.QualificationDetailRepository;
import com.skilize.inventory.domain.repository.SeminarDetailRepository;
import com.skilize.user.domain.model.Role;
import com.skilize.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * DashboardService の単体テスト。現在年度・当年度棚卸・明細件数の段階的な null 分岐を検証する。
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock FiscalYearRepository fiscalYearRepository;
    @Mock InventoryRepository inventoryRepository;
    @Mock ItSkillDetailRepository itSkillDetailRepository;
    @Mock QualificationDetailRepository qualificationDetailRepository;
    @Mock SeminarDetailRepository seminarDetailRepository;

    @InjectMocks DashboardService dashboardService;

    private User user;
    private FiscalYear currentFy;

    @BeforeEach
    void setUp() {
        user = User.create("user01", "テストユーザー", null, Role.GENERAL, null, "hash");
        ReflectionTestUtils.setField(user, "id", 1);

        currentFy = FiscalYear.create("2025年度", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), null, null);
        ReflectionTestUtils.setField(currentFy, "id", 2);
    }

    @Nested
    class GetDashboard {

        @Test
        void 正常系_有効年度なし_全項目デフォルト値を返す() {
            when(fiscalYearRepository.findCurrent(any(LocalDate.class))).thenReturn(Optional.empty());

            DashboardQueryResult result = dashboardService.getDashboard(1);

            assertThat(result.fiscalYear()).isNull();
            assertThat(result.inventory()).isNull();
            assertThat(result.itSkillCount()).isEqualTo(0);
            assertThat(result.qualificationCount()).isEqualTo(0);
            assertThat(result.seminarCount()).isEqualTo(0);
        }

        @Test
        void 正常系_有効年度ありだが今年度棚卸未作成_棚卸nullで返す() {
            when(fiscalYearRepository.findCurrent(any(LocalDate.class))).thenReturn(Optional.of(currentFy));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of());

            DashboardQueryResult result = dashboardService.getDashboard(1);

            assertThat(result.fiscalYear()).isEqualTo(currentFy);
            assertThat(result.inventory()).isNull();
            assertThat(result.itSkillCount()).isEqualTo(0);
        }

        @Test
        void 正常系_今年度棚卸あり_各明細件数を返す() {
            Inventory currentInv = Inventory.create(user, currentFy);
            ReflectionTestUtils.setField(currentInv, "id", 10);

            ItSkillDetail itDetail = ItSkillDetail.create(currentInv, null, "自作スキル", null, null);
            QualificationDetail qualDetail = QualificationDetail.create(currentInv, null, "自作資格", null, null);
            SeminarDetail semDetail1 = SeminarDetail.create(currentInv, null, "セミナーA", null, null, null);
            SeminarDetail semDetail2 = SeminarDetail.create(currentInv, null, "セミナーB", null, null, null);

            when(fiscalYearRepository.findCurrent(any(LocalDate.class))).thenReturn(Optional.of(currentFy));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(currentInv));
            when(itSkillDetailRepository.findByInventoryId(10)).thenReturn(List.of(itDetail));
            when(qualificationDetailRepository.findByInventoryId(10)).thenReturn(List.of(qualDetail));
            when(seminarDetailRepository.findByInventoryId(10)).thenReturn(List.of(semDetail1, semDetail2));

            DashboardQueryResult result = dashboardService.getDashboard(1);

            assertThat(result.inventory()).isEqualTo(currentInv);
            assertThat(result.itSkillCount()).isEqualTo(1);
            assertThat(result.qualificationCount()).isEqualTo(1);
            assertThat(result.seminarCount()).isEqualTo(2);
        }

        @Test
        void 正常系_他年度の棚卸は今年度棚卸として扱わない() {
            FiscalYear otherFy = FiscalYear.create("2023年度", LocalDate.of(2023, 4, 1), LocalDate.of(2024, 3, 31), null, null);
            ReflectionTestUtils.setField(otherFy, "id", 99);
            Inventory otherInv = Inventory.create(user, otherFy);
            ReflectionTestUtils.setField(otherInv, "id", 5);

            when(fiscalYearRepository.findCurrent(any(LocalDate.class))).thenReturn(Optional.of(currentFy));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(otherInv));

            DashboardQueryResult result = dashboardService.getDashboard(1);

            assertThat(result.inventory()).isNull();
        }
    }
}

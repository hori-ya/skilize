package com.skilize.report.application;

import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.inventory.domain.model.*;
import com.skilize.inventory.domain.repository.InventoryGoalRepository;
import com.skilize.inventory.domain.repository.InventoryRepository;
import com.skilize.inventory.domain.repository.ItSkillDetailRepository;
import com.skilize.inventory.domain.repository.SeminarDetailRepository;
import com.skilize.master.domain.model.*;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

/**
 * ReportService の単体テスト。アクセス制御分岐（本人/TL/ADMIN/他人）と
 * 棚卸不在時の404、および実テンプレートを用いたPDF生成成功パスを検証する。
 * @PostConstruct はコンテナ外で呼ばれないため initFonts() を明示的に呼び出してから検証する。
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock ItSkillDetailRepository itSkillDetailRepository;
    @Mock SeminarDetailRepository seminarDetailRepository;
    @Mock InventoryGoalRepository inventoryGoalRepository;

    @InjectMocks ReportService reportService;

    private User owner;
    private User tlUser;
    private User otherUser;
    private FiscalYear fy;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.invokeMethod(reportService, "initFonts");

        owner = User.create("user01", "対象ユーザー", null, Role.GENERAL, null, "hash");
        ReflectionTestUtils.setField(owner, "id", 1);
        tlUser = User.create("tl01", "TL", null, Role.TL, null, "hash");
        ReflectionTestUtils.setField(tlUser, "id", 2);
        otherUser = User.create("user02", "他人", null, Role.GENERAL, null, "hash");
        ReflectionTestUtils.setField(otherUser, "id", 3);

        fy = FiscalYear.create("2025年度", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), null, null);
        ReflectionTestUtils.setField(fy, "id", 1);
        inventory = Inventory.create(owner, fy);
        ReflectionTestUtils.setField(inventory, "id", 10);
    }

    @Nested
    class GenerateInventoryReport {

        @Test
        void 異常系_棚卸不在_404をスロー() {
            when(inventoryRepository.findByIdWithAssociations(99)).thenReturn(Optional.empty());

            try {
                reportService.generateInventoryReport(99L, owner);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }

        @Test
        void 異常系_他人の棚卸へGENERALアクセス_FORBIDDENをスロー() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));

            try {
                reportService.generateInventoryReport(10L, otherUser);
                fail("AuthException が発生する想定");
            } catch (AuthException e) {
                assertThat(e.getCode()).isEqualTo("FORBIDDEN");
            }
        }

        @Test
        void 正常系_本人アクセス_PDFバイナリを返す() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(itSkillDetailRepository.findByInventoryIdWithCategories(10)).thenReturn(List.of());
            when(seminarDetailRepository.findByInventoryId(10)).thenReturn(List.of());
            when(inventoryGoalRepository.findByInventoryIdForReport(10)).thenReturn(List.of());

            byte[] pdf = reportService.generateInventoryReport(10L, owner);

            assertThat(pdf).isNotEmpty();
            // PDF ファイルのマジックナンバー（先頭4バイト "%PDF"）を確認する
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        }

        @Test
        void 正常系_TLアクセス_他人の棚卸でもPDFを返す() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(itSkillDetailRepository.findByInventoryIdWithCategories(10)).thenReturn(List.of());
            when(seminarDetailRepository.findByInventoryId(10)).thenReturn(List.of());
            when(inventoryGoalRepository.findByInventoryIdForReport(10)).thenReturn(List.of());

            byte[] pdf = reportService.generateInventoryReport(10L, tlUser);

            assertThat(pdf).isNotEmpty();
        }

        @Test
        void 正常系_明細データありでもPDF生成される() {
            ItSkillCategory cat1 = ItSkillCategory.create(null, (short) 1, "プログラミング", 1);
            ReflectionTestUtils.setField(cat1, "id", 100);
            ItSkillCategory cat2 = ItSkillCategory.create(100, (short) 2, "言語", 1);
            ReflectionTestUtils.setField(cat2, "id", 101);
            ReflectionTestUtils.setField(cat2, "parent", cat1);
            ItSkill skill = ItSkill.create(cat2, "Java", null, 1);
            SkillLevel level = SkillLevel.create((short) 3, "上級", 30);
            ItSkillDetail detail = ItSkillDetail.create(inventory, skill, null, level, "備考");

            AdSeminarCategory adCat = AdSeminarCategory.create("研修", 1);
            AdSeminar adSeminar = AdSeminar.create(adCat, "AWS研修", null, 1);
            SeminarDetail semDetail = SeminarDetail.create(inventory, adSeminar, null, null,
                    LocalDate.of(2025, 6, 1), "受講メモ");

            InventoryGoal itGoal = InventoryGoal.create(inventory, GoalCategory.IT_SKILL, skill, null,
                    null, null, LocalDate.of(2026, 1, 1), "理由");
            InventoryGoal adGoal = InventoryGoal.create(inventory, GoalCategory.AD, null, null,
                    adSeminar, null, LocalDate.of(2026, 2, 1), "AD理由");

            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(inventory));
            when(itSkillDetailRepository.findByInventoryIdWithCategories(10)).thenReturn(List.of(detail));
            when(seminarDetailRepository.findByInventoryId(10)).thenReturn(List.of(semDetail));
            when(inventoryGoalRepository.findByInventoryIdForReport(10)).thenReturn(List.of(itGoal, adGoal));

            byte[] pdf = reportService.generateInventoryReport(10L, owner);

            assertThat(pdf).isNotEmpty();
        }
    }
}

package com.skilize.charts.application;

import com.skilize.charts.application.query.*;
import com.skilize.charts.application.query.GrowthQueryResult.GrowthSeries;
import com.skilize.charts.application.query.HeatmapQueryResult.HeatmapCell;
import com.skilize.charts.application.query.HeatmapQueryResult.HeatmapRow;
import com.skilize.charts.application.query.RadarQueryResult.RadarAxis;
import com.skilize.charts.application.query.TimelineQueryResult.TimelineEvent;
import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.fiscalyear.domain.repository.FiscalYearRepository;
import com.skilize.inventory.domain.model.*;
import com.skilize.inventory.domain.repository.*;
import com.skilize.master.domain.model.*;
import com.skilize.master.domain.repository.*;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ChartService の単体テスト。radar/growth/heatmap/timeline の集計ロジックと
 * カテゴリツリー遡上・当年度データなし分岐を検証する。
 */
@ExtendWith(MockitoExtension.class)
class ChartServiceTest {

    @Mock FiscalYearRepository fiscalYearRepository;
    @Mock InventoryRepository inventoryRepository;
    @Mock ItSkillDetailRepository itSkillDetailRepository;
    @Mock QualificationDetailRepository qualificationDetailRepository;
    @Mock SeminarDetailRepository seminarDetailRepository;
    @Mock InventoryGoalRepository inventoryGoalRepository;
    @Mock ItSkillCategoryRepository itSkillCategoryRepository;
    @Mock ItSkillRepository itSkillRepository;
    @Mock SkillLevelRepository skillLevelRepository;

    @InjectMocks ChartService chartService;

    private User user;
    private ItSkillCategory cat1;
    private ItSkillCategory cat2;
    private ItSkill skillA;
    private SkillLevel level2;
    private SkillLevel level4;
    private FiscalYear currentFy;
    private FiscalYear prevFy;

    @BeforeEach
    void setUp() {
        user = User.create("user01", "テストユーザー", null, Role.GENERAL, null, "hash");
        ReflectionTestUtils.setField(user, "id", 1);

        cat1 = ItSkillCategory.create(null, (short) 1, "プログラミング", 1);
        ReflectionTestUtils.setField(cat1, "id", 10);
        cat2 = ItSkillCategory.create(10, (short) 2, "言語", 1);
        ReflectionTestUtils.setField(cat2, "id", 20);

        skillA = ItSkill.create(cat2, "Java", "Javaスキル", 1);
        ReflectionTestUtils.setField(skillA, "id", 100);

        level2 = SkillLevel.create((short) 2, "中級", 20);
        level4 = SkillLevel.create((short) 4, "上級", 40);

        currentFy = FiscalYear.create("2025年度", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), null, null);
        ReflectionTestUtils.setField(currentFy, "id", 2);
        prevFy = FiscalYear.create("2024年度", LocalDate.of(2024, 4, 1), LocalDate.of(2025, 3, 31), null, null);
        ReflectionTestUtils.setField(prevFy, "id", 1);
    }

    private Inventory buildInventory(int id, FiscalYear fy, InventoryStatus status) {
        Inventory inv = Inventory.create(user, fy);
        ReflectionTestUtils.setField(inv, "id", id);
        ReflectionTestUtils.setField(inv, "status", status);
        return inv;
    }

    // ═══════════════════════════════════════════════════════════
    //  getRadar
    // ═══════════════════════════════════════════════════════════

    @Nested
    class GetRadar {

        @Test
        void 正常系_当年度なし_全軸スコア0で返す() {
            when(itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()).thenReturn(List.of(cat1));
            when(skillLevelRepository.findByActiveOrderByLevelValueAsc(true)).thenReturn(List.of(level2, level4));
            when(fiscalYearRepository.findCurrent(any(LocalDate.class))).thenReturn(Optional.empty());

            RadarQueryResult result = chartService.getRadar(user);

            assertThat(result.currentFiscalYear()).isNull();
            assertThat(result.hasCurrentYearData()).isFalse();
            assertThat(result.axes()).hasSize(1);
            RadarAxis axis = result.axes().get(0);
            assertThat(axis.category1Id()).isEqualTo(10);
            assertThat(axis.currentAvgScore()).isEqualTo(0.0);
            assertThat(axis.prevAvgScore()).isNull();
        }

        @Test
        void 正常系_当年度棚卸なし_平均0で前年度情報なし() {
            when(itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()).thenReturn(List.of(cat1, cat2));
            when(skillLevelRepository.findByActiveOrderByLevelValueAsc(true)).thenReturn(List.of(level2, level4));
            when(fiscalYearRepository.findCurrent(any(LocalDate.class))).thenReturn(Optional.of(currentFy));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of());

            RadarQueryResult result = chartService.getRadar(user);

            assertThat(result.currentFiscalYear()).isEqualTo("2025年度");
            assertThat(result.hasCurrentYearData()).isFalse();
            assertThat(result.prevFiscalYear()).isNull();
        }

        @Test
        void 正常系_当年度と前年度データあり_平均スコアと前年度名を返す() {
            Inventory currentInv = buildInventory(10, currentFy, InventoryStatus.PENDING_GOAL);
            Inventory prevInv = buildInventory(9, prevFy, InventoryStatus.COMPLETED);
            ItSkillDetail currentDetail = ItSkillDetail.create(currentInv, skillA, null, level4, null);
            ItSkillDetail prevDetail = ItSkillDetail.create(prevInv, skillA, null, level2, null);

            when(itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()).thenReturn(List.of(cat1, cat2));
            when(skillLevelRepository.findByActiveOrderByLevelValueAsc(true)).thenReturn(List.of(level2, level4));
            when(fiscalYearRepository.findCurrent(any(LocalDate.class))).thenReturn(Optional.of(currentFy));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(currentInv, prevInv));
            when(itSkillDetailRepository.findByInventoryId(10)).thenReturn(List.of(currentDetail));
            when(itSkillDetailRepository.findByInventoryId(9)).thenReturn(List.of(prevDetail));

            RadarQueryResult result = chartService.getRadar(user);

            assertThat(result.hasCurrentYearData()).isTrue();
            assertThat(result.prevFiscalYear()).isEqualTo("2024年度");
            RadarAxis axis = null;
            for (RadarAxis a : result.axes()) {
                if (a.category1Id() == 10) {
                    axis = a;
                }
            }
            assertThat(axis).isNotNull();
            assertThat(axis.currentAvgScore()).isEqualTo(40.0);
            assertThat(axis.prevAvgScore()).isEqualTo(20.0);
        }

        @Test
        void 正常系_カスタムスキルは集計対象外() {
            Inventory currentInv = buildInventory(10, currentFy, InventoryStatus.DRAFT);
            ItSkillDetail customDetail = ItSkillDetail.create(currentInv, null, "自作スキル", level4, null);

            when(itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()).thenReturn(List.of(cat1, cat2));
            when(skillLevelRepository.findByActiveOrderByLevelValueAsc(true)).thenReturn(List.of(level2, level4));
            when(fiscalYearRepository.findCurrent(any(LocalDate.class))).thenReturn(Optional.of(currentFy));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(currentInv));
            when(itSkillDetailRepository.findByInventoryId(10)).thenReturn(List.of(customDetail));

            RadarQueryResult result = chartService.getRadar(user);

            assertThat(result.hasCurrentYearData()).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  getGrowth
    // ═══════════════════════════════════════════════════════════

    @Nested
    class GetGrowth {

        @Test
        void 正常系_提出済み棚卸なし_空の系列を返す() {
            when(itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()).thenReturn(List.of(cat1));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of());

            GrowthQueryResult result = chartService.getGrowth(user);

            assertThat(result.fiscalYears()).isEmpty();
            assertThat(result.series()).hasSize(1);
            assertThat(result.series().get(0).yearlyTotalScores()).isEmpty();
        }

        @Test
        void 正常系_DRAFTの棚卸は集計対象外() {
            Inventory draftInv = buildInventory(5, currentFy, InventoryStatus.DRAFT);

            when(itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()).thenReturn(List.of(cat1));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(draftInv));

            GrowthQueryResult result = chartService.getGrowth(user);

            assertThat(result.fiscalYears()).isEmpty();
        }

        @Test
        void 正常系_マスタスキルのみ_年度順にスコア合計を集計() {
            Inventory prevInv = buildInventory(9, prevFy, InventoryStatus.COMPLETED);
            Inventory currentInv = buildInventory(10, currentFy, InventoryStatus.PENDING_GOAL);
            ItSkillDetail prevDetail = ItSkillDetail.create(prevInv, skillA, null, level2, null);
            ItSkillDetail currentDetail = ItSkillDetail.create(currentInv, skillA, null, level4, null);

            when(itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()).thenReturn(List.of(cat1, cat2));
            when(skillLevelRepository.findByActiveOrderByLevelValueAsc(true)).thenReturn(List.of(level2, level4));
            // 順序をあえて逆に返し、サービス側のソートを検証する
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(currentInv, prevInv));
            when(itSkillDetailRepository.findByInventoryId(9)).thenReturn(List.of(prevDetail));
            when(itSkillDetailRepository.findByInventoryId(10)).thenReturn(List.of(currentDetail));

            GrowthQueryResult result = chartService.getGrowth(user);

            assertThat(result.fiscalYears()).containsExactly("2024年度", "2025年度");
            GrowthSeries series = null;
            for (GrowthSeries s : result.series()) {
                if (s.category1Id() == 10) {
                    series = s;
                }
            }
            assertThat(series).isNotNull();
            assertThat(series.yearlyTotalScores()).containsExactly(20, 40);
        }

        @Test
        void 正常系_カスタムスキルが1件以上_カスタムスキル系列が末尾に追加される() {
            Inventory inv = buildInventory(10, currentFy, InventoryStatus.PENDING_GOAL);
            ItSkillDetail customDetail = ItSkillDetail.create(inv, null, "自作フレームワーク", level4, null);

            when(itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()).thenReturn(List.of(cat1, cat2));
            when(skillLevelRepository.findByActiveOrderByLevelValueAsc(true)).thenReturn(List.of(level2, level4));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(inv));
            when(itSkillDetailRepository.findByInventoryId(10)).thenReturn(List.of(customDetail));

            GrowthQueryResult result = chartService.getGrowth(user);

            GrowthSeries customSeries = result.series().get(result.series().size() - 1);
            assertThat(customSeries.category1Id()).isEqualTo(-1);
            assertThat(customSeries.category1Name()).isEqualTo("カスタムスキル");
            assertThat(customSeries.yearlyTotalScores()).containsExactly(40);
        }

        @Test
        void 正常系_カスタムスキルなし_カスタム系列は追加されない() {
            Inventory inv = buildInventory(10, currentFy, InventoryStatus.PENDING_GOAL);
            ItSkillDetail masterDetail = ItSkillDetail.create(inv, skillA, null, level4, null);

            when(itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()).thenReturn(List.of(cat1, cat2));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(inv));
            when(itSkillDetailRepository.findByInventoryId(10)).thenReturn(List.of(masterDetail));

            GrowthQueryResult result = chartService.getGrowth(user);

            for (GrowthSeries s : result.series()) {
                assertThat(s.category1Id()).isNotEqualTo(-1);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  getHeatmap
    // ═══════════════════════════════════════════════════════════

    @Nested
    class GetHeatmap {

        @Test
        void 正常系_当年度なし_スコアなしヒートマップを返す() {
            when(itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()).thenReturn(List.of(cat1, cat2));
            when(skillLevelRepository.findByActiveOrderByLevelValueAsc(true)).thenReturn(List.of(level2, level4));
            when(itSkillRepository.findAllActiveWithCategory()).thenReturn(List.of(skillA));
            when(fiscalYearRepository.findCurrent(any(LocalDate.class))).thenReturn(Optional.empty());

            HeatmapQueryResult result = chartService.getHeatmap(user);

            assertThat(result.currentFiscalYear()).isNull();
            assertThat(result.hasCurrentYearData()).isFalse();
            HeatmapRow row = result.rows().get(0);
            HeatmapCell cell = row.cells().get(0);
            assertThat(cell.scoredSkillCount()).isEqualTo(0);
            assertThat(cell.avgLevelValue()).isNull();
        }

        @Test
        void 正常系_当年度の棚卸なし_年度名ありでスコアなし() {
            when(itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()).thenReturn(List.of(cat1, cat2));
            when(skillLevelRepository.findByActiveOrderByLevelValueAsc(true)).thenReturn(List.of(level2, level4));
            when(itSkillRepository.findAllActiveWithCategory()).thenReturn(List.of(skillA));
            when(fiscalYearRepository.findCurrent(any(LocalDate.class))).thenReturn(Optional.of(currentFy));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of());

            HeatmapQueryResult result = chartService.getHeatmap(user);

            assertThat(result.currentFiscalYear()).isEqualTo("2025年度");
            assertThat(result.hasCurrentYearData()).isFalse();
        }

        @Test
        void 正常系_採点済みスキルあり_平均レベルを算出する() {
            Inventory inv = buildInventory(10, currentFy, InventoryStatus.DRAFT);
            ItSkillDetail detail = ItSkillDetail.create(inv, skillA, null, level4, null);

            when(itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()).thenReturn(List.of(cat1, cat2));
            when(skillLevelRepository.findByActiveOrderByLevelValueAsc(true)).thenReturn(List.of(level2, level4));
            when(itSkillRepository.findAllActiveWithCategory()).thenReturn(List.of(skillA));
            when(fiscalYearRepository.findCurrent(any(LocalDate.class))).thenReturn(Optional.of(currentFy));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(inv));
            when(itSkillDetailRepository.findByInventoryId(10)).thenReturn(List.of(detail));

            HeatmapQueryResult result = chartService.getHeatmap(user);

            assertThat(result.hasCurrentYearData()).isTrue();
            HeatmapCell cell = result.rows().get(0).cells().get(0);
            assertThat(cell.scoredSkillCount()).isEqualTo(1);
            assertThat(cell.avgLevelValue()).isEqualTo(4.0);
        }

        @Test
        void 正常系_無効化スキルでも採点済みなら表示継続する() {
            Inventory inv = buildInventory(10, currentFy, InventoryStatus.DRAFT);
            ItSkill inactiveSkill = ItSkill.create(cat2, "旧スキル", null, 2);
            ReflectionTestUtils.setField(inactiveSkill, "id", 999);
            ItSkillDetail detail = ItSkillDetail.create(inv, inactiveSkill, null, level2, null);

            when(itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()).thenReturn(List.of(cat1, cat2));
            when(skillLevelRepository.findByActiveOrderByLevelValueAsc(true)).thenReturn(List.of(level2, level4));
            when(itSkillRepository.findAllActiveWithCategory()).thenReturn(List.of(skillA));
            when(fiscalYearRepository.findCurrent(any(LocalDate.class))).thenReturn(Optional.of(currentFy));
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(inv));
            when(itSkillDetailRepository.findByInventoryId(10)).thenReturn(List.of(detail));

            HeatmapQueryResult result = chartService.getHeatmap(user);

            HeatmapCell cell = result.rows().get(0).cells().get(0);
            boolean found = false;
            for (var s : cell.skills()) {
                if (s.skillName().equals("旧スキル")) {
                    found = true;
                }
            }
            assertThat(found).isTrue();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  getTimeline
    // ═══════════════════════════════════════════════════════════

    @Nested
    class GetTimeline {

        @Test
        void 正常系_棚卸なし_空イベントを返す() {
            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of());

            TimelineQueryResult result = chartService.getTimeline(user);

            assertThat(result.events()).isEmpty();
        }

        @Test
        void 正常系_資格とセミナー実績_ゴールを時系列イベントとして返す() {
            Inventory inv = buildInventory(10, currentFy, InventoryStatus.COMPLETED);

            Qualification qualification = Qualification.create(null, "基本情報技術者", null, 1);
            QualificationDetail qd = QualificationDetail.create(inv, qualification, null,
                    LocalDate.of(2020, 5, 15), null);
            QualificationDetail customQd = QualificationDetail.create(inv, null, "自作資格",
                    LocalDate.of(2020, 6, 1), null);

            AdSeminar adSeminar = AdSeminar.create(null, "AWS研修", null, 1);
            SeminarDetail sd = SeminarDetail.create(inv, adSeminar, null, null, LocalDate.of(2020, 7, 1), null);
            SeminarDetail freeSd = SeminarDetail.create(inv, null, "社外勉強会", null, LocalDate.of(2020, 8, 1), null);

            InventoryGoal qualGoal = InventoryGoal.create(inv, GoalCategory.QUALIFICATION, null, qualification,
                    null, null, LocalDate.of(2099, 1, 1), null);
            InventoryGoal skillGoal = InventoryGoal.create(inv, GoalCategory.IT_SKILL, skillA, null,
                    null, null, LocalDate.of(2099, 2, 1), null);
            InventoryGoal adGoal = InventoryGoal.create(inv, GoalCategory.AD, null, null,
                    adSeminar, null, LocalDate.of(2099, 3, 1), null);

            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(inv));
            when(qualificationDetailRepository.findByInventoryId(10)).thenReturn(List.of(qd, customQd));
            when(seminarDetailRepository.findByInventoryId(10)).thenReturn(List.of(sd, freeSd));
            when(inventoryGoalRepository.findByInventoryId(10)).thenReturn(List.of(qualGoal, skillGoal, adGoal));

            TimelineQueryResult result = chartService.getTimeline(user);

            assertThat(result.events()).hasSize(7);
            boolean hasCustomQual = false;
            boolean hasFreeSeminar = false;
            boolean hasSkillGoal = false;
            boolean hasAdGoal = false;
            for (TimelineEvent e : result.events()) {
                if (e.name().equals("自作資格")) hasCustomQual = true;
                if (e.name().equals("社外勉強会")) hasFreeSeminar = true;
                if (e.type().equals("GOAL_IT_SKILL")) {
                    hasSkillGoal = true;
                    assertThat(e.lane()).isEqualTo("ACTIVITY");
                }
                if (e.type().equals("GOAL_AD")) {
                    hasAdGoal = true;
                    assertThat(e.lane()).isEqualTo("ACTIVITY");
                }
            }
            assertThat(hasCustomQual).isTrue();
            assertThat(hasFreeSeminar).isTrue();
            assertThat(hasSkillGoal).isTrue();
            assertThat(hasAdGoal).isTrue();
        }

        @Test
        void 正常系_複数棚卸_最新年度のみ実績ソースとして扱う() {
            Inventory oldInv = buildInventory(9, prevFy, InventoryStatus.COMPLETED);
            Inventory newInv = buildInventory(10, currentFy, InventoryStatus.PENDING_GOAL);

            Qualification qualification = Qualification.create(null, "新資格", null, 1);
            QualificationDetail oldQd = QualificationDetail.create(oldInv, qualification, null,
                    LocalDate.of(2019, 1, 1), null);

            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(oldInv, newInv));
            when(qualificationDetailRepository.findByInventoryId(10)).thenReturn(List.of());
            when(seminarDetailRepository.findByInventoryId(10)).thenReturn(List.of());
            when(inventoryGoalRepository.findByInventoryId(10)).thenReturn(List.of());

            TimelineQueryResult result = chartService.getTimeline(user);

            assertThat(result.events()).isEmpty();
            verify(qualificationDetailRepository, never()).findByInventoryId(9);
        }

        @Test
        void 異常系_取得年月nullの資格明細は無視される() {
            Inventory inv = buildInventory(10, currentFy, InventoryStatus.COMPLETED);
            Qualification qualification = Qualification.create(null, "資格A", null, 1);
            QualificationDetail qdNoDate = QualificationDetail.create(inv, qualification, null, null, null);

            when(inventoryRepository.findByUserIdWithFiscalYear(1)).thenReturn(List.of(inv));
            when(qualificationDetailRepository.findByInventoryId(10)).thenReturn(List.of(qdNoDate));
            when(seminarDetailRepository.findByInventoryId(10)).thenReturn(List.of());
            when(inventoryGoalRepository.findByInventoryId(10)).thenReturn(List.of());

            TimelineQueryResult result = chartService.getTimeline(user);

            assertThat(result.events()).isEmpty();
        }
    }
}

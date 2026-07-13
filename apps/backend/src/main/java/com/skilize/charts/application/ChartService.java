/**************************************************************************************************************
 * 機能ID      ：CHT
 * 機能名      ：グラフ・チャート
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * スキル統計グラフの集計サービス。レーダーチャート・成長推移・ヒートマップ・タイムラインの
 * 各グラフデータをユーザーの棚卸データから集計して返す。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.charts.application;

import com.skilize.charts.application.query.*;
import com.skilize.charts.application.query.GrowthQueryResult.GrowthSeries;
import com.skilize.charts.application.query.HeatmapQueryResult.HeatmapCell;
import com.skilize.charts.application.query.HeatmapQueryResult.HeatmapRow;
import com.skilize.charts.application.query.HeatmapQueryResult.HeatmapSkill;
import com.skilize.charts.application.query.RadarQueryResult.RadarAxis;
import com.skilize.charts.application.query.TimelineQueryResult.TimelineEvent;
import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.fiscalyear.domain.repository.FiscalYearRepository;
import com.skilize.inventory.domain.model.*;
import com.skilize.inventory.domain.repository.*;
import com.skilize.master.domain.model.*;
import com.skilize.master.domain.repository.*;
import com.skilize.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * スキル統計グラフ（レーダー・成長推移・ヒートマップ・タイムライン）の集計ロジック。
 * ITスキルカテゴリは最大3階層のツリー構造を持ち、resolveAncestorById で第1/第2レベルへ遡上する。
 * グラフデータの集計はすべて読み取り専用トランザクション内で実行する。
 */
@Service
@RequiredArgsConstructor
public class ChartService {

    private final FiscalYearRepository fiscalYearRepository;
    private final InventoryRepository inventoryRepository;
    private final ItSkillDetailRepository itSkillDetailRepository;
    private final QualificationDetailRepository qualificationDetailRepository;
    private final SeminarDetailRepository seminarDetailRepository;
    private final InventoryGoalRepository inventoryGoalRepository;
    private final ItSkillCategoryRepository itSkillCategoryRepository;
    private final ItSkillRepository itSkillRepository;
    private final SkillLevelRepository skillLevelRepository;

    // ===== Radar =====

    /**
     * レーダーチャートデータを返す。ITスキル大分類ごとに今年度・前年度の平均スコアを集計する。
     * 今年度の有効年度が存在しない場合はスコアがすべて 0.0 の軸データを返す。
     */
    @Transactional(readOnly = true)
    public RadarQueryResult getRadar(User user) {
        Map<Integer, ItSkillCategory> catMap = buildCategoryMap();
        List<ItSkillCategory> cat1List = getActiveCat1List(catMap);
        int maxLevelValue = getMaxLevelValue();

        Optional<FiscalYear> currentFyOptional = fiscalYearRepository.findCurrent(LocalDate.now());
        if (currentFyOptional.isEmpty()) {
            List<RadarAxis> axes = new ArrayList<>();
            for (ItSkillCategory c : cat1List) {
                axes.add(new RadarAxis(c.getId(), c.getName(), 0.0, null));
            }
            return new RadarQueryResult(null, null, false, maxLevelValue, axes);
        }
        FiscalYear currentFy = currentFyOptional.get();

        List<Inventory> inventories = inventoryRepository.findByUserIdWithFiscalYear(user.getId());
        Inventory currentInv = null;
        for (Inventory i : inventories) {
            if (i.getFiscalYear().getId().equals(currentFy.getId())) {
                currentInv = i;
                break;
            }
        }

        Inventory prevInv = null;
        if (currentInv != null) {
            for (Inventory i : inventories) {
                if (i.getId().equals(currentInv.getId())) {
                    continue;
                }
                if (i.getFiscalYear().getEndDate().isBefore(currentFy.getStartDate())) {
                    prevInv = i;
                    break;
                }
            }
        }

        // cat1（大分類）ごとにスコア重みを集約し、後で平均を計算する。
        // cat1Id → scored scoreWeights
        Map<Integer, List<Integer>> currentByCat1 = new HashMap<>();
        boolean hasCurrentYearData = false;
        if (currentInv != null) {
            for (ItSkillDetail d : itSkillDetailRepository.findByInventoryId(currentInv.getId())) {
                if (d.getItSkill() == null) continue;
                ItSkillCategory cat1 = resolveAncestorById(d.getItSkill().getCategory().getId(), catMap, (short) 1);
                if (cat1 == null) continue;
                getOrCreateList(currentByCat1, cat1.getId()).add(d.getSkillLevel().getScoreWeight());
                hasCurrentYearData = true;
            }
        }

        Map<Integer, List<Integer>> prevByCat1 = new HashMap<>();
        if (prevInv != null) {
            for (ItSkillDetail d : itSkillDetailRepository.findByInventoryId(prevInv.getId())) {
                if (d.getItSkill() == null) continue;
                ItSkillCategory cat1 = resolveAncestorById(d.getItSkill().getCategory().getId(), catMap, (short) 1);
                if (cat1 == null) continue;
                getOrCreateList(prevByCat1, cat1.getId()).add(d.getSkillLevel().getScoreWeight());
            }
        }

        boolean hasPrevInv = prevInv != null;
        List<RadarAxis> axes = new ArrayList<>();
        for (ItSkillCategory cat1 : cat1List) {
            List<Integer> cs = currentByCat1.getOrDefault(cat1.getId(), List.of());
            List<Integer> ps = prevByCat1.getOrDefault(cat1.getId(), List.of());
            double currentAvg = 0.0;
            if (!cs.isEmpty()) {
                currentAvg = round1(averageInt(cs));
            }
            Double prevAvg = null;
            if (hasPrevInv && !ps.isEmpty()) {
                prevAvg = round1(averageInt(ps));
            }
            axes.add(new RadarAxis(cat1.getId(), cat1.getName(), currentAvg, prevAvg));
        }

        String prevFyName = null;
        if (prevInv != null) {
            prevFyName = prevInv.getFiscalYear().getName();
        }
        return new RadarQueryResult(currentFy.getName(), prevFyName, hasCurrentYearData, getMaxScoreWeight(), axes);
    }

    // ===== Growth =====

    /**
     * 成長推移グラフデータを返す。提出済み棚卸を年度順に並べ、大分類ごとのスコア合計を集計する。
     * カスタムスキルは大分類に属さないため、別途 customSeriesId（-1）の系列として末尾に追加する。
     */
    @Transactional(readOnly = true)
    public GrowthQueryResult getGrowth(User user) {
        Map<Integer, ItSkillCategory> catMap = buildCategoryMap();
        List<ItSkillCategory> cat1List = getActiveCat1List(catMap);

        List<Inventory> submitted = new ArrayList<>();
        for (Inventory i : inventoryRepository.findByUserIdWithFiscalYear(user.getId())) {
            if (i.getStatus() == InventoryStatus.PENDING_GOAL || i.getStatus() == InventoryStatus.COMPLETED) {
                submitted.add(i);
            }
        }
        submitted.sort(new Comparator<Inventory>() {
            @Override
            public int compare(Inventory a, Inventory b) {
                return a.getFiscalYear().getStartDate().compareTo(b.getFiscalYear().getStartDate());
            }
        });

        if (submitted.isEmpty()) {
            List<GrowthSeries> series = new ArrayList<>();
            for (ItSkillCategory c : cat1List) {
                series.add(new GrowthSeries(c.getId(), c.getName(), List.of()));
            }
            return new GrowthQueryResult(List.of(), series);
        }

        // カスタムスキルは最大重みで集計するため取得しておく
        int maxWeight = getMaxScoreWeight();
        // カスタムスキル用の仮 ID（マスタ cat1 と衝突しない負値）
        int customSeriesId = -1;

        // invId → (cat1Id | customSeriesId) → total score
        Map<Integer, Map<Integer, Integer>> scoreMap = new HashMap<>();
        for (Inventory inv : submitted) {
            Map<Integer, Integer> cat1Score = new HashMap<>();
            for (ItSkillDetail d : itSkillDetailRepository.findByInventoryId(inv.getId())) {
                if (d.getItSkill() == null) {
                    // カスタムスキル: スコアへの寄与を最大重みで扱う
                    addToMap(cat1Score, customSeriesId, maxWeight);
                    continue;
                }
                ItSkillCategory cat1 = resolveAncestorById(d.getItSkill().getCategory().getId(), catMap, (short) 1);
                if (cat1 == null) continue;
                addToMap(cat1Score, cat1.getId(), d.getSkillLevel().getScoreWeight());
            }
            scoreMap.put(inv.getId(), cat1Score);
        }

        List<String> fiscalYears = new ArrayList<>();
        for (Inventory i : submitted) {
            fiscalYears.add(i.getFiscalYear().getName());
        }

        List<GrowthSeries> series = new ArrayList<>();
        for (ItSkillCategory cat1 : cat1List) {
            List<Integer> scores = new ArrayList<>();
            for (Inventory inv : submitted) {
                scores.add(scoreMap.getOrDefault(inv.getId(), Map.of()).getOrDefault(cat1.getId(), 0));
            }
            series.add(new GrowthSeries(cat1.getId(), cat1.getName(), scores));
        }

        // カスタムスキルが1件以上あれば末尾に追加
        List<Integer> customScores = new ArrayList<>();
        for (Inventory inv : submitted) {
            customScores.add(scoreMap.getOrDefault(inv.getId(), Map.of()).getOrDefault(customSeriesId, 0));
        }
        boolean hasCustomScore = false;
        for (Integer s : customScores) {
            if (s > 0) {
                hasCustomScore = true;
                break;
            }
        }
        if (hasCustomScore) {
            series.add(new GrowthSeries(customSeriesId, "カスタムスキル", customScores));
        }

        return new GrowthQueryResult(fiscalYears, series);
    }

    // ===== Heatmap =====

    /**
     * ヒートマップデータを返す。大分類 → 中分類 → スキルの階層構造で今年度のスキルレベルを表示する。
     * 無効化されたスキルでも採点済みの場合はヒートマップに表示する（過去棚卸との表示継続性を保持）。
     */
    @Transactional(readOnly = true)
    public HeatmapQueryResult getHeatmap(User user) {
        Map<Integer, ItSkillCategory> catMap = buildCategoryMap();
        List<ItSkillCategory> cat1List = getActiveCat1List(catMap);
        int maxLevelValue = getMaxLevelValue();
        List<ItSkill> activeSkills = itSkillRepository.findAllActiveWithCategory();

        Optional<FiscalYear> currentFyOptional = fiscalYearRepository.findCurrent(LocalDate.now());
        if (currentFyOptional.isEmpty()) {
            return buildHeatmapNoScores(null, cat1List, catMap, activeSkills, maxLevelValue);
        }
        FiscalYear currentFy = currentFyOptional.get();

        Inventory currentInv = null;
        for (Inventory i : inventoryRepository.findByUserIdWithFiscalYear(user.getId())) {
            if (i.getFiscalYear().getId().equals(currentFy.getId())) {
                currentInv = i;
                break;
            }
        }

        if (currentInv == null) {
            return buildHeatmapNoScores(currentFy, cat1List, catMap, activeSkills, maxLevelValue);
        }

        List<ItSkillDetail> details = itSkillDetailRepository.findByInventoryId(currentInv.getId());
        Map<Integer, Short> scoredBySkillId = new HashMap<>();
        for (ItSkillDetail d : details) {
            if (d.getItSkill() != null) {
                scoredBySkillId.put(d.getItSkill().getId(), d.getSkillLevel().getLevelValue());
            }
        }

        Set<Integer> activeSkillIds = new HashSet<>();
        for (ItSkill skill : activeSkills) {
            activeSkillIds.add(skill.getId());
        }

        // LinkedHashMap: 挿入順を保持する HashMap。cat1 の表示順を維持するために使用する。
        // 通常の HashMap はキーの順序を保証しない。
        // cat1Id → cat2Key → skill entries (LinkedHashMap preserves insertion order)
        Map<Integer, LinkedHashMap<Cat2Key, List<SkillEntry>>> structure = initStructure(cat1List);

        // Add active skills (with or without user score)
        for (ItSkill skill : activeSkills) {
            ItSkillCategory cat1 = resolveAncestorById(skill.getCategory().getId(), catMap, (short) 1);
            if (cat1 == null || !structure.containsKey(cat1.getId())) continue;
            ItSkillCategory cat2 = resolveAncestorById(skill.getCategory().getId(), catMap, (short) 2);
            Cat2Key cat2Key = toCat2Key(cat2);
            Short levelValue = scoredBySkillId.get(skill.getId());
            getOrCreateList(structure.get(cat1.getId()), cat2Key).add(new SkillEntry(skill.getName(), levelValue));
        }

        // 無効化済みスキル（is_active=false）でもユーザーが採点している場合はヒートマップに表示する。
        // 過去に登録されたスキルが後からマスタ削除された場合の表示継続を保証する。
        for (ItSkillDetail d : details) {
            if (d.getItSkill() == null || activeSkillIds.contains(d.getItSkill().getId())) continue;
            var skill = d.getItSkill();
            ItSkillCategory cat1 = resolveAncestorById(skill.getCategory().getId(), catMap, (short) 1);
            if (cat1 == null || !structure.containsKey(cat1.getId())) continue;
            ItSkillCategory cat2 = resolveAncestorById(skill.getCategory().getId(), catMap, (short) 2);
            Cat2Key cat2Key = toCat2Key(cat2);
            getOrCreateList(structure.get(cat1.getId()), cat2Key)
                    .add(new SkillEntry(skill.getName(), d.getSkillLevel().getLevelValue()));
        }

        boolean hasCurrentYearData = !scoredBySkillId.isEmpty();
        List<HeatmapRow> rows = buildHeatmapRows(cat1List, structure);
        return new HeatmapQueryResult(currentFy.getName(), hasCurrentYearData, maxLevelValue, rows);
    }

    /** スコアなし（棚卸未作成・採点なし）時のヒートマップを構築する。全スキルを levelValue=null でセルに追加する。 */
    private HeatmapQueryResult buildHeatmapNoScores(FiscalYear currentFy,
                                                     List<ItSkillCategory> cat1List,
                                                     Map<Integer, ItSkillCategory> catMap,
                                                     List<ItSkill> activeSkills,
                                                     int maxLevelValue) {
        Map<Integer, LinkedHashMap<Cat2Key, List<SkillEntry>>> structure = initStructure(cat1List);
        for (ItSkill skill : activeSkills) {
            ItSkillCategory cat1 = resolveAncestorById(skill.getCategory().getId(), catMap, (short) 1);
            if (cat1 == null || !structure.containsKey(cat1.getId())) continue;
            ItSkillCategory cat2 = resolveAncestorById(skill.getCategory().getId(), catMap, (short) 2);
            Cat2Key cat2Key = toCat2Key(cat2);
            getOrCreateList(structure.get(cat1.getId()), cat2Key).add(new SkillEntry(skill.getName(), null));
        }
        List<HeatmapRow> rows = buildHeatmapRows(cat1List, structure);
        String fyName = null;
        if (currentFy != null) {
            fyName = currentFy.getName();
        }
        return new HeatmapQueryResult(fyName, false, maxLevelValue, rows);
    }

    /** 中分類カテゴリから Cat2Key を組み立てる。中分類が存在しない場合は「(分類なし)」を返す。 */
    private Cat2Key toCat2Key(ItSkillCategory cat2) {
        if (cat2 != null) {
            return new Cat2Key(cat2.getId(), cat2.getName());
        }
        return new Cat2Key(null, "(分類なし)");
    }

    /** ヒートマップの大分類→中分類→スキルエントリの入れ子マップ構造を初期化する。挿入順保持のため LinkedHashMap を使用する。 */
    private Map<Integer, LinkedHashMap<Cat2Key, List<SkillEntry>>> initStructure(List<ItSkillCategory> cat1List) {
        Map<Integer, LinkedHashMap<Cat2Key, List<SkillEntry>>> structure = new LinkedHashMap<>();
        for (ItSkillCategory cat1 : cat1List) {
            structure.put(cat1.getId(), new LinkedHashMap<>());
        }
        return structure;
    }

    /** ヒートマップ表示用の行リストを構築する。大分類→中分類→スキルの階層で平均レベルを集計する。 */
    private List<HeatmapRow> buildHeatmapRows(List<ItSkillCategory> cat1List,
                                               Map<Integer, LinkedHashMap<Cat2Key, List<SkillEntry>>> structure) {
        List<HeatmapRow> rows = new ArrayList<>();
        for (ItSkillCategory cat1 : cat1List) {
            LinkedHashMap<Cat2Key, List<SkillEntry>> cellMap = structure.get(cat1.getId());
            List<HeatmapCell> cells = new ArrayList<>();
            for (Map.Entry<Cat2Key, List<SkillEntry>> e : cellMap.entrySet()) {
                Cat2Key cat2Key = e.getKey();
                List<SkillEntry> entries = e.getValue();
                List<Short> scored = new ArrayList<>();
                for (SkillEntry entry : entries) {
                    if (entry.levelValue() != null) {
                        scored.add(entry.levelValue());
                    }
                }
                Double avgLevelValue = null;
                if (!scored.isEmpty()) {
                    avgLevelValue = round1(average(scored));
                }
                List<HeatmapSkill> skills = new ArrayList<>();
                for (SkillEntry entry : entries) {
                    Integer levelValue = null;
                    if (entry.levelValue() != null) {
                        levelValue = entry.levelValue().intValue();
                    }
                    skills.add(new HeatmapSkill(entry.skillName(), levelValue));
                }
                cells.add(new HeatmapCell(cat2Key.id(), cat2Key.name(), avgLevelValue, scored.size(), skills));
            }
            rows.add(new HeatmapRow(cat1.getId(), cat1.getName(), cells));
        }
        return rows;
    }

    // ===== Timeline =====

    /**
     * タイムラインデータを返す。資格・セミナー実績と目標の時系列イベント一覧を返す。
     * 実績は最新の提出済み棚卸1件のみを参照し（年度間の重複登録を避けるため）、目標は最新棚卸から取得する。
     */
    @Transactional(readOnly = true)
    public TimelineQueryResult getTimeline(User user) {
        List<Inventory> inventories = inventoryRepository.findByUserIdWithFiscalYear(user.getId());
        List<TimelineEvent> events = new ArrayList<>();
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);

        // 資格・セミナー実績は複数年度の棚卸に同じデータが重複して登録される場合があるため、
        // 最新の提出済み棚卸 1 件のみを実績ソースとして扱う
        Inventory achievementSource = null;
        for (Inventory i : inventories) {
            if (i.getStatus() != InventoryStatus.PENDING_GOAL && i.getStatus() != InventoryStatus.COMPLETED) {
                continue;
            }
            if (achievementSource == null || i.getFiscalYear().getId() > achievementSource.getFiscalYear().getId()) {
                achievementSource = i;
            }
        }

        if (achievementSource != null) {
            for (QualificationDetail qd : qualificationDetailRepository.findByInventoryId(achievementSource.getId())) {
                if (qd.getAcquiredYearMonth() == null) continue;
                String name;
                if (qd.getQualification() != null) {
                    name = qd.getQualification().getName();
                } else {
                    name = qd.getCustomQualificationName();
                }
                LocalDate ym = qd.getAcquiredYearMonth().withDayOfMonth(1);
                events.add(new TimelineEvent("QUALIFICATION", "ACHIEVEMENT", name,
                        ym.toString(), ym.isBefore(firstOfMonth)));
            }
            for (SeminarDetail sd : seminarDetailRepository.findByInventoryId(achievementSource.getId())) {
                if (sd.getAttendedYearMonth() == null) continue;
                LocalDate ym = sd.getAttendedYearMonth().withDayOfMonth(1);
                if (sd.getAdSeminar() != null) {
                    events.add(new TimelineEvent("AD_SEMINAR", "ACTIVITY", sd.getAdSeminar().getName(),
                            ym.toString(), ym.isBefore(firstOfMonth)));
                } else if (sd.getSeminarName() != null) {
                    events.add(new TimelineEvent("FREE_SEMINAR", "ACTIVITY", sd.getSeminarName(),
                            ym.toString(), ym.isBefore(firstOfMonth)));
                }
            }
        }

        Inventory latestInv = null;
        for (Inventory i : inventories) {
            if (latestInv == null || i.getFiscalYear().getId() > latestInv.getFiscalYear().getId()) {
                latestInv = i;
            }
        }
        if (latestInv != null) {
            for (InventoryGoal goal : inventoryGoalRepository.findByInventoryId(latestInv.getId())) {
                LocalDate ym = goal.getTargetPeriod().withDayOfMonth(1);
                String type;
                switch (goal.getGoalCategory()) {
                    case QUALIFICATION:
                        type = "GOAL_QUALIFICATION";
                        break;
                    case IT_SKILL:
                        type = "GOAL_IT_SKILL";
                        break;
                    case AD:
                        type = "GOAL_AD";
                        break;
                    default:
                        throw new IllegalStateException("Unexpected goal category: " + goal.getGoalCategory());
                }
                String lane;
                if (goal.getGoalCategory() == GoalCategory.QUALIFICATION) {
                    lane = "ACHIEVEMENT";
                } else {
                    lane = "ACTIVITY";
                }
                String name = resolveGoalName(goal);
                events.add(new TimelineEvent(type, lane, name, ym.toString(), ym.isBefore(firstOfMonth)));
            }
        }

        events.sort(new Comparator<TimelineEvent>() {
            @Override
            public int compare(TimelineEvent a, TimelineEvent b) {
                return a.yearMonth().compareTo(b.yearMonth());
            }
        });
        return new TimelineQueryResult(events);
    }

    // ===== Helpers =====

    /** 全ITスキル分類を ID→エンティティ のマップで返す。ツリー遡上時に高速アクセスするために使用する。 */
    private Map<Integer, ItSkillCategory> buildCategoryMap() {
        Map<Integer, ItSkillCategory> map = new HashMap<>();
        for (ItSkillCategory c : itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()) {
            map.put(c.getId(), c);
        }
        return map;
    }

    /** 全分類マップからアクティブなレベル1（大分類）を sortOrder 昇順で返す。 */
    private List<ItSkillCategory> getActiveCat1List(Map<Integer, ItSkillCategory> catMap) {
        List<ItSkillCategory> result = new ArrayList<>();
        for (ItSkillCategory c : catMap.values()) {
            if (c.getLevel() == 1 && c.isActive()) {
                result.add(c);
            }
        }
        result.sort(new Comparator<ItSkillCategory>() {
            @Override
            public int compare(ItSkillCategory a, ItSkillCategory b) {
                return Integer.compare(a.getSortOrder(), b.getSortOrder());
            }
        });
        return result;
    }

    /** アクティブなスキルレベルの最大 levelValue を返す。スキルレベル未設定の場合は 5 を返す。 */
    private int getMaxLevelValue() {
        int max = 5;
        boolean found = false;
        for (SkillLevel s : skillLevelRepository.findByActiveOrderByLevelValueAsc(true)) {
            int value = s.getLevelValue();
            if (!found || value > max) {
                max = value;
                found = true;
            }
        }
        return max;
    }

    /** アクティブなスキルレベルの最大 scoreWeight を返す。スキルレベル未設定の場合は 4 を返す。 */
    private int getMaxScoreWeight() {
        int max = 4;
        boolean found = false;
        for (SkillLevel s : skillLevelRepository.findByActiveOrderByLevelValueAsc(true)) {
            int value = s.getScoreWeight();
            if (!found || value > max) {
                max = value;
                found = true;
            }
        }
        return max;
    }

    // カテゴリツリーを leaf から上方向にたどり targetLevel の祖先カテゴリを返す
    // 最大5回でガードするのは循環参照や想定外の深い階層への対策
    private ItSkillCategory resolveAncestorById(int leafCategoryId,
                                                  Map<Integer, ItSkillCategory> catMap,
                                                  short targetLevel) {
        ItSkillCategory current = catMap.get(leafCategoryId);
        for (int guard = 0; guard < 5; guard++) {
            if (current == null) return null;
            if (current.getLevel() == targetLevel) return current;
            if (current.getLevel() < targetLevel) return null;
            if (current.getParentId() == null) return null;
            current = catMap.get(current.getParentId());
        }
        return null;
    }

    /** Short リストの平均値を計算する。 */
    private double average(List<Short> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        int sum = 0;
        for (Short v : values) {
            sum += v;
        }
        return (double) sum / values.size();
    }

    /** Integer リストの平均値を計算する。 */
    private double averageInt(List<Integer> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        int sum = 0;
        for (Integer v : values) {
            sum += v;
        }
        return (double) sum / values.size();
    }

    // 小数第1位に丸める。Math.round は最近接偶数丸めではなく四捨五入（0.5 → 切り上げ）。
    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    /** 目標名称を参照先の優先順位（ITスキル → 資格 → ADセミナー → カスタム名）で解決して返す。 */
    private String resolveGoalName(InventoryGoal g) {
        if (g.getItSkill() != null) return g.getItSkill().getName();
        if (g.getQualification() != null) return g.getQualification().getName();
        if (g.getAdSeminar() != null) return g.getAdSeminar().getName();
        return g.getCustomName();
    }

    /** マップにキーが存在しなければ空リストを作成して登録し、そのリストを返す（computeIfAbsentの明示版）。 */
    private <K, V> List<V> getOrCreateList(Map<K, List<V>> map, K key) {
        List<V> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        return list;
    }

    /** マップの値に加算する（キーが存在しなければ新規登録する。Map.mergeの明示版）。 */
    private void addToMap(Map<Integer, Integer> map, int key, int amount) {
        Integer current = map.get(key);
        if (current == null) {
            map.put(key, amount);
        } else {
            map.put(key, current + amount);
        }
    }

    // Internal value objects
    private record Cat2Key(Integer id, String name) {}

    private record SkillEntry(String skillName, Short levelValue) {}
}

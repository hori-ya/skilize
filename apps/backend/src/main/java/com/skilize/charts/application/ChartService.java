package com.skilize.charts.application;

import com.skilize.fiscalyear.domain.FiscalYear;
import com.skilize.fiscalyear.domain.FiscalYearRepository;
import com.skilize.inventory.domain.*;
import com.skilize.master.domain.*;
import com.skilize.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public RadarResponse getRadar(User user) {
        Map<Integer, ItSkillCategory> catMap = buildCategoryMap();
        List<ItSkillCategory> cat1List = getActiveCat1List(catMap);
        int maxLevelValue = getMaxLevelValue();

        FiscalYear currentFy = fiscalYearRepository.findCurrent(LocalDate.now()).orElse(null);
        if (currentFy == null) {
            List<RadarAxis> axes = cat1List.stream()
                    .map(c -> new RadarAxis(c.getId(), c.getName(), 0.0, null))
                    .toList();
            return new RadarResponse(null, null, false, maxLevelValue, axes);
        }

        List<Inventory> inventories = inventoryRepository.findByUserIdWithFiscalYear(user.getId());
        Inventory currentInv = inventories.stream()
                .filter(i -> i.getFiscalYear().getId().equals(currentFy.getId()))
                .findFirst().orElse(null);

        Inventory prevInv = currentInv != null
                ? inventories.stream()
                        .filter(i -> !i.getId().equals(currentInv.getId()))
                        .filter(i -> i.getFiscalYear().getEndDate().isBefore(currentFy.getStartDate()))
                        .findFirst().orElse(null)
                : null;

        // cat1Id → scored levelValues
        Map<Integer, List<Short>> currentByCat1 = new HashMap<>();
        boolean hasCurrentYearData = false;
        if (currentInv != null) {
            for (ItSkillDetail d : itSkillDetailRepository.findByInventoryId(currentInv.getId())) {
                if (d.getItSkill() == null) continue;
                ItSkillCategory cat1 = resolveAncestorById(d.getItSkill().getCategory().getId(), catMap, (short) 1);
                if (cat1 == null) continue;
                currentByCat1.computeIfAbsent(cat1.getId(), k -> new ArrayList<>())
                        .add(d.getSkillLevel().getLevelValue());
                hasCurrentYearData = true;
            }
        }

        Map<Integer, List<Short>> prevByCat1 = new HashMap<>();
        if (prevInv != null) {
            for (ItSkillDetail d : itSkillDetailRepository.findByInventoryId(prevInv.getId())) {
                if (d.getItSkill() == null) continue;
                ItSkillCategory cat1 = resolveAncestorById(d.getItSkill().getCategory().getId(), catMap, (short) 1);
                if (cat1 == null) continue;
                prevByCat1.computeIfAbsent(cat1.getId(), k -> new ArrayList<>())
                        .add(d.getSkillLevel().getLevelValue());
            }
        }

        final boolean hasPrevInv = prevInv != null;
        List<RadarAxis> axes = cat1List.stream().map(cat1 -> {
            List<Short> cs = currentByCat1.getOrDefault(cat1.getId(), List.of());
            List<Short> ps = prevByCat1.getOrDefault(cat1.getId(), List.of());
            double currentAvg = cs.isEmpty() ? 0.0 : round1(average(cs));
            Double prevAvg = hasPrevInv ? (ps.isEmpty() ? null : round1(average(ps))) : null;
            return new RadarAxis(cat1.getId(), cat1.getName(), currentAvg, prevAvg);
        }).toList();

        String prevFyName = prevInv != null ? prevInv.getFiscalYear().getName() : null;
        return new RadarResponse(currentFy.getName(), prevFyName, hasCurrentYearData, maxLevelValue, axes);
    }

    // ===== Growth =====

    @Transactional(readOnly = true)
    public GrowthResponse getGrowth(User user) {
        Map<Integer, ItSkillCategory> catMap = buildCategoryMap();
        List<ItSkillCategory> cat1List = getActiveCat1List(catMap);

        List<Inventory> submitted = inventoryRepository.findByUserIdWithFiscalYear(user.getId()).stream()
                .filter(i -> i.getStatus() == InventoryStatus.PENDING_GOAL
                        || i.getStatus() == InventoryStatus.COMPLETED)
                .sorted(Comparator.comparing(i -> i.getFiscalYear().getStartDate()))
                .toList();

        if (submitted.isEmpty()) {
            List<GrowthSeries> series = cat1List.stream()
                    .map(c -> new GrowthSeries(c.getId(), c.getName(), List.of()))
                    .toList();
            return new GrowthResponse(List.of(), series);
        }

        // invId → cat1Id → total score
        Map<Integer, Map<Integer, Integer>> scoreMap = new HashMap<>();
        for (Inventory inv : submitted) {
            Map<Integer, Integer> cat1Score = new HashMap<>();
            for (ItSkillDetail d : itSkillDetailRepository.findByInventoryId(inv.getId())) {
                if (d.getItSkill() == null) continue;
                ItSkillCategory cat1 = resolveAncestorById(d.getItSkill().getCategory().getId(), catMap, (short) 1);
                if (cat1 == null) continue;
                cat1Score.merge(cat1.getId(), (int) d.getSkillLevel().getLevelValue(), Integer::sum);
            }
            scoreMap.put(inv.getId(), cat1Score);
        }

        List<String> fiscalYears = submitted.stream().map(i -> i.getFiscalYear().getName()).toList();
        List<GrowthSeries> series = cat1List.stream().map(cat1 -> {
            List<Integer> scores = submitted.stream()
                    .map(inv -> scoreMap.getOrDefault(inv.getId(), Map.of()).getOrDefault(cat1.getId(), 0))
                    .toList();
            return new GrowthSeries(cat1.getId(), cat1.getName(), scores);
        }).toList();

        return new GrowthResponse(fiscalYears, series);
    }

    // ===== Heatmap =====

    @Transactional(readOnly = true)
    public HeatmapResponse getHeatmap(User user) {
        Map<Integer, ItSkillCategory> catMap = buildCategoryMap();
        List<ItSkillCategory> cat1List = getActiveCat1List(catMap);
        int maxLevelValue = getMaxLevelValue();
        List<ItSkill> activeSkills = itSkillRepository.findAllActiveWithCategory();

        FiscalYear currentFy = fiscalYearRepository.findCurrent(LocalDate.now()).orElse(null);
        if (currentFy == null) {
            return buildHeatmapNoScores(currentFy, cat1List, catMap, activeSkills, maxLevelValue);
        }

        Inventory currentInv = inventoryRepository.findByUserIdWithFiscalYear(user.getId()).stream()
                .filter(i -> i.getFiscalYear().getId().equals(currentFy.getId()))
                .findFirst().orElse(null);

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

        Set<Integer> activeSkillIds = activeSkills.stream().map(ItSkill::getId).collect(Collectors.toSet());

        // cat1Id → cat2Key → skill entries (LinkedHashMap preserves insertion order)
        Map<Integer, LinkedHashMap<Cat2Key, List<SkillEntry>>> structure = initStructure(cat1List);

        // Add active skills (with or without user score)
        for (ItSkill skill : activeSkills) {
            ItSkillCategory cat1 = resolveAncestorById(skill.getCategory().getId(), catMap, (short) 1);
            if (cat1 == null || !structure.containsKey(cat1.getId())) continue;
            ItSkillCategory cat2 = resolveAncestorById(skill.getCategory().getId(), catMap, (short) 2);
            Cat2Key cat2Key = cat2 != null ? new Cat2Key(cat2.getId(), cat2.getName())
                    : new Cat2Key(null, "(分類なし)");
            Short levelValue = scoredBySkillId.get(skill.getId());
            structure.get(cat1.getId())
                    .computeIfAbsent(cat2Key, k -> new ArrayList<>())
                    .add(new SkillEntry(skill.getName(), levelValue));
        }

        // Add inactive skills that were scored (not in activeSkills)
        for (ItSkillDetail d : details) {
            if (d.getItSkill() == null || activeSkillIds.contains(d.getItSkill().getId())) continue;
            ItSkill skill = d.getItSkill();
            ItSkillCategory cat1 = resolveAncestorById(skill.getCategory().getId(), catMap, (short) 1);
            if (cat1 == null || !structure.containsKey(cat1.getId())) continue;
            ItSkillCategory cat2 = resolveAncestorById(skill.getCategory().getId(), catMap, (short) 2);
            Cat2Key cat2Key = cat2 != null ? new Cat2Key(cat2.getId(), cat2.getName())
                    : new Cat2Key(null, "(分類なし)");
            structure.get(cat1.getId())
                    .computeIfAbsent(cat2Key, k -> new ArrayList<>())
                    .add(new SkillEntry(skill.getName(), d.getSkillLevel().getLevelValue()));
        }

        boolean hasCurrentYearData = !scoredBySkillId.isEmpty();
        List<HeatmapRow> rows = buildHeatmapRows(cat1List, structure);
        return new HeatmapResponse(currentFy.getName(), hasCurrentYearData, maxLevelValue, rows);
    }

    private HeatmapResponse buildHeatmapNoScores(FiscalYear currentFy,
                                                   List<ItSkillCategory> cat1List,
                                                   Map<Integer, ItSkillCategory> catMap,
                                                   List<ItSkill> activeSkills,
                                                   int maxLevelValue) {
        Map<Integer, LinkedHashMap<Cat2Key, List<SkillEntry>>> structure = initStructure(cat1List);
        for (ItSkill skill : activeSkills) {
            ItSkillCategory cat1 = resolveAncestorById(skill.getCategory().getId(), catMap, (short) 1);
            if (cat1 == null || !structure.containsKey(cat1.getId())) continue;
            ItSkillCategory cat2 = resolveAncestorById(skill.getCategory().getId(), catMap, (short) 2);
            Cat2Key cat2Key = cat2 != null ? new Cat2Key(cat2.getId(), cat2.getName())
                    : new Cat2Key(null, "(分類なし)");
            structure.get(cat1.getId())
                    .computeIfAbsent(cat2Key, k -> new ArrayList<>())
                    .add(new SkillEntry(skill.getName(), null));
        }
        List<HeatmapRow> rows = buildHeatmapRows(cat1List, structure);
        String fyName = currentFy != null ? currentFy.getName() : null;
        return new HeatmapResponse(fyName, false, maxLevelValue, rows);
    }

    private Map<Integer, LinkedHashMap<Cat2Key, List<SkillEntry>>> initStructure(List<ItSkillCategory> cat1List) {
        Map<Integer, LinkedHashMap<Cat2Key, List<SkillEntry>>> structure = new LinkedHashMap<>();
        for (ItSkillCategory cat1 : cat1List) {
            structure.put(cat1.getId(), new LinkedHashMap<>());
        }
        return structure;
    }

    private List<HeatmapRow> buildHeatmapRows(List<ItSkillCategory> cat1List,
                                               Map<Integer, LinkedHashMap<Cat2Key, List<SkillEntry>>> structure) {
        return cat1List.stream().map(cat1 -> {
            LinkedHashMap<Cat2Key, List<SkillEntry>> cellMap = structure.get(cat1.getId());
            List<HeatmapCell> cells = cellMap.entrySet().stream().map(e -> {
                Cat2Key cat2Key = e.getKey();
                List<SkillEntry> entries = e.getValue();
                List<Short> scored = entries.stream()
                        .filter(s -> s.levelValue() != null)
                        .map(SkillEntry::levelValue)
                        .toList();
                Double avgLevelValue = scored.isEmpty() ? null : round1(average(scored));
                List<HeatmapSkill> skills = entries.stream()
                        .map(s -> new HeatmapSkill(s.skillName(),
                                s.levelValue() != null ? s.levelValue().intValue() : null))
                        .toList();
                return new HeatmapCell(cat2Key.id(), cat2Key.name(), avgLevelValue, scored.size(), skills);
            }).toList();
            return new HeatmapRow(cat1.getId(), cat1.getName(), cells);
        }).toList();
    }

    // ===== Timeline =====

    @Transactional(readOnly = true)
    public TimelineResponse getTimeline(User user) {
        List<Inventory> inventories = inventoryRepository.findByUserIdWithFiscalYear(user.getId());
        List<TimelineEvent> events = new ArrayList<>();
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);

        // 実績は「提出済みの最新棚卸」1件のみから取得（引き継ぎによる重複を防ぐ）
        Optional<Inventory> achievementSourceOpt = inventories.stream()
                .filter(i -> i.getStatus() == InventoryStatus.PENDING_GOAL
                        || i.getStatus() == InventoryStatus.COMPLETED)
                .max(Comparator.comparing(i -> i.getFiscalYear().getId()));

        if (achievementSourceOpt.isPresent()) {
            Inventory src = achievementSourceOpt.get();
            for (QualificationDetail qd : qualificationDetailRepository.findByInventoryId(src.getId())) {
                if (qd.getAcquiredYearMonth() == null) continue;
                String name = qd.getQualification() != null
                        ? qd.getQualification().getName()
                        : qd.getCustomQualificationName();
                LocalDate ym = qd.getAcquiredYearMonth().withDayOfMonth(1);
                events.add(new TimelineEvent("QUALIFICATION", "ACHIEVEMENT", name,
                        ym.toString(), ym.isBefore(firstOfMonth)));
            }
            for (SeminarDetail sd : seminarDetailRepository.findByInventoryId(src.getId())) {
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

        Optional<Inventory> latestInvOpt = inventories.stream()
                .max(Comparator.comparing(i -> i.getFiscalYear().getId()));
        if (latestInvOpt.isPresent()) {
            for (InventoryGoal goal : inventoryGoalRepository.findByInventoryId(latestInvOpt.get().getId())) {
                LocalDate ym = goal.getTargetPeriod().withDayOfMonth(1);
                String type = switch (goal.getGoalCategory()) {
                    case QUALIFICATION -> "GOAL_QUALIFICATION";
                    case IT_SKILL -> "GOAL_IT_SKILL";
                    case AD -> "GOAL_AD";
                };
                String lane = goal.getGoalCategory() == GoalCategory.QUALIFICATION ? "ACHIEVEMENT" : "ACTIVITY";
                String name = resolveGoalName(goal);
                events.add(new TimelineEvent(type, lane, name, ym.toString(), ym.isBefore(firstOfMonth)));
            }
        }

        events.sort(Comparator.comparing(TimelineEvent::yearMonth));
        return new TimelineResponse(events);
    }

    // ===== Helpers =====

    private Map<Integer, ItSkillCategory> buildCategoryMap() {
        return itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc().stream()
                .collect(Collectors.toMap(ItSkillCategory::getId, c -> c));
    }

    private List<ItSkillCategory> getActiveCat1List(Map<Integer, ItSkillCategory> catMap) {
        return catMap.values().stream()
                .filter(c -> c.getLevel() == 1 && c.isActive())
                .sorted(Comparator.comparingInt(ItSkillCategory::getSortOrder))
                .toList();
    }

    private int getMaxLevelValue() {
        return skillLevelRepository.findByActiveOrderByLevelValueAsc(true).stream()
                .mapToInt(s -> (int) s.getLevelValue())
                .max()
                .orElse(5);
    }

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

    private double average(List<Short> values) {
        return values.stream().mapToInt(Short::intValue).average().orElse(0.0);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String resolveGoalName(InventoryGoal g) {
        if (g.getItSkill() != null) return g.getItSkill().getName();
        if (g.getQualification() != null) return g.getQualification().getName();
        if (g.getAdSeminar() != null) return g.getAdSeminar().getName();
        return g.getCustomName();
    }

    // ===== Response DTOs =====

    public record RadarResponse(String currentFiscalYear, String prevFiscalYear,
                                 boolean hasCurrentYearData, int maxLevelValue,
                                 List<RadarAxis> axes) {}

    public record RadarAxis(int category1Id, String category1Name,
                             double currentAvgScore, Double prevAvgScore) {}

    public record GrowthResponse(List<String> fiscalYears, List<GrowthSeries> series) {}

    public record GrowthSeries(int category1Id, String category1Name,
                                List<Integer> yearlyTotalScores) {}

    public record HeatmapResponse(String currentFiscalYear, boolean hasCurrentYearData,
                                   int maxLevelValue, List<HeatmapRow> rows) {}

    public record HeatmapRow(int category1Id, String category1Name, List<HeatmapCell> cells) {}

    public record HeatmapCell(Integer category2Id, String category2Name,
                               Double avgLevelValue, int scoredSkillCount,
                               List<HeatmapSkill> skills) {}

    public record HeatmapSkill(String skillName, Integer levelValue) {}

    public record TimelineResponse(List<TimelineEvent> events) {}

    public record TimelineEvent(String type, String lane, String name,
                                 String yearMonth, boolean isPast) {}

    // Internal value objects
    private record Cat2Key(Integer id, String name) {}

    private record SkillEntry(String skillName, Short levelValue) {}
}

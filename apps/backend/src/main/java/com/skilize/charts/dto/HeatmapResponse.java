package com.skilize.charts.dto;

import java.util.List;

public record HeatmapResponse(String currentFiscalYear, boolean hasCurrentYearData,
                               int maxLevelValue, List<HeatmapRow> rows) {

    public record HeatmapRow(int category1Id, String category1Name, List<HeatmapCell> cells) {}

    public record HeatmapCell(Integer category2Id, String category2Name,
                               Double avgLevelValue, int scoredSkillCount,
                               List<HeatmapSkill> skills) {}

    public record HeatmapSkill(String skillName, Integer levelValue) {}
}

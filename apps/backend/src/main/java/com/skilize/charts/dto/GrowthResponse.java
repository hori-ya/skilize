package com.skilize.charts.dto;

import java.util.List;

public record GrowthResponse(List<String> fiscalYears, List<GrowthSeries> series) {

    public record GrowthSeries(int category1Id, String category1Name,
                                List<Integer> yearlyTotalScores) {}
}

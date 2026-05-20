package com.skilize.charts.dto;

import java.util.List;

public record RadarResponse(String currentFiscalYear, String prevFiscalYear,
                             boolean hasCurrentYearData, int maxScoreWeight,
                             List<RadarAxis> axes) {

    public record RadarAxis(int category1Id, String category1Name,
                             double currentAvgScore, Double prevAvgScore) {}
}

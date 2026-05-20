package com.skilize.inventory.dto;

import java.util.List;

public record ComparisonResponse(int inventoryId, String currentFiscalYear, String prevFiscalYear,
                                 boolean hasPrevYear, List<ComparisonItem> items) {

    public record ComparisonItem(Integer itSkillId, String skillName, int currentDetailId,
                                 int currentLevelValue, String currentRemarks,
                                 Integer prevLevelValue, Integer diff) {}
}

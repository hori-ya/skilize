package com.skilize.inventory.dto;

import com.skilize.inventory.domain.Inventory;

public record InventorySummaryDto(int id, FiscalYearRef fiscalYear, String status,
                                   String submittedAt, String goalCompletedAt) {

    public static InventorySummaryDto from(Inventory i) {
        return new InventorySummaryDto(i.getId(),
                new FiscalYearRef(i.getFiscalYear().getId(), i.getFiscalYear().getName()),
                i.getStatus().name(),
                i.getSubmittedAt() != null ? i.getSubmittedAt().toString() : null,
                i.getGoalCompletedAt() != null ? i.getGoalCompletedAt().toString() : null);
    }
}

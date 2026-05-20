package com.skilize.user.dto;

import com.skilize.inventory.domain.Inventory;

public record MemberInventorySummaryDto(int id, FiscalYearRef fiscalYear, String status,
                                         String submittedAt, String goalCompletedAt) {

    public static MemberInventorySummaryDto from(Inventory inv) {
        return new MemberInventorySummaryDto(
                inv.getId(),
                new FiscalYearRef(inv.getFiscalYear().getId(), inv.getFiscalYear().getName()),
                inv.getStatus().name(),
                inv.getSubmittedAt() != null ? inv.getSubmittedAt().toString() : null,
                inv.getGoalCompletedAt() != null ? inv.getGoalCompletedAt().toString() : null
        );
    }
}

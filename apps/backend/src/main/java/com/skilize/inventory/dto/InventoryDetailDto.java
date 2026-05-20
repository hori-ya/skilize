package com.skilize.inventory.dto;

import com.skilize.inventory.domain.Inventory;

public record InventoryDetailDto(int id, int userId, FiscalYearRef fiscalYear, String status,
                                  String submittedAt, String goalReviewCompletedAt,
                                  String goalCompletedAt, String createdAt, String updatedAt) {

    public static InventoryDetailDto from(Inventory i) {
        return new InventoryDetailDto(i.getId(), i.getUser().getId(),
                new FiscalYearRef(i.getFiscalYear().getId(), i.getFiscalYear().getName()),
                i.getStatus().name(),
                i.getSubmittedAt() != null ? i.getSubmittedAt().toString() : null,
                i.getGoalReviewCompletedAt() != null ? i.getGoalReviewCompletedAt().toString() : null,
                i.getGoalCompletedAt() != null ? i.getGoalCompletedAt().toString() : null,
                i.getCreatedAt() != null ? i.getCreatedAt().toString() : null,
                i.getUpdatedAt() != null ? i.getUpdatedAt().toString() : null);
    }
}

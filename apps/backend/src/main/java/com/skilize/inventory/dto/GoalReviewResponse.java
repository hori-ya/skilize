package com.skilize.inventory.dto;

import java.util.List;

public record GoalReviewResponse(String prevFiscalYear, boolean hasPrevGoals, List<GoalReviewItem> items) {

    public record GoalReviewItem(int prevGoalId, String goalCategory, String goalName,
                                 String targetPeriod, String reason,
                                 String achievementStatus, String reviewNote) {}
}

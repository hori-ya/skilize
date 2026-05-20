package com.skilize.dashboard.dto;

public record DashboardResponse(UserInfo user, FiscalYearRef currentFiscalYear,
                                CurrentInventoryInfo currentInventory) {

    public record UserInfo(int id, String name, String role) {}

    public record FiscalYearRef(int id, String name, String inputStartDate, String inputEndDate) {}

    public record CurrentInventoryInfo(int id, String status,
                                       int itSkillCount, int qualificationCount, int seminarCount,
                                       String submittedAt, String goalReviewCompletedAt, String goalCompletedAt) {}
}

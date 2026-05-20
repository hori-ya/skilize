package com.skilize.fiscalyear.dto;

import com.skilize.fiscalyear.domain.FiscalYear;

public record FiscalYearDto(int id, String name, String startDate, String endDate,
                            String inputStartDate, String inputEndDate, boolean isActive) {

    public static FiscalYearDto from(FiscalYear f) {
        return new FiscalYearDto(
                f.getId(), f.getName(),
                f.getStartDate().toString(),
                f.getEndDate().toString(),
                f.getInputStartDate() != null ? f.getInputStartDate().toString() : null,
                f.getInputEndDate() != null ? f.getInputEndDate().toString() : null,
                f.isActive());
    }
}

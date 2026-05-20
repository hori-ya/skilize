package com.skilize.fiscalyear.dto;

import jakarta.validation.constraints.NotBlank;

public record FiscalYearRequest(
        @NotBlank String name,
        @NotBlank String startDate,
        @NotBlank String endDate,
        String inputStartDate,
        String inputEndDate,
        Boolean active
) {}

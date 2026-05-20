package com.skilize.fiscalyear.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FiscalYearSettingsRequest(
        @NotNull @Min(1) @Max(12) Short fiscalYearStartMonth
) {}

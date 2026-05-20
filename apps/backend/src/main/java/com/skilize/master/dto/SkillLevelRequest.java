package com.skilize.master.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SkillLevelRequest(
        @NotNull @Min(1) Short levelValue,
        @NotBlank String description,
        Boolean active,
        @NotNull @Min(0) Integer scoreWeight
) {}

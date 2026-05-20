package com.skilize.master.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PromoteItSkillRequest(
        @NotBlank String customName,
        @NotNull Integer categoryId,
        @NotBlank String name,
        String description,
        Integer sortOrder
) {}

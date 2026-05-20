package com.skilize.master.dto;

import jakarta.validation.constraints.NotBlank;

public record ItSkillCategoryUpdateRequest(
        @NotBlank String name,
        Integer sortOrder,
        Boolean active
) {}

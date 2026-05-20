package com.skilize.master.dto;

import jakarta.validation.constraints.NotBlank;

public record ItSkillCategoryRequest(
        Integer parentId,
        @NotBlank String name,
        Integer sortOrder
) {}

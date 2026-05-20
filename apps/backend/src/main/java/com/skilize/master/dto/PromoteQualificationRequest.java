package com.skilize.master.dto;

import jakarta.validation.constraints.NotBlank;

public record PromoteQualificationRequest(
        @NotBlank String customName,
        Integer categoryId,
        @NotBlank String name,
        String description,
        Integer sortOrder
) {}

package com.skilize.master.dto;

import jakarta.validation.constraints.NotBlank;

public record QualificationRequest(
        Integer categoryId,
        @NotBlank String name,
        String description,
        Integer sortOrder,
        Boolean active
) {}

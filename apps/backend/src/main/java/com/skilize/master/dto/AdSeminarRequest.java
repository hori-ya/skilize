package com.skilize.master.dto;

import jakarta.validation.constraints.NotBlank;

public record AdSeminarRequest(
        Integer categoryId,
        @NotBlank String name,
        String description,
        Integer sortOrder,
        Boolean active
) {}
